package com.gongchampou.gapps

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: UUID)
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos")
    fun getAllTodos(): Flow<List<Todo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: Todo)

    @Delete
    suspend fun deleteTodo(todo: Todo)
}

@Dao
interface MoneyDao {
    @Query("SELECT * FROM money_entries ORDER BY timestamp DESC")
    fun getAllMoneyEntries(): Flow<List<MoneyEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoneyEntry(entry: MoneyEntry)

    @Delete
    suspend fun deleteMoneyEntry(entry: MoneyEntry)

    @Query("DELETE FROM money_entries")
    suspend fun deleteAllMoneyEntries()
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllStudySessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySession)

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAllStudySessions()
}

@Database(entities = [Task::class, Todo::class, MoneyEntry::class, StudySession::class], version = 6)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun todoDao(): TodoDao
    abstract fun moneyDao(): MoneyDao
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN characterImageName TEXT NOT NULL DEFAULT 'study_default'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
