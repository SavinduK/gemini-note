package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.NavigationDestination
import com.example.ui.components.AppDrawerContent
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StickyWallScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.StickyDeskTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StickyDeskTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val showArchivedNotes by viewModel.showArchivedNotes.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allChatSessions by viewModel.allChatSessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val currentChatMessages by viewModel.currentChatMessages.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsStateWithLifecycle()
    val selectedAiModel by viewModel.selectedAiModel.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val aiErrorMessage by viewModel.aiErrorMessage.collectAsStateWithLifecycle()

    // Aggregate all unique tags from notes
    val allTags = remember(activeNotes) {
        activeNotes.flatMap { it.getTagList() }.distinct()
    }

    // Badges counts
    val upcomingCount = remember(allTasks) { viewModel.getUpcomingTasksCount(allTasks) }
    val todayCount = remember(allTasks) { viewModel.getTodayTasksCount(allTasks) }

    // Back handling: close drawer first, or return to StickyWall if on secondary screen
    BackHandler(enabled = drawerState.isOpen || currentDestination != NavigationDestination.StickyWall) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (currentDestination != NavigationDestination.StickyWall) {
            viewModel.navigateTo(NavigationDestination.StickyWall)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                AppDrawerContent(
                    currentDestination = currentDestination,
                    onNavigate = { dest ->
                        viewModel.navigateTo(dest)
                        coroutineScope.launch { drawerState.close() }
                    },
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    upcomingTasksCount = upcomingCount,
                    todayTasksCount = todayCount,
                    categories = allCategories,
                    selectedCategory = selectedCategoryFilter,
                    onSelectCategory = { viewModel.setCategoryFilter(it) },
                    allTags = allTags,
                    selectedTag = selectedTagFilter,
                    onSelectTag = { viewModel.setTagFilter(it) },
                    onAddNewCategory = { name, colorHex ->
                        viewModel.addCategory(name, colorHex)
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentDestination) {
                is NavigationDestination.StickyWall -> {
                    StickyWallScreen(
                        notes = if (showArchivedNotes) archivedNotes else activeNotes,
                        categories = allCategories,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategoryFilter,
                        selectedTag = selectedTagFilter,
                        showArchived = showArchivedNotes,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                        onTagFilterChange = { viewModel.setTagFilter(it) },
                        onToggleShowArchived = { viewModel.toggleShowArchived() },
                        onSaveNote = { viewModel.saveNote(it) },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onTogglePin = { viewModel.togglePin(it) },
                        onToggleArchive = { viewModel.toggleArchive(it) },
                        onExtractWithAi = { text, cat, onComplete ->
                            viewModel.extractNoteFromText(text, cat, onComplete)
                        }
                    )
                }
                is NavigationDestination.TasksUpcoming -> {
                    TasksScreen(
                        tasks = allTasks,
                        categories = allCategories,
                        initialTabIndex = 0,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onSaveTask = { viewModel.saveTask(it) },
                        onToggleStatus = { viewModel.toggleTaskStatus(it) },
                        onDeleteTask = { viewModel.deleteTask(it) }
                    )
                }
                is NavigationDestination.TasksToday -> {
                    TasksScreen(
                        tasks = allTasks,
                        categories = allCategories,
                        initialTabIndex = 1,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onSaveTask = { viewModel.saveTask(it) },
                        onToggleStatus = { viewModel.toggleTaskStatus(it) },
                        onDeleteTask = { viewModel.deleteTask(it) }
                    )
                }
                is NavigationDestination.TasksCalendar -> {
                    TasksScreen(
                        tasks = allTasks,
                        categories = allCategories,
                        initialTabIndex = 2,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onSaveTask = { viewModel.saveTask(it) },
                        onToggleStatus = { viewModel.toggleTaskStatus(it) },
                        onDeleteTask = { viewModel.deleteTask(it) }
                    )
                }
                is NavigationDestination.RecentChats -> {
                    AiChatScreen(
                        sessions = allChatSessions,
                        currentSessionId = currentSessionId,
                        messages = currentChatMessages,
                        selectedModel = selectedAiModel,
                        isThinking = isAiThinking,
                        errorMessage = aiErrorMessage,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onSelectModel = { viewModel.setAiModel(it) },
                        onSelectSession = { viewModel.selectChatSession(it) },
                        onNewSession = { viewModel.createNewChatSession() },
                        onDeleteSession = { viewModel.deleteChatSession(it) },
                        onSendMessage = { text, bitmap -> viewModel.sendChatMessage(text, bitmap) },
                        onSaveAsNote = { viewModel.saveNote(it) },
                        onSaveAsTask = { viewModel.saveTask(it) }
                    )
                }
                is NavigationDestination.Transactions -> {
                    TransactionsScreen(
                        transactions = allTransactions,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onSaveTransaction = { viewModel.saveTransaction(it) },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                    )
                }
                is NavigationDestination.Settings -> {
                    SettingsScreen(
                        currentModel = selectedAiModel,
                        onSelectModel = { viewModel.setAiModel(it) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onExportData = { viewModel.exportDataJson() },
                        onImportData = { json, cb -> viewModel.importDataJson(json, cb) },
                        onResetSampleData = { viewModel.resetToSampleData() },
                        onClearAllData = { viewModel.clearAllData() }
                    )
                }
            }
        }
    }
}
