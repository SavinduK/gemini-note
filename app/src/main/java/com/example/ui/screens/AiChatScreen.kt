package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiModels
import com.example.data.ChatMessageEntity
import com.example.data.ChatSessionEntity
import com.example.data.NoteEntity
import com.example.data.TaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    sessions: List<ChatSessionEntity>,
    currentSessionId: Long?,
    messages: List<ChatMessageEntity>,
    selectedModel: String,
    isThinking: Boolean,
    errorMessage: String?,
    onOpenDrawer: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectSession: (Long) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (Long) -> Unit,
    onSendMessage: (text: String, bitmap: Bitmap?) -> Unit,
    onSaveAsNote: (NoteEntity) -> Unit,
    onSaveAsTask: (TaskEntity) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showModelMenu by remember { mutableStateOf(false) }
    var showSessionMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Scroll to bottom when messages update
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Android Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                attachedBitmap = bitmap
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // Session Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showSessionMenu = true }
                        ) {
                            val activeTitle = sessions.firstOrNull { it.id == currentSessionId }?.title ?: "Recent Chats"
                            Text(
                                text = activeTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(" ▾", fontSize = 14.sp)
                        }

                        // Model Badge with switcher
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showModelMenu = true }
                        ) {
                            Text(
                                text = "Model: ${GeminiModels.getDisplayName(selectedModel)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        // Session Dropdown
                        DropdownMenu(
                            expanded = showSessionMenu,
                            onDismissRequest = { showSessionMenu = false }
                        ) {
                            sessions.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.title, maxLines = 1) },
                                    trailingIcon = {
                                        IconButton(onClick = { onDeleteSession(s.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        onSelectSession(s.id)
                                        showSessionMenu = false
                                    }
                                )
                            }
                        }

                        // Model Selector Dropdown
                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false }
                        ) {
                            GeminiModels.AVAILABLE_MODELS.forEach { m ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(GeminiModels.getDisplayName(m), fontWeight = if (m == selectedModel) FontWeight.Bold else FontWeight.Normal)
                                            Text(m, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        onSelectModel(m)
                                        showModelMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("menu_drawer_button")) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNewSession, modifier = Modifier.testTag("new_chat_button")) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Chat message list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Start a conversation with Gemini!\nAsk questions, draft notes, or paste transcripts.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isUser = msg.role == "user"
                            ChatBubble(
                                message = msg,
                                isUser = isUser,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(msg.content))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onSaveNote = {
                                    val note = NoteEntity(
                                        title = msg.content.lines().firstOrNull()?.take(35)?.replace(Regex("^#+\\s*"), "") ?: "AI Note",
                                        content = msg.content,
                                        category = "Other",
                                        colorHex = "#FFF9C4",
                                        tags = "AI, Workspace"
                                    )
                                    onSaveAsNote(note)
                                    Toast.makeText(context, "Saved as Sticky Note!", Toast.LENGTH_SHORT).show()
                                },
                                onSaveTask = {
                                    val task = TaskEntity(
                                        title = msg.content.lines().firstOrNull()?.take(40) ?: "New Task from AI",
                                        description = msg.content.take(150),
                                        priority = "MEDIUM"
                                    )
                                    onSaveAsTask(task)
                                    Toast.makeText(context, "Saved as Task!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        if (isThinking) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Gemini is thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Attached image preview if selected
            if (attachedBitmap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            bitmap = attachedBitmap!!.asImageBitmap(),
                            contentDescription = "Attachment preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Image attached", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { attachedBitmap = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove attachment")
                    }
                }
            }

            // Chat Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(40.dp).testTag("attach_image_button")
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach image", tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask Gemini or paste text...", fontSize = 14.sp) },
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() || attachedBitmap != null) {
                                val text = inputText.trim()
                                val bmp = attachedBitmap
                                inputText = ""
                                attachedBitmap = null
                                onSendMessage(text, bmp)
                            }
                        },
                        enabled = !isThinking && (inputText.isNotBlank() || attachedBitmap != null),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (!isThinking && (inputText.isNotBlank() || attachedBitmap != null)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("send_chat_button")
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (!isThinking && (inputText.isNotBlank() || attachedBitmap != null)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessageEntity,
    isUser: Boolean,
    onCopy: () -> Unit,
    onSaveNote: () -> Unit,
    onSaveTask: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Render text with readable typography
                FormattedMarkdownText(
                    text = message.content,
                    isUser = isUser
                )

                // Actions on assistant responses
                if (!isUser) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(15.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = onSaveNote, modifier = Modifier.height(28.dp)) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Note", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = onSaveTask, modifier = Modifier.height(28.dp)) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Task", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedMarkdownText(
    text: String,
    isUser: Boolean
) {
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val lines = text.lines()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            when {
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = textColor)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = textColor)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = textColor)
                    )
                }
                line.startsWith("```") -> {
                    // Code block indicator
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 4.dp)) {
                        Text("• ", color = textColor, fontWeight = FontWeight.Bold)
                        Text(line.substring(2), color = textColor, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> {
                    Text(
                        text = line,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp)
                    )
                }
            }
        }
    }
}
