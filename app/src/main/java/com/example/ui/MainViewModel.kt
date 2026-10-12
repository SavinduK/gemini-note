package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.ai.ExtractedNoteData
import com.example.ai.GeminiClient
import com.example.ai.GeminiModels
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.CategoryEntity
import com.example.data.ChatMessageEntity
import com.example.data.ChatSessionEntity
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class NavigationDestination(val route: String) {
    object StickyWall : NavigationDestination("sticky_wall")
    object TasksUpcoming : NavigationDestination("tasks_upcoming")
    object TasksToday : NavigationDestination("tasks_today")
    object TasksCalendar : NavigationDestination("tasks_calendar")
    object RecentChats : NavigationDestination("recent_chats")
    object Transactions : NavigationDestination("transactions")
    object Settings : NavigationDestination("settings")
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = AppRepository(database)
    val geminiClient = GeminiClient()

    // Navigation & UI State
    private val _currentDestination = MutableStateFlow<NavigationDestination>(NavigationDestination.StickyWall)
    val currentDestination: StateFlow<NavigationDestination> = _currentDestination.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    private val _showArchivedNotes = MutableStateFlow(false)
    val showArchivedNotes: StateFlow<Boolean> = _showArchivedNotes.asStateFlow()

    // Selected AI Model
    private val _selectedAiModel = MutableStateFlow(GeminiModels.FLASH)
    val selectedAiModel: StateFlow<String> = _selectedAiModel.asStateFlow()

    // Data streams
    val activeNotes: StateFlow<List<NoteEntity>> = repository.allActiveNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allChatSessions: StateFlow<List<ChatSessionEntity>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current Chat Session
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    val currentChatMessages: StateFlow<List<ChatMessageEntity>> = _currentSessionId.flatMapLatest { id ->
        if (id != null) {
            repository.getMessagesForSession(id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage: StateFlow<String?> = _aiErrorMessage.asStateFlow()

    init {
        // Auto-select first chat session if available
        viewModelScope.launch {
            allChatSessions.collect { sessions ->
                if (_currentSessionId.value == null && sessions.isNotEmpty()) {
                    _currentSessionId.value = sessions.first().id
                }
            }
        }
    }

    // Navigation Actions
    fun navigateTo(destination: NavigationDestination) {
        _currentDestination.value = destination
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setTagFilter(tag: String?) {
        _selectedTagFilter.value = tag
    }

    fun toggleShowArchived() {
        _showArchivedNotes.value = !_showArchivedNotes.value
    }

    fun setAiModel(model: String) {
        _selectedAiModel.value = model
    }

    // Note Actions
    fun saveNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (note.id == 0L) {
                repository.insertNote(note)
            } else {
                repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(note)
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun toggleArchive(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNote(note.copy(isArchived = !note.isArchived, updatedAt = System.currentTimeMillis()))
        }
    }

    // Task Actions
    fun saveTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (task.id == 0L) {
                repository.insertTask(task)
            } else {
                repository.updateTask(task)
            }
        }
    }

    fun toggleTaskStatus(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextStatus = if (task.isCompleted) "PENDING" else "COMPLETED"
            repository.updateTask(task.copy(status = nextStatus))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task)
        }
    }

    // Transaction Actions
    fun saveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (transaction.id == 0L) {
                repository.insertTransaction(transaction)
            } else {
                repository.updateTransaction(transaction)
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
        }
    }

    // Category Actions
    fun addCategory(name: String, colorHex: String, iconName: String = "folder") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(
                CategoryEntity(
                    name = name,
                    colorHex = colorHex,
                    iconName = iconName
                )
            )
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }

    // Chat Actions
    fun createNewChatSession(title: String = "New Chat") {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val newId = repository.insertSession(
                ChatSessionEntity(
                    title = title,
                    model = _selectedAiModel.value,
                    createdAt = now,
                    updatedAt = now
                )
            )
            _currentSessionId.value = newId
            // Welcome message
            repository.insertMessage(
                ChatMessageEntity(
                    sessionId = newId,
                    role = "model",
                    content = "How can I help you? Ask a question, paste notes to organize, or attach an image.",
                    timestamp = now
                )
            )
        }
    }

    fun selectChatSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                val remaining = allChatSessions.value.filter { it.id != sessionId }
                _currentSessionId.value = remaining.firstOrNull()?.id
            }
        }
    }

    fun sendChatMessage(userText: String, imageBitmap: Bitmap? = null) {
        val currentId = _currentSessionId.value ?: return
        if (userText.isBlank() && imageBitmap == null) return

        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            // 1. Insert user message
            repository.insertMessage(
                ChatMessageEntity(
                    sessionId = currentId,
                    role = "user",
                    content = userText,
                    imageBase64 = null,
                    timestamp = now
                )
            )

            // Update session timestamp & title if first message
            val currentSession = repository.getSessionById(currentId)
            if (currentSession != null && currentSession.title == "New Conversation" && userText.isNotBlank()) {
                val newTitle = userText.take(28).trim()
                repository.updateSession(currentSession.copy(title = newTitle, updatedAt = now))
            } else if (currentSession != null) {
                repository.updateSession(currentSession.copy(updatedAt = now))
            }

            // 2. Fetch history for context
            val history = currentChatMessages.value.map { it.role to it.content }

            _isAiThinking.value = true
            _aiErrorMessage.value = null

            val result = geminiClient.generateChatResponse(
                prompt = userText,
                model = _selectedAiModel.value,
                imageBitmap = imageBitmap,
                history = history
            )

            _isAiThinking.value = false

            result.onSuccess { modelReply ->
                repository.insertMessage(
                    ChatMessageEntity(
                        sessionId = currentId,
                        role = "model",
                        content = modelReply,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }.onFailure { err ->
                _aiErrorMessage.value = err.message
                repository.insertMessage(
                    ChatMessageEntity(
                        sessionId = currentId,
                        role = "model",
                        content = "⚠️ ${err.message ?: "Unable to complete request"}\n\nNote: Make sure your GEMINI_API_KEY is configured in the AI Studio Secrets panel, or verify device network connectivity.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // AI Note Extraction
    fun extractNoteFromText(
        text: String,
        preferredCategory: String = "Auto-detect",
        onComplete: (ExtractedNoteData) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAiThinking.value = true
            val result = geminiClient.extractStructuredNote(
                rawText = text,
                preferredCategory = preferredCategory,
                model = _selectedAiModel.value
            )
            _isAiThinking.value = false
            result.onSuccess { extracted ->
                launch(Dispatchers.Main) {
                    onComplete(extracted)
                }
            }
        }
    }

    // Data management
    suspend fun exportDataJson(): String {
        return repository.exportToJson(
            notes = activeNotes.value + archivedNotes.value,
            tasks = allTasks.value,
            transactions = allTransactions.value,
            categories = allCategories.value
        )
    }

    fun importDataJson(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.importFromJson(jsonString)
            launch(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
            _currentSessionId.value = null
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetToDefaultSample()
        }
    }

    // Computed Counts for Navigation Badges
    fun getUpcomingTasksCount(tasks: List<TaskEntity>): Int {
        val now = System.currentTimeMillis()
        return tasks.count { !it.isCompleted && (it.dueDate == null || it.dueDate >= now - 86400000L) }
    }

    fun getTodayTasksCount(tasks: List<TaskEntity>): Int {
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return tasks.count {
            if (it.isCompleted || it.dueDate == null) false
            else {
                val dueStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.dueDate))
                dueStr == todayStr
            }
        }
    }

    fun getCategoryNotesCount(notes: List<NoteEntity>, categoryName: String): Int {
        return notes.count { it.category.equals(categoryName, ignoreCase = true) }
    }
}
