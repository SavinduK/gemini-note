package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.ui.NavigationDestination

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDrawerContent(
    currentDestination: NavigationDestination,
    onNavigate: (NavigationDestination) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    upcomingTasksCount: Int,
    todayTasksCount: Int,
    categories: List<CategoryEntity>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    allTags: List<String>,
    selectedTag: String?,
    onSelectTag: (String?) -> Unit,
    onAddNewCategory: (name: String, colorHex: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryColor by remember { mutableStateOf("#FFF9C4") }

    val presetColors = listOf(
        "#FFF9C4" to "Yellow",
        "#E1F5FE" to "Blue",
        "#FCE4EC" to "Pink",
        "#FFE0B2" to "Orange",
        "#E8F5E9" to "Mint",
        "#EDE7F6" to "Lavender"
    )

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add New List / Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("List Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_list_name_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Color Tag:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetColors.forEach { (hex, _) ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { newCategoryColor = hex }
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddNewCategory(newCategoryName.trim(), newCategoryColor)
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_new_list_button")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App Title & Logo Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFF9C4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.StickyNote2,
                    contentDescription = null,
                    tint = Color(0xFF5D4037)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "StickyDesk",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = "AI Personal Workspace",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar in Drawer
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("drawer_search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // TASKS SECTION
        SectionHeader(title = "TASKS")

        DrawerNavRow(
            label = "Upcoming",
            icon = Icons.Default.Upcoming,
            isSelected = currentDestination == NavigationDestination.TasksUpcoming,
            badge = if (upcomingTasksCount > 0) "$upcomingTasksCount" else null,
            onClick = { onNavigate(NavigationDestination.TasksUpcoming) },
            testTag = "nav_upcoming_tasks"
        )

        DrawerNavRow(
            label = "Today",
            icon = Icons.Default.Today,
            isSelected = currentDestination == NavigationDestination.TasksToday,
            badge = if (todayTasksCount > 0) "$todayTasksCount" else null,
            onClick = { onNavigate(NavigationDestination.TasksToday) },
            testTag = "nav_today_tasks"
        )

        DrawerNavRow(
            label = "Calendar",
            icon = Icons.Default.CalendarMonth,
            isSelected = currentDestination == NavigationDestination.TasksCalendar,
            onClick = { onNavigate(NavigationDestination.TasksCalendar) },
            testTag = "nav_calendar_tasks"
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(16.dp))

        // WORKSPACE / MAIN APPS
        SectionHeader(title = "WORKSPACE")

        DrawerNavRow(
            label = "Sticky Wall",
            icon = Icons.Default.StickyNote2,
            isSelected = currentDestination == NavigationDestination.StickyWall && selectedCategory == null,
            onClick = {
                onSelectCategory(null)
                onNavigate(NavigationDestination.StickyWall)
            },
            testTag = "nav_sticky_wall"
        )

        DrawerNavRow(
            label = "Recent Chats (AI)",
            icon = Icons.Default.Forum,
            isSelected = currentDestination == NavigationDestination.RecentChats,
            onClick = { onNavigate(NavigationDestination.RecentChats) },
            testTag = "nav_recent_chats"
        )

        DrawerNavRow(
            label = "Transactions & Ledger",
            icon = Icons.Default.Payments,
            isSelected = currentDestination == NavigationDestination.Transactions,
            onClick = { onNavigate(NavigationDestination.Transactions) },
            testTag = "nav_transactions"
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(16.dp))

        // LISTS & CATEGORIES SECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "LISTS")
            IconButton(
                onClick = { showAddCategoryDialog = true },
                modifier = Modifier.size(28.dp).testTag("add_category_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add List",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        val distinctCategories = remember(categories) {
            categories.filter { it.name.isNotBlank() }.distinctBy { it.name.trim().lowercase() }
        }

        distinctCategories.forEach { cat ->
            val isCatSelected = currentDestination == NavigationDestination.StickyWall && selectedCategory?.equals(cat.name, ignoreCase = true) == true
            val catColor = remember(cat.colorHex) {
                try {
                    Color(android.graphics.Color.parseColor(cat.colorHex))
                } catch (_: Exception) {
                    Color(0xFFFFF9C4)
                }
            }

            DrawerCategoryRow(
                name = cat.name,
                color = catColor,
                isSelected = isCatSelected,
                onClick = {
                    onSelectCategory(cat.name)
                    onNavigate(NavigationDestination.StickyWall)
                }
            )
        }

        TextButton(
            onClick = { showAddCategoryDialog = true },
            modifier = Modifier.padding(start = 4.dp).testTag("add_new_list_text_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ Add New List", fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(16.dp))

        // TAGS SECTION
        if (allTags.isNotEmpty()) {
            SectionHeader(title = "TAGS")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                allTags.forEach { tag ->
                    val isTagSelected = selectedTag == tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isTagSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectTag(if (isTagSelected) null else tag)
                                onNavigate(NavigationDestination.StickyWall)
                            }
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isTagSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isTagSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // FOOTER / SETTINGS
        Spacer(modifier = Modifier.weight(1f, fill = false))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(12.dp))

        DrawerNavRow(
            label = "Settings",
            icon = Icons.Default.Settings,
            isSelected = currentDestination == NavigationDestination.Settings,
            onClick = { onNavigate(NavigationDestination.Settings) },
            testTag = "nav_settings"
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun DrawerNavRow(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            )
        }

        if (badge != null) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(0.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawerCategoryRow(
    name: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
