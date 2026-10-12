package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "Other",
    val colorHex: String = "#FFF9C4", // Default Pastel Yellow
    val tags: String = "", // Comma-separated tags
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val metadataJson: String = "{}" // Category-specific structured fields
) {
    fun getTagList(): List<String> {
        return if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getMetadata(): JSONObject {
        return try {
            JSONObject(metadataJson)
        } catch (_: Exception) {
            JSONObject()
        }
    }
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long? = null, // Epoch millis
    val dueTime: String? = null, // "14:30"
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val status: String = "PENDING", // PENDING, COMPLETED
    val category: String = "Tasks",
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isCompleted: Boolean get() = status == "COMPLETED"

    fun getTagList(): List<String> {
        return if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // INCOME or EXPENSE
    val category: String = "General",
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val currency: String = "$"
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#FFF9C4",
    val iconName: String = "folder",
    val isDefault: Boolean = false
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "New Conversation",
    val model: String = "gemini-3.5-flash",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user" or "model"
    val content: String,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
