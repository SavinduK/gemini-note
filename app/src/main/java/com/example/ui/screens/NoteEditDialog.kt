package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.CategoryEntity
import com.example.data.NoteEntity
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditDialog(
    initialNote: NoteEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (NoteEntity) -> Unit,
    onAiAssist: (text: String, category: String, onComplete: (Map<String, String>, String, String) -> Unit) -> Unit
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var selectedCategory by remember { mutableStateOf(initialNote?.category ?: "Projects") }
    var selectedColorHex by remember { mutableStateOf(initialNote?.colorHex ?: "#FFF9C4") }
    var tags by remember { mutableStateOf(initialNote?.tags ?: "") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var isArchived by remember { mutableStateOf(initialNote?.isArchived ?: false) }

    var isAiAssisting by remember { mutableStateOf(false) }

    // Category dropdown state
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Structured fields map
    val structuredFields = remember {
        mutableStateMapOf<String, String>().apply {
            if (initialNote != null) {
                val meta = initialNote.getMetadata()
                val keys = meta.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, meta.optString(k))
                }
            }
        }
    }

    val pastelColors = listOf(
        "#FFF9C4" to "Yellow",
        "#E1F5FE" to "Blue",
        "#FCE4EC" to "Pink",
        "#FFE0B2" to "Orange",
        "#E8F5E9" to "Mint",
        "#EDE7F6" to "Lavender"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(max = 680.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialNote == null) "New Sticky Note" else "Edit Note",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                // AI Assist Button
                OutlinedButton(
                    onClick = {
                        if (content.isNotBlank() || title.isNotBlank()) {
                            isAiAssisting = true
                            onAiAssist(
                                if (content.isNotBlank()) content else title,
                                selectedCategory
                            ) { extractedFields, newTitle, newContent ->
                                isAiAssisting = false
                                if (title.isBlank()) title = newTitle
                                if (newContent.isNotBlank()) content = newContent
                                extractedFields.forEach { (k, v) ->
                                    structuredFields[k] = v
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("ai_assist_button")
                ) {
                    if (isAiAssisting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("AI Fill", fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category & Color row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Exposed Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            val allCatNames = (listOf("Projects", "Travel", "Tech", "Clinical", "Other") + categories.map { it.name }).distinct()
                            allCatNames.forEach { catName ->
                                DropdownMenuItem(
                                    text = { Text(catName) },
                                    onClick = {
                                        selectedCategory = catName
                                        categoryDropdownExpanded = false
                                        // Auto-suggest color based on category
                                        selectedColorHex = when (catName) {
                                            "Projects" -> "#FFF9C4"
                                            "Travel" -> "#E8F5E9"
                                            "Tech" -> "#E1F5FE"
                                            "Clinical" -> "#FCE4EC"
                                            else -> "#FFE0B2"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pastel Color Selector
                Text("Sticky Color:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pastelColors.forEach { (hex, _) ->
                        val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tags Input
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated, e.g. Design, Sprint)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("note_tags_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Content Field
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content / Checklist") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("note_content_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category-specific structured fields
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Structured Details (${selectedCategory})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        when (selectedCategory) {
                            "Travel" -> {
                                StructuredTextField("destination", "Destination / Place", structuredFields)
                                StructuredTextField("budget", "Estimated Budget", structuredFields)
                                StructuredTextField("travelDates", "Travel Dates", structuredFields)
                                StructuredTextField("details", "Useful Details / Packing", structuredFields)
                            }
                            "Projects" -> {
                                StructuredTextField("projectName", "Project Name", structuredFields)
                                StructuredTextField("currentState", "Current State", structuredFields)
                                StructuredTextField("whatNeedsToBeDone", "What Needs To Be Done", structuredFields)
                                StructuredTextField("milestones", "Milestones", structuredFields)
                                StructuredTextField("nextActions", "Next Actions", structuredFields)
                            }
                            "Tech" -> {
                                StructuredTextField("sourceUrl", "Source URL / Reference", structuredFields)
                                StructuredTextField("sourceName", "Source Name", structuredFields)
                                StructuredTextField("specialNotes", "Special Notes / Quirks", structuredFields)
                            }
                            "Clinical" -> {
                                StructuredTextField("conciseSummary", "Concise Summary", structuredFields)
                                StructuredTextField("mechanisms", "Important Mechanisms", structuredFields)
                                StructuredTextField("diagnosticPoints", "Diagnostic Points", structuredFields)
                                StructuredTextField("management", "Clinical Management", structuredFields)
                                StructuredTextField("reference", "Source / Reference", structuredFields)
                            }
                            else -> {
                                StructuredTextField("summary", "Key Takeaway", structuredFields)
                                StructuredTextField("reference", "Reference / Link", structuredFields)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        val metaJson = JSONObject().apply {
                            structuredFields.forEach { (k, v) ->
                                if (v.isNotBlank()) put(k, v)
                            }
                        }.toString()

                        val finalNote = (initialNote ?: NoteEntity(title = title, content = content)).copy(
                            title = if (title.isNotBlank()) title else "Untitled Note",
                            content = content,
                            category = selectedCategory,
                            colorHex = selectedColorHex,
                            tags = tags,
                            isPinned = isPinned,
                            isArchived = isArchived,
                            metadataJson = metaJson,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(finalNote)
                    }
                },
                modifier = Modifier.testTag("save_note_button")
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StructuredTextField(
    key: String,
    label: String,
    fields: MutableMap<String, String>
) {
    OutlinedTextField(
        value = fields[key] ?: "",
        onValueChange = { fields[key] = it },
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
