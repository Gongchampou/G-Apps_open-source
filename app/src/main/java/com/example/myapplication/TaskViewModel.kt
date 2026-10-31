package com.gongchampou.gapps

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val taskDao = db.taskDao()
    private val todoDao = db.todoDao()
    private val moneyDao = db.moneyDao()
    private val studyDao = db.studyDao()
    val settingsManager = SettingsManager(application)

    val studySessions: StateFlow<List<StudySession>> = studyDao.getAllStudySessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- STUDY TIMER STATE IN VIEWMODEL (Survives Navigation & App Exit) ---
    private val _studyInitialDuration = MutableStateFlow(0L)
    val studyInitialDuration: StateFlow<Long> = _studyInitialDuration.asStateFlow()

    private val _studyRemainingTime = MutableStateFlow(0L)
    val studyRemainingTime: StateFlow<Long> = _studyRemainingTime.asStateFlow()

    private val _isStudyRunning = MutableStateFlow(false)
    val isStudyRunning: StateFlow<Boolean> = _isStudyRunning.asStateFlow()

    private val _studyLastStartTime = MutableStateFlow(0L)
    val studyLastStartTime: StateFlow<Long> = _studyLastStartTime.asStateFlow()

    private val _accumulatedStudyMillis = MutableStateFlow(0L)
    val accumulatedStudyMillis: StateFlow<Long> = _accumulatedStudyMillis.asStateFlow()

    val tasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todos: StateFlow<List<Todo>> = todoDao.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isDarkMode: StateFlow<Boolean?> = settingsManager.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isNotificationsEnabled: StateFlow<Boolean> = settingsManager.notificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isVibrationEnabled: StateFlow<Boolean> = settingsManager.vibrationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSoundEnabled: StateFlow<Boolean> = settingsManager.soundEffectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showCircularProgress: StateFlow<Boolean> = settingsManager.showCircularProgressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSwipeMainNavEnabled: StateFlow<Boolean> = settingsManager.swipeMainNavFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSwipeTrackingNavEnabled: StateFlow<Boolean> = settingsManager.swipeTrackingNavFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currency: StateFlow<String> = settingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    val timerTone: StateFlow<String> = settingsManager.timerToneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Default")

    val ebookFontSize: StateFlow<Float> = settingsManager.ebookFontSizeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18f)

    val moneyLimit: StateFlow<Float> = settingsManager.moneyLimitFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val moneySpent: StateFlow<Float> = settingsManager.moneySpentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val keepScreenAwake: StateFlow<Boolean> = settingsManager.keepScreenAwakeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val moneyEntries: StateFlow<List<MoneyEntry>> = moneyDao.getAllMoneyEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _characters = MutableStateFlow<List<FocusCharacter>>(emptyList())
    val characters = _characters.asStateFlow()

    private var mediaPlayer: android.media.MediaPlayer? = null

    init {
        loadCharacters()
    }

    private fun loadCharacters() {
        viewModelScope.launch {
            try {
                // INSTRUCTION: characters.json is now in the root assets folder
                val jsonString = getApplication<Application>().assets.open("characters.json")
                    .bufferedReader().use { it.readText() }
                val listType = object : TypeToken<List<FocusCharacter>>() {}.type
                val loadedCharacters: List<FocusCharacter> = Gson().fromJson(jsonString, listType)
                _characters.value = loadedCharacters
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback or empty list
            }
        }
    }

    // Task logic
    fun addTask(name: String, hours: Int = 0, minutes: Int = 25, seconds: Int = 0, characterImageName: String = "study_default") {
        val millis = (hours * 3600 + minutes * 60 + seconds) * 1000L
        viewModelScope.launch {
            taskDao.insertTask(Task(
                name = name, 
                initialTimeMillis = millis, 
                remainingTimeMillis = millis,
                characterImageName = characterImageName
            ))
        }
    }

    fun removeTask(taskId: UUID) {
        viewModelScope.launch {
            taskDao.deleteTaskById(taskId)
        }
    }

    fun toggleTask(taskId: UUID) {
        viewModelScope.launch {
            val currentTasks = tasks.value
            val task = currentTasks.find { it.id == taskId }
            if (task != null) {
                val now = System.currentTimeMillis()
                val updatedTask = if (task.isRunning) {
                    val elapsedSinceStart = now - (task.lastStartTime ?: now)
                    val newRemaining = (task.remainingTimeMillis - elapsedSinceStart).coerceAtLeast(0)
                    stopTimerService()
                    task.copy(
                        isRunning = false,
                        remainingTimeMillis = newRemaining,
                        lastStartTime = null
                    )
                } else {
                    if (task.remainingTimeMillis <= 0) {
                        // Reset if it reached zero
                        startTimerService(task.name, task.initialTimeMillis)
                        task.copy(
                            isRunning = true,
                            remainingTimeMillis = task.initialTimeMillis,
                            lastStartTime = now
                        )
                    } else {
                        startTimerService(task.name, task.remainingTimeMillis)
                        task.copy(
                            isRunning = true,
                            lastStartTime = now
                        )
                    }
                }
                taskDao.insertTask(updatedTask)
            }
        }
    }

    fun updateTaskProgress(taskId: UUID, remaining: Long) {
        viewModelScope.launch {
            val task = tasks.value.find { it.id == taskId }
            if (task != null && task.isRunning) {
                if (remaining <= 0) {
                    toggleTask(taskId) // Stop it
                } else {
                    taskDao.insertTask(task.copy(remainingTimeMillis = remaining))
                }
            }
        }
    }

    private var wasStudyAutoPausedOnBackground = false

    fun onEnterBackground() {
        // Auto-pause Study Timer when exiting or backgrounding the app
        if (_isStudyRunning.value) {
            wasStudyAutoPausedOnBackground = true
            pauseStudyTimer()
        }

        val runningTask = tasks.value.find { it.isRunning }
        if (runningTask != null) {
            val intent = Intent(getApplication(), TimerService::class.java).apply {
                action = "UPDATE_VISIBILITY"
                putExtra("IS_VISIBLE", false)
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun onEnterForeground() {
        // Auto-resume Study Timer when returning to the app if it was auto-paused on background
        if (wasStudyAutoPausedOnBackground) {
            wasStudyAutoPausedOnBackground = false
            startStudyTimer()
        }

        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = "UPDATE_VISIBILITY"
            putExtra("IS_VISIBLE", true)
        }
        getApplication<Application>().startService(intent)
    }

    private fun startTimerService(taskName: String, remainingTime: Long) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = "START"
            putExtra("TASK_NAME", taskName)
            putExtra("REMAINING_TIME", remainingTime)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    private fun stopTimerService() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = "STOP"
        }
        getApplication<Application>().stopService(intent)
        triggerAlert()
    }

    private fun triggerAlert() {
        viewModelScope.launch {
            if (settingsManager.vibrationFlow.first()) {
                vibrate()
            }
            if (settingsManager.soundEffectsFlow.first()) {
                playSound()
            }
        }
    }

    fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }

    fun playSound() {
        stopSound()
        try {
            val tone = timerTone.value
            if (tone == "Default") {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(getApplication(), notification)
                r.play()
            } else {
                // Play from assets/music/ringtone
                val assetManager = getApplication<Application>().assets
                val fileName = if (tone.contains(".")) "music/ringtone/$tone" else "music/ringtone/$tone.mp3"
                
                // Log for debugging (you can see this in Logcat)
                android.util.Log.d("TaskViewModel", "Attempting to play: $fileName")
                
                val afd = assetManager.openFd(fileName)
                val mp = android.media.MediaPlayer()
                mediaPlayer = mp
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mp.prepare()
                mp.start()
                mp.setOnCompletionListener { 
                    it.release()
                    if (mediaPlayer == it) mediaPlayer = null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskViewModel", "Error playing sound", e)
            // Fallback to default if custom fails
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(getApplication(), notification)
                r.play()
            } catch (e2: Exception) { e2.printStackTrace() }
        }
    }

    fun stopSound() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    override fun onCleared() {
        stopSound()
        super.onCleared()
    }

    // Todo logic
    fun addTodo(text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                todoDao.insertTodo(Todo(text = text))
            }
        }
    }

    fun toggleTodo(todoId: UUID) {
        viewModelScope.launch {
            val currentTodos = todos.value
            val todo = currentTodos.find { it.id == todoId }
            if (todo != null) {
                todoDao.insertTodo(todo.copy(isCompleted = !todo.isCompleted))
            }
        }
    }

    fun removeTodo(todoId: UUID) {
        viewModelScope.launch {
            val currentTodos = todos.value
            val todo = currentTodos.find { it.id == todoId }
            if (todo != null) {
                todoDao.deleteTodo(todo)
            }
        }
    }

    // Settings logic
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setDarkMode(enabled)
        }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setNotifications(enabled)
        }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setVibration(enabled)
        }
    }

    fun setSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSoundEffects(enabled)
        }
    }

    fun setShowCircularProgress(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setShowCircularProgress(enabled)
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            settingsManager.setCurrency(currency)
        }
    }

    fun setTimerTone(tone: String) {
        viewModelScope.launch {
            settingsManager.setTimerTone(tone)
        }
    }

    fun setEbookFontSize(size: Float) {
        viewModelScope.launch {
            settingsManager.setEbookFontSize(size)
        }
    }

    fun setMoneyLimit(limit: Float) {
        viewModelScope.launch {
            settingsManager.setMoneyLimit(limit)
        }
    }

    fun resetMoneyData() {
        viewModelScope.launch {
            moneyDao.deleteAllMoneyEntries()
            setMoneySpent(0f)
        }
    }

    fun setMoneySpent(spent: Float) {
        viewModelScope.launch {
            settingsManager.setMoneySpent(spent)
        }
    }

    fun logStudySession(durationMinutes: Int) {
        if (durationMinutes > 0) {
            viewModelScope.launch {
                studyDao.insertStudySession(StudySession(durationMinutes = durationMinutes))
            }
        }
    }

    fun setStudyDuration(durationMillis: Long) {
        _studyInitialDuration.value = durationMillis
        _studyRemainingTime.value = durationMillis
        _isStudyRunning.value = false
    }

    fun startStudyTimer() {
        if (_studyInitialDuration.value > 0L) {
            val now = System.currentTimeMillis()
            _studyLastStartTime.value = now
            if (_studyRemainingTime.value <= 0L) {
                _studyRemainingTime.value = _studyInitialDuration.value
            }
            _isStudyRunning.value = true
            startTimerService("Study Timer", _studyRemainingTime.value)
        }
    }

    fun pauseStudyTimer() {
        if (_isStudyRunning.value) {
            val now = System.currentTimeMillis()
            val elapsed = now - _studyLastStartTime.value
            _accumulatedStudyMillis.value += elapsed
            _studyRemainingTime.value = (_studyRemainingTime.value - elapsed).coerceAtLeast(0L)
            _isStudyRunning.value = false
            stopTimerService()
            flushAndLogStudySession()
        }
    }

    fun resetStudyTimer() {
        if (_isStudyRunning.value) {
            val now = System.currentTimeMillis()
            val elapsed = now - _studyLastStartTime.value
            _accumulatedStudyMillis.value += elapsed
            flushAndLogStudySession()
            _isStudyRunning.value = false
            stopTimerService()
        }
        _studyRemainingTime.value = _studyInitialDuration.value
        stopSound()
    }

    fun updateStudyTick() {
        if (_isStudyRunning.value) {
            val now = System.currentTimeMillis()
            val elapsed = now - _studyLastStartTime.value
            val currentRemaining = (_studyRemainingTime.value - elapsed).coerceAtLeast(0L)
            if (currentRemaining <= 0L) {
                _isStudyRunning.value = false
                _studyRemainingTime.value = 0L
                val sessionMins = (_studyInitialDuration.value / (60 * 1000L)).toInt().coerceAtLeast(1)
                logStudySession(sessionMins)
                _accumulatedStudyMillis.value = 0L
                stopTimerService()
                triggerAlert()
            }
        }
    }

    fun flushAndLogStudySession() {
        if (_accumulatedStudyMillis.value >= 10000L) {
            val loggedMins = ((_accumulatedStudyMillis.value + 30000L) / 60000L).toInt().coerceAtLeast(1)
            logStudySession(loggedMins)
            _accumulatedStudyMillis.value = 0L
        }
    }

    fun clearStudySessions() {
        viewModelScope.launch {
            studyDao.deleteAllStudySessions()
        }
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setKeepScreenAwake(enabled)
        }
    }

    fun setSwipeMainNav(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSwipeMainNav(enabled)
        }
    }

    fun setSwipeTrackingNav(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSwipeTrackingNav(enabled)
        }
    }

    fun addMoneyEntry(amount: Float, description: String) {
        viewModelScope.launch {
            moneyDao.insertMoneyEntry(MoneyEntry(amount = amount, description = description))
            val currentSpent = moneySpent.value
            setMoneySpent(currentSpent + amount)
        }
    }

    fun removeMoneyEntry(entry: MoneyEntry) {
        viewModelScope.launch {
            moneyDao.deleteMoneyEntry(entry)
            val currentSpent = moneySpent.value
            setMoneySpent((currentSpent - entry.amount).coerceAtLeast(0f))
        }
    }
}
