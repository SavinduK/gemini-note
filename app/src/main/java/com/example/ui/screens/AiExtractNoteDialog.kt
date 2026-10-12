package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.DialogProperties
import com.example.ai.ExtractedNoteData
import com.example.data.NoteEntity
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiExtractNoteDialog(
    initialPastedText: String = "",
    onDismiss: () -> Unit,
    onExtractRequest: (text: String, category: String, onResult: (ExtractedNoteData) -> Unit) -> Unit,
    onSaveExtractedNote: (NoteEntity) -> Unit
) {
    var rawInputText by remember { mutableStateOf(initialPastedText) }
    var selectedCategoryOption by remember { mutableStateOf("Auto-detect") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var isExtracting by remember { mutableStateOf(false) }
    var extractedResult by remember { mutableStateOf<ExtractedNoteData?>(null) }

    // Editable fields for confirmation preview
    var previewTitle by remember { mutableStateOf("") }
    var previewContent by remember { mutableStateOf("") }
    var previewCategory by remember { mutableStateOf("") }

    val categoryOptions = listOf("Auto-detect", "Projects", "Travel", "Tech", "Clinical", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(max = 680.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "AI Conversation Note Extractor",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Paste any chat snippet, meeting transcript, or notes. Gemini will structure it according to your chosen category schema with verification preview.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Target Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategoryOption,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category Detection Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categoryOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedCategoryOption = opt
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Paste Input Area
                OutlinedTextField(
                    value = rawInputText,
                    onValueChange = { rawInputText = it },
                    label = { Text("Paste conversation or text here") },
                    placeholder = { Text("e.g. In our meeting we planned a Kyoto trip with $3,500 budget in Autumn...") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth().testTag("ai_raw_input_text")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Extract Button
                Button(
                    onClick = {
                        if (rawInputText.isNotBlank()) {
                            isExtracting = true
                            onExtractRequest(rawInputText, selectedCategoryOption) { result ->
                                isExtracting = false
                                extractedResult = result
                                previewTitle = result.title
                                previewContent = result.content
                                previewCategory = result.category
                            }
                        }
                    },
                    enabled = rawInputText.isNotBlank() && !isExtracting,
                    modifier = Modifier.fillMaxWidth().testTag("ai_analyze_button")
                ) {
                    if (isExtracting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Extracting with Gemini...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Extract Structured Note")
                    }
                }

                // Extracted Result Preview & Confirmation Section
                if (extractedResult != null) {
                    val result = extractedResult!!
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Preview & Confirmation",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = previewCategory,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = previewTitle,
                                onValueChange = { previewTitle = it },
                                label = { Text("Extracted Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("preview_title_input")
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = previewContent,
                                onValueChange = { previewContent = it },
                                label = { Text("Extracted Content") },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth().testTag("preview_content_input")
                            )

                            if (result.structuredFields.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Extracted Schema Fields:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                result.structuredFields.forEach { (k, v) ->
                                    if (v.isNotBlank()) {
                                        Text(
                                            text = "• $k: $v",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (extractedResult != null) {
                Button(
                    onClick = {
                        val result = extractedResult!!
                        val metaJson = JSONObject().apply {
                            result.structuredFields.forEach { (k, v) ->
                                put(k, v)
                            }
                        }.toString()

                        val newNote = NoteEntity(
                            title = previewTitle.ifBlank { result.title },
                            content = previewContent.ifBlank { result.content },
                            category = previewCategory.ifBlank { result.category },
                            colorHex = result.colorHex,
                            tags = result.tags.joinToString(", "),
                            metadataJson = metaJson,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSaveExtractedNote(newNote)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("confirm_save_extracted_note_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Save to Sticky Wall")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
