package com.example.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(
    private val database: AppDatabase
) {
    private val noteDao = database.noteDao()
    private val taskDao = database.taskDao()
    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val chatDao = database.chatDao()

    // Notes
    val allActiveNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)
    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)
    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)

    // Tasks
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    suspend fun insertTransaction(transaction: TransactionEntity): Long = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.deleteTransaction(transaction)

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    // Chat
    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()
    suspend fun getSessionById(id: Long) = chatDao.getSessionById(id)
    suspend fun insertSession(session: ChatSessionEntity): Long = chatDao.insertSession(session)
    suspend fun updateSession(session: ChatSessionEntity) = chatDao.updateSession(session)
    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSessionById(sessionId)
    }

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun insertMessage(message: ChatMessageEntity): Long = chatDao.insertMessage(message)

    // Clear all user data
    suspend fun clearAllData() {
        noteDao.deleteAllNotes()
        taskDao.deleteAllTasks()
        transactionDao.deleteAllTransactions()
        categoryDao.deleteAllCategories()
        chatDao.deleteAllSessions()
        chatDao.deleteAllMessages()
    }

    // Reset default sample data
    suspend fun resetToDefaultSample() {
        clearAllData()
        AppDatabase.populateInitialData(database)
    }

    // Export all data to JSON
    suspend fun exportToJson(
        notes: List<NoteEntity>,
        tasks: List<TaskEntity>,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val notesArr = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("category", n.category)
            obj.put("colorHex", n.colorHex)
            obj.put("tags", n.tags)
            obj.put("isPinned", n.isPinned)
            obj.put("isArchived", n.isArchived)
            obj.put("metadataJson", n.metadataJson)
            notesArr.put(obj)
        }
        root.put("notes", notesArr)

        val tasksArr = JSONArray()
        for (t in tasks) {
            val obj = JSONObject()
            obj.put("title", t.title)
            obj.put("description", t.description)
            obj.put("dueDate", t.dueDate ?: -1L)
            obj.put("dueTime", t.dueTime ?: "")
            obj.put("priority", t.priority)
            obj.put("status", t.status)
            obj.put("category", t.category)
            obj.put("tags", t.tags)
            tasksArr.put(obj)
        }
        root.put("tasks", tasksArr)

        val transArr = JSONArray()
        for (tr in transactions) {
            val obj = JSONObject()
            obj.put("title", tr.title)
            obj.put("amount", tr.amount)
            obj.put("type", tr.type)
            obj.put("category", tr.category)
            obj.put("date", tr.date)
            obj.put("notes", tr.notes)
            obj.put("currency", tr.currency)
            transArr.put(obj)
        }
        root.put("transactions", transArr)

        val catArr = JSONArray()
        for (c in categories) {
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("colorHex", c.colorHex)
            obj.put("iconName", c.iconName)
            obj.put("isDefault", c.isDefault)
            catArr.put(obj)
        }
        root.put("categories", catArr)

        return root.toString(2)
    }

    // Import from JSON
    suspend fun importFromJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (root.has("notes")) {
                val arr = root.getJSONArray("notes")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    insertNote(
                        NoteEntity(
                            title = obj.optString("title", "Imported Note"),
                            content = obj.optString("content", ""),
                            category = obj.optString("category", "Other"),
                            colorHex = obj.optString("colorHex", "#FFF9C4"),
                            tags = obj.optString("tags", ""),
                            isPinned = obj.optBoolean("isPinned", false),
                            isArchived = obj.optBoolean("isArchived", false),
                            metadataJson = obj.optString("metadataJson", "{}")
                        )
                    )
                }
            }
            if (root.has("tasks")) {
                val arr = root.getJSONArray("tasks")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val dDate = obj.optLong("dueDate", -1L)
                    insertTask(
                        TaskEntity(
                            title = obj.optString("title", "Imported Task"),
                            description = obj.optString("description", ""),
                            dueDate = if (dDate > 0) dDate else null,
                            dueTime = obj.optString("dueTime", "").takeIf { it.isNotBlank() },
                            priority = obj.optString("priority", "MEDIUM"),
                            status = obj.optString("status", "PENDING"),
                            category = obj.optString("category", "Tasks"),
                            tags = obj.optString("tags", "")
                        )
                    )
                }
            }
            if (root.has("transactions")) {
                val arr = root.getJSONArray("transactions")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    insertTransaction(
                        TransactionEntity(
                            title = obj.optString("title", "Imported Transaction"),
                            amount = obj.optDouble("amount", 0.0),
                            type = obj.optString("type", "EXPENSE"),
                            category = obj.optString("category", "General"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            notes = obj.optString("notes", ""),
                            currency = obj.optString("currency", "$")
                        )
                    )
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
