package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ExtractedNoteData
import com.example.data.CategoryEntity
import com.example.data.NoteEntity
import com.example.ui.components.StickyNoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickyWallScreen(
    notes: List<NoteEntity>,
    categories: List<CategoryEntity>,
    searchQuery: String,
    selectedCategory: String?,
    selectedTag: String?,
    showArchived: Boolean,
    onOpenDrawer: () -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onToggleShowArchived: () -> Unit,
    onSaveNote: (NoteEntity) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onToggleArchive: (NoteEntity) -> Unit,
    onExtractWithAi: (text: String, category: String, onComplete: (ExtractedNoteData) -> Unit) -> Unit
) {
    var noteToEdit by remember { mutableStateOf<NoteEntity?>(null) }
    var showNewNoteDialog by remember { mutableStateOf(false) }
    var showAiExtractDialog by remember { mutableStateOf(false) }

    // Filter notes based on category, tag, and search query
    val filteredNotes = remember(notes, searchQuery, selectedCategory, selectedTag, showArchived) {
        notes.filter { note ->
            val matchesCategory = selectedCategory == null || note.category.equals(selectedCategory, ignoreCase = true)
            val matchesTag = selectedTag == null || note.getTagList().any { it.equals(selectedTag, ignoreCase = true) }
            val matchesSearch = searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.content.contains(searchQuery, ignoreCase = true) ||
                    note.category.contains(searchQuery, ignoreCase = true) ||
                    note.tags.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesTag && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                showArchived -> "Archived Notes"
                                selectedCategory != null -> "Sticky Wall • $selectedCategory"
                                else -> "Sticky Wall"
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("menu_drawer_button")) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // AI Extract Note button
                    IconButton(
                        onClick = { showAiExtractDialog = true },
                        modifier = Modifier.testTag("ai_extract_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Extract Note with AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // + New Note Button
                    IconButton(
                        onClick = {
                            noteToEdit = null
                            showNewNoteDialog = true
                        },
                        modifier = Modifier.testTag("top_bar_add_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Sticky Note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    noteToEdit = null
                    showNewNoteDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_note")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Active tag filter banner if filtered from drawer
            if (selectedTag != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTagFilterChange(null) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Tag: #$selectedTag",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear tag filter",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // ==========================================
            // MAIN STICKY NOTES GRID
            // ==========================================
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.StickyNote2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No notes match \"$searchQuery\"" else "No sticky notes here yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to create a note, or use the sparkles icon to extract notes with AI.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(minSize = 170.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        StickyNoteCard(
                            note = note,
                            onClick = {
                                noteToEdit = note
                                showNewNoteDialog = true
                            },
                            onEdit = {
                                noteToEdit = note
                                showNewNoteDialog = true
                            },
                            onDelete = { onDeleteNote(note) },
                            onTogglePin = { onTogglePin(note) },
                            onToggleArchive = { onToggleArchive(note) }
                        )
                    }
                }
            }
        }
    }

    // Edit / Create Note Dialog
    if (showNewNoteDialog) {
        NoteEditDialog(
            initialNote = noteToEdit,
            categories = categories,
            onDismiss = { showNewNoteDialog = false },
            onSave = { savedNote ->
                onSaveNote(savedNote)
                showNewNoteDialog = false
            },
            onAiAssist = { text, category, onComplete ->
                onExtractWithAi(text, category) { res ->
                    onComplete(res.structuredFields, res.title, res.content)
                }
            }
        )
    }

    // AI Extract Note Dialog
    if (showAiExtractDialog) {
        AiExtractNoteDialog(
            onDismiss = { showAiExtractDialog = false },
            onExtractRequest = { text, cat, onResult ->
                onExtractWithAi(text, cat, onResult)
            },
            onSaveExtractedNote = { extractedNote ->
                onSaveNote(extractedNote)
            }
        )
    }
}
