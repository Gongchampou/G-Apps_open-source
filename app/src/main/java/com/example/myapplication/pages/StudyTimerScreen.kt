package com.gongchampou.gapps.pages

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gongchampou.gapps.StudySession
import com.gongchampou.gapps.TaskViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * STUDY TIMER SCREEN
 * Dual Concentric Circle Timer Display with separate Hours & Minutes setting inputs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTimerScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val keepScreenAwake by viewModel.keepScreenAwake.collectAsState()
    val studySessions by viewModel.studySessions.collectAsState()

    // Collect Study Timer State from ViewModel (Survives Tab Navigation & App Backgrounding)
    val initialDurationMillis by viewModel.studyInitialDuration.collectAsState()
    val remainingTimeMillis by viewModel.studyRemainingTime.collectAsState()
    val isRunning by viewModel.isStudyRunning.collectAsState()
    val lastStartTime by viewModel.studyLastStartTime.collectAsState()

    // Dialog & Sheet States
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showAnalyticsSheet by remember { mutableStateOf(false) }
    var inputHoursText by remember { mutableStateOf("0") }
    var inputMinutesText by remember { mutableStateOf("0") }

    // Helper: Opens set time dialog prefilled with current hours and minutes
    fun openSetTimeDialog() {
        val currentHrs = (initialDurationMillis / (3600 * 1000L)).toInt()
        val currentMins = ((initialDurationMillis % (3600 * 1000L)) / (60 * 1000L)).toInt()
        inputHoursText = if (currentHrs > 0) currentHrs.toString() else ""
        inputMinutesText = if (currentMins > 0) currentMins.toString() else ""
        showTimePickerDialog = true
    }

    // State variable that forces smooth Compose recomposition every 100ms
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Keep screen awake while timer is running
    DisposableEffect(isRunning, keepScreenAwake) {
        val window = (context as? Activity)?.window
        if (isRunning && keepScreenAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // High-frequency ticker loop
    LaunchedEffect(isRunning, lastStartTime) {
        if (isRunning) {
            while (isRunning) {
                delay(100)
                nowMillis = System.currentTimeMillis()
                viewModel.updateStudyTick()
            }
        }
    }

    // Calculate current remaining display time
    val displayRemaining = if (isRunning && lastStartTime > 0L) {
        val elapsed = nowMillis - lastStartTime
        (remainingTimeMillis - elapsed).coerceAtLeast(0L)
    } else {
        remainingTimeMillis
    }

    // Outer Circle Progress Ratio (Hours / Total Duration)
    val outerRatio = if (initialDurationMillis > 0) {
        (displayRemaining.toFloat() / initialDurationMillis.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Inner Circle Progress Ratio (Minutes within current hour / Seconds within minute)
    val innerRatio = if (initialDurationMillis >= 60 * 60 * 1000L) {
        val minsInHour = displayRemaining % (60 * 60 * 1000L)
        (minsInHour.toFloat() / (60 * 60 * 1000L).toFloat()).coerceIn(0f, 1f)
    } else {
        val secsInMin = displayRemaining % (60 * 1000L)
        (secsInMin.toFloat() / (60 * 1000L).toFloat()).coerceIn(0f, 1f)
    }

    val animatedOuterProgress by animateFloatAsState(
        targetValue = outerRatio,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "OuterProgress"
    )

    val animatedInnerProgress by animateFloatAsState(
        targetValue = innerRatio,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "InnerProgress"
    )

    // Sleek Dark Theme Colors (Matching Image 1 & 2)
    val darkBackground = Color(0xFF141921)
    val circleTrackColor = Color(0xFF232D3B)
    val neonCyanColor = Color(0xFF00E5FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .padding(20.dp)
    ) {
        // --- TOP HEADER ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TOP LEFT: HAMBURGER BUTTON & RESET BUTTON
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // HAMBURGER MENU BUTTON (OPENS TRACKING SHEET)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(circleTrackColor)
                        .clickable { showAnalyticsSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Tracking Analytics Menu",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // RESET BUTTON
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(circleTrackColor)
                        .clickable {
                            viewModel.resetStudyTimer()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // TOP RIGHT: CUSTOM TIME EDIT ICON
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(circleTrackColor)
                    .clickable {
                        openSetTimeDialog()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreTime,
                    contentDescription = "Set Custom Time",
                    tint = neonCyanColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // --- MAIN CENTER TIMER CONTENT ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {

            // DUAL CONCENTRIC CIRCULAR TIMER DISPLAY
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(290.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (!isRunning) {
                            openSetTimeDialog()
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val outerStrokeWidth = 14.dp.toPx()
                    val outerDiameter = size.minDimension - outerStrokeWidth
                    val outerTopLeft = Offset(
                        (size.width - outerDiameter) / 2,
                        (size.height - outerDiameter) / 2
                    )
                    val outerArcSize = Size(outerDiameter, outerDiameter)

                    // 1. OUTER CIRCLE: Overall / Hours Progress
                    drawCircle(
                        color = circleTrackColor,
                        radius = outerDiameter / 2,
                        style = Stroke(width = outerStrokeWidth)
                    )

                    if (animatedOuterProgress > 0f) {
                        drawArc(
                            color = neonCyanColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedOuterProgress,
                            useCenter = false,
                            topLeft = outerTopLeft,
                            size = outerArcSize,
                            style = Stroke(width = outerStrokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // 2. INNER CIRCLE: 60 Radial Segmented Ticks (Minutes / Small Timer Progress)
                    val center = Offset(size.width / 2, size.height / 2)
                    val innerOuterRadius = (outerDiameter / 2) - 18.dp.toPx()
                    val tickLength = 12.dp.toPx()
                    val innerInnerRadius = innerOuterRadius - tickLength

                    val activeTickCount = (animatedInnerProgress * 60f).toInt()

                    for (i in 0 until 60) {
                        val angleDeg = -90f + (i * 6f)
                        val angleRad = Math.toRadians(angleDeg.toDouble())

                        val startX = center.x + (innerInnerRadius * Math.cos(angleRad)).toFloat()
                        val startY = center.y + (innerInnerRadius * Math.sin(angleRad)).toFloat()
                        val endX = center.x + (innerOuterRadius * Math.cos(angleRad)).toFloat()
                        val endY = center.y + (innerOuterRadius * Math.sin(angleRad)).toFloat()

                        val isTickActive = i <= activeTickCount && animatedInnerProgress > 0f
                        val tickColor = if (isTickActive) {
                            if (i < 20) Color(0xFFFFEA00) else if (i < 40) Color(0xFFFF9100) else Color(0xFFFF2A6D)
                        } else {
                            Color(0xFF232D3B)
                        }

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (isTickActive) 3.5.dp.toPx() else 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // CENTRAL DIGITAL TIME DISPLAY WITH SMALLER SECONDS
                val hours = TimeUnit.MILLISECONDS.toHours(displayRemaining)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(displayRemaining) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(displayRemaining) % 60

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (hours > 0) {
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", hours, minutes),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = neonCyanColor
                        )
                        Text(
                            text = String.format(Locale.US, ":%02d", seconds),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = neonCyanColor.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    } else {
                        Text(
                            text = String.format(Locale.US, "%02d", minutes),
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = neonCyanColor
                        )
                        Text(
                            text = String.format(Locale.US, ":%02d", seconds),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = neonCyanColor.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- START / PAUSE MAIN BUTTON ---
            Button(
                onClick = {
                    viewModel.stopSound()
                    if (isRunning) {
                        viewModel.pauseStudyTimer()
                    } else {
                        // Start logic: If duration is 0, prompt to set time
                        if (initialDurationMillis <= 0L || remainingTimeMillis <= 0L) {
                            openSetTimeDialog()
                        } else {
                            viewModel.startStudyTimer()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = neonCyanColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .height(60.dp)
                    .width(200.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    tint = Color.Black,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "PAUSE" else "START",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
            }
        }
    }

    // --- SEPARATE HOURS & MINUTES TIME SETTING DIALOG ---
    if (showTimePickerDialog) {
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            containerColor = circleTrackColor,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Text(
                    text = "Set Study Time",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set hours and minutes separately:",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // HOURS INPUT FIELD
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hours",
                                color = neonCyanColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = inputHoursText,
                                onValueChange = { inputHoursText = it.filter { char -> char.isDigit() } },
                                singleLine = true,
                                placeholder = { Text("0", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = neonCyanColor,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        Text(
                            text = ":",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        // MINUTES INPUT FIELD
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Minutes",
                                color = neonCyanColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = inputMinutesText,
                                onValueChange = { inputMinutesText = it.filter { char -> char.isDigit() } },
                                singleLine = true,
                                placeholder = { Text("0", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = neonCyanColor,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hrs = inputHoursText.toIntOrNull() ?: 0
                        val mins = inputMinutesText.toIntOrNull() ?: 0
                        val totalMillis = (hrs * 3600L + mins * 60L) * 1000L
                        if (totalMillis > 0L) {
                            viewModel.setStudyDuration(totalMillis)
                            viewModel.stopSound()
                        }
                        showTimePickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = neonCyanColor, contentColor = Color.Black)
                ) {
                    Text("Set Time", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    // --- HAMBURGER MENU: STUDY ANALYTICS & TRACKING SHEET ---
    if (showAnalyticsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAnalyticsSheet = false },
            containerColor = Color(0xFF1B222C),
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            StudyAnalyticsView(
                sessions = studySessions,
                onClose = { showAnalyticsSheet = false }
            )
        }
    }
}

/**
 * STUDY ANALYTICS VIEW
 * Displays automatic real-time study progress with progress bars for Day, Week, Month, and Year (Year-by-Year list).
 */
@Composable
fun StudyAnalyticsView(
    sessions: List<StudySession>,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Day") }
    val tabs = listOf("Day", "Week", "Month", "Year")

    val neonCyan = Color(0xFF00E5FF)
    val trackBg = Color(0xFF2A3648)

    // Calculate time metrics from automatic session history
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()

    // 1. Day stats (Today)
    cal.timeInMillis = now
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    val startOfDay = cal.timeInMillis

    val todaySessions = sessions.filter { it.timestamp >= startOfDay }
    val todayMins = todaySessions.sumOf { it.durationMinutes }
    val dailyGoalMins = 120 // 2 Hours Goal

    // 9 Daily Time Periods (Dawn, Morning, Late Morning, Noon, Dusk, Evening, Late Evening, Night Time, Mid Night Time)
    val dayPeriodList = remember(todaySessions) {
        val periods = listOf(
            Triple("Dawn", "🌅 4 AM - 7 AM", setOf(4, 5, 6)),
            Triple("Morning", "🌄 7 AM - 10 AM", setOf(7, 8, 9)),
            Triple("Late Morning", "☀️ 10 AM - 12 PM", setOf(10, 11)),
            Triple("Noon", "🌤️ 12 PM - 3 PM", setOf(12, 13, 14)),
            Triple("Dusk", "🌆 3 PM - 6 PM", setOf(15, 16, 17)),
            Triple("Evening", "🌆 6 PM - 8 PM", setOf(18, 19)),
            Triple("Late Evening", "🌃 8 PM - 10 PM", setOf(20, 21)),
            Triple("Night Time", "🌙 10 PM - 1 AM", setOf(22, 23, 0)),
            Triple("Mid Night Time", "🌌 1 AM - 4 AM", setOf(1, 2, 3))
        )

        periods.map { (name, label, hours) ->
            val periodMins = todaySessions.filter { session ->
                val c = Calendar.getInstance()
                c.timeInMillis = session.timestamp
                c.get(Calendar.HOUR_OF_DAY) in hours
            }.sumOf { it.durationMinutes }

            DayPeriodStat(name, label, periodMins)
        }
    }

    // 2. Week stats
    cal.timeInMillis = now
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    val startOfWeek = cal.timeInMillis
    val weekSessions = sessions.filter { it.timestamp >= startOfWeek }
    val weekMins = weekSessions.sumOf { it.durationMinutes }
    val weeklyGoalMins = 900 // 15 Hours Goal

    // Week Days Breakdown (Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday)
    val weekDayList = remember(sessions, startOfWeek) {
        val list = mutableListOf<DayPeriodStat>()
        val dayFormat = SimpleDateFormat("EEEE (MMM d)", Locale.getDefault())
        val c = Calendar.getInstance()

        for (d in 0 until 7) {
            c.timeInMillis = startOfWeek
            c.add(Calendar.DAY_OF_YEAR, d)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            val dStart = c.timeInMillis

            c.add(Calendar.DAY_OF_YEAR, 1)
            val dEnd = c.timeInMillis

            val dLabel = dayFormat.format(Date(dStart))
            val dSessions = sessions.filter { it.timestamp in dStart until dEnd }
            val dMins = dSessions.sumOf { it.durationMinutes }

            list.add(DayPeriodStat(dLabel, "", dMins))
        }
        list
    }

    // 3. Month stats
    cal.timeInMillis = now
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    val startOfMonth = cal.timeInMillis
    val monthSessions = sessions.filter { it.timestamp >= startOfMonth }
    val monthMins = monthSessions.sumOf { it.durationMinutes }
    val monthlyGoalMins = 3600 // 60 Hours Goal

    // 4. All 12 Months breakdown list
    val yearMonthList = remember(sessions) {
        val list = mutableListOf<MonthStat>()
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val c = Calendar.getInstance()

        for (i in 0 until 12) {
            c.timeInMillis = now
            c.add(Calendar.MONTH, -i)
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            val mStart = c.timeInMillis

            c.add(Calendar.MONTH, 1)
            val mEnd = c.timeInMillis

            val monthName = dateFormat.format(Date(mStart))
            val mSessions = sessions.filter { it.timestamp in mStart until mEnd }
            val mTotalMins = mSessions.sumOf { it.durationMinutes }

            list.add(MonthStat(monthName, mTotalMins))
        }
        list
    }

    // 5. Year-by-Year breakdown list (2026, 2025, 2024, 2023, 2022)
    val yearList = remember(sessions) {
        val list = mutableListOf<YearStat>()
        val c = Calendar.getInstance()
        val currentYear = c.get(Calendar.YEAR)

        for (y in currentYear downTo (currentYear - 4)) {
            val yCal = Calendar.getInstance()
            yCal.set(y, Calendar.JANUARY, 1, 0, 0, 0)
            val yStart = yCal.timeInMillis

            yCal.set(y, Calendar.DECEMBER, 31, 23, 59, 59)
            val yEnd = yCal.timeInMillis

            val ySessions = sessions.filter { it.timestamp in yStart..yEnd }
            val yTotalMins = ySessions.sumOf { it.durationMinutes }

            list.add(YearStat("Year $y", yTotalMins, ySessions.size))
        }
        list
    }

    val currentYearStat = yearList.firstOrNull()
    val currentYearMins = currentYearStat?.totalMinutes ?: 0
    val yearGoalMins = 43200 // 720 Hours Yearly Target

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Auto Study Tracking",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    text = "Continuous automatic timer tracking",
                    fontSize = 12.sp,
                    color = neonCyan
                )
            }

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TAB SWITCHER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(trackBg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) neonCyan else Color.Transparent)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
        ) {
            when (selectedTab) {
                "Day" -> {
                    item {
                        ProgressCard(
                            title = "Daily Study Goal",
                            subtitle = "Target: 2 Hours (120 min)",
                            currentMins = todayMins,
                            targetMins = dailyGoalMins,
                            sessionCount = todaySessions.size,
                            accentColor = neonCyan,
                            trackBg = trackBg
                        )
                    }

                    item {
                        // TODAY'S TIME PERIODS DOWN LIST (Dawn, Morning, Late Morning, Noon, Dusk, Evening, Late Evening, Night Time, Mid Night Time)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = trackBg),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Daily Time Periods Breakdown",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                dayPeriodList.forEach { period ->
                                    val periodRatio = (period.totalMinutes.toFloat() / 60f).coerceIn(0f, 1f)
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = period.periodName,
                                                    fontSize = 14.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (period.timeLabel.isNotEmpty()) {
                                                    Text(
                                                        text = period.timeLabel,
                                                        fontSize = 11.sp,
                                                        color = Color.LightGray
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${period.totalMinutes / 60}h ${period.totalMinutes % 60}m",
                                                fontSize = 13.sp,
                                                color = neonCyan,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { periodRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = neonCyan,
                                            trackColor = Color.Black.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "Week" -> {
                    item {
                        ProgressCard(
                            title = "Weekly Study Goal",
                            subtitle = "Target: 15 Hours (900 min)",
                            currentMins = weekMins,
                            targetMins = weeklyGoalMins,
                            sessionCount = weekSessions.size,
                            accentColor = Color(0xFF00FF9D),
                            trackBg = trackBg
                        )
                    }

                    item {
                        // WEEKLY DAILY BREAKDOWN (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = trackBg),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "This Week's Daily Breakdown",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                weekDayList.forEach { dayStat ->
                                    val dRatio = (dayStat.totalMinutes.toFloat() / 120f).coerceIn(0f, 1f)
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = dayStat.periodName,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${dayStat.totalMinutes / 60}h ${dayStat.totalMinutes % 60}m",
                                                fontSize = 13.sp,
                                                color = Color(0xFF00FF9D),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { dRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = Color(0xFF00FF9D),
                                            trackColor = Color.Black.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "Month" -> {
                    item {
                        ProgressCard(
                            title = "Monthly Study Goal",
                            subtitle = "Target: 60 Hours (3600 min)",
                            currentMins = monthMins,
                            targetMins = monthlyGoalMins,
                            sessionCount = monthSessions.size,
                            accentColor = Color(0xFFFF9100),
                            trackBg = trackBg
                        )
                    }

                    item {
                        // DIFFERENT MONTHS LIST DOWN WITH TRACKING STATUS LOAD UP
                        Card(
                            colors = CardDefaults.cardColors(containerColor = trackBg),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Monthly Tracking History",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                yearMonthList.forEach { mStat ->
                                    val mRatio = (mStat.totalMinutes.toFloat() / 3600f).coerceIn(0f, 1f)
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = mStat.monthName,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${mStat.totalMinutes / 60}h ${mStat.totalMinutes % 60}m",
                                                fontSize = 13.sp,
                                                color = Color(0xFFFF9100),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { mRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = Color(0xFFFF9100),
                                            trackColor = Color.Black.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "Year" -> {
                    item {
                        // YEARLY GOAL CARD
                        Card(
                            colors = CardDefaults.cardColors(containerColor = trackBg),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Yearly Study Progress", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                        Text(text = "Target: 720 Hours (Yearly)", fontSize = 12.sp, color = Color.LightGray)
                                    }
                                    val yearProgressRatio = (currentYearMins.toFloat() / yearGoalMins.toFloat()).coerceIn(0f, 1f)
                                    Text(
                                        text = "${(yearProgressRatio * 100).toInt()}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp,
                                        color = neonCyan
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                val overallProgress = (currentYearMins.toFloat() / yearGoalMins.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { overallProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = neonCyan,
                                    trackColor = Color.Black.copy(alpha = 0.3f)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Current Year Studied", fontSize = 11.sp, color = Color.Gray)
                                        Text("${currentYearMins / 60}h ${currentYearMins % 60}m", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Sessions", fontSize = 11.sp, color = Color.Gray)
                                        Text("${currentYearStat?.sessionCount ?: 0} Sessions", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // YEAR-BY-YEAR HISTORY LIST DOWN THE SCREEN
                        Card(
                            colors = CardDefaults.cardColors(containerColor = trackBg),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Year-by-Year Tracking History",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                yearList.forEach { yStat ->
                                    val yRatio = (yStat.totalMinutes.toFloat() / yearGoalMins.toFloat()).coerceIn(0f, 1f)
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(yStat.yearName, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text("${yStat.totalMinutes / 60}h ${yStat.totalMinutes % 60}m", fontSize = 13.sp, color = neonCyan, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { yRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = neonCyan,
                                            trackColor = Color.Black.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * PROGRESS CARD FOR DAY / WEEK / MONTH STATS
 */
@Composable
fun ProgressCard(
    title: String,
    subtitle: String,
    currentMins: Int,
    targetMins: Int,
    sessionCount: Int,
    accentColor: Color,
    trackBg: Color
) {
    val progress = (currentMins.toFloat() / targetMins.toFloat()).coerceIn(0f, 1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = trackBg),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    Text(text = subtitle, fontSize = 12.sp, color = Color.LightGray)
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PROGRESS BAR
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = accentColor,
                trackColor = Color.Black.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Studied Time", fontSize = 11.sp, color = Color.Gray)
                    Text("${currentMins / 60}h ${currentMins % 60}m", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sessions Done", fontSize = 11.sp, color = Color.Gray)
                    Text("$sessionCount Sessions", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

data class DayPeriodStat(
    val periodName: String,
    val timeLabel: String,
    val totalMinutes: Int
)

data class MonthStat(
    val monthName: String,
    val totalMinutes: Int
)

data class YearStat(
    val yearName: String,
    val totalMinutes: Int,
    val sessionCount: Int
)
