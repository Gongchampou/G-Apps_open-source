package com.gongchampou.gapps

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import java.util.concurrent.TimeUnit

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val initialTimeMillis: Long = 25 * 60 * 1000L,
    val remainingTimeMillis: Long = 25 * 60 * 1000L,
    val isRunning: Boolean = false,
    val lastStartTime: Long? = null,
    val characterImageName: String = "study_default" // New field to support dynamic images
)

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val text: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "money_entries")
data class MoneyEntry(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val amount: Float,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class Track(
    val id: Int,
    val title: String,
    val artist: String,
    val url: String = "",
    val fileName: String = "",
    val category: String = ""
)

data class Ebook(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val category: String = "Story", 
    val coverImage: String = "", 
    val description: String = "",
    val content: String = ""
)

data class RelaxTrick(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val steps: List<String>
)

data class FocusCharacter(
    val name: String,
    val imagePath: String
)

fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
