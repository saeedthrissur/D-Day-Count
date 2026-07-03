package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Event
import com.example.data.Group
import com.example.ui.DDayViewModel
import com.example.util.CalculationSettings
import com.example.util.DDayCalculator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DDayViewModel,
    onNavigateToAddEvent: () -> Unit,
    onNavigateToEditEvent: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val events by viewModel.filteredEvents.collectAsState()
    val allEventsRaw by viewModel.allEventsFlow.collectAsState()
    val groups by viewModel.allGroupsFlow.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSortEditMode by viewModel.isSortEditMode.collectAsState()
    val selectedEventIds by viewModel.selectedEventIds.collectAsState()

    val sortBy by viewModel.sortBy.collectAsState()
    val isReverseSort by viewModel.isReverseSort.collectAsState()

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showShareLimitDialog by remember { mutableStateOf(false) }

    // Map of group IDs to event counts
    val groupEventCounts = remember(allEventsRaw) {
        val counts = mutableMapOf<Long?, Int>()
        counts[null] = allEventsRaw.size
        for (event in allEventsRaw) {
            if (event.groupId != null) {
                counts[event.groupId] = (counts[event.groupId] ?: 0) + 1
            }
        }
        counts
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "D-Day Count",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Divider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(modifier = Modifier.height(16.dp))

                // All groups item
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AllInclusive, contentDescription = null) },
                    label = { Text("All Events") },
                    selected = selectedGroupId == null,
                    onClick = {
                        viewModel.selectedGroupId.value = null
                        scope.launch { drawerState.close() }
                    },
                    badge = { Badge { Text("${groupEventCounts[null] ?: 0}") } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Custom groups
                Text(
                    text = "Custom Groups",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    items(groups) { group ->
                        var showGroupOptions by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        imageVector = when (group.iconIdentifier) {
                                            "favorite" -> Icons.Default.Favorite
                                            "work" -> Icons.Default.Work
                                            "school" -> Icons.Default.School
                                            "family" -> Icons.Default.People
                                            "star" -> Icons.Default.Star
                                            else -> Icons.Default.Folder
                                        },
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(
                                        text = group.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(0.6f)
                                    )
                                },
                                selected = selectedGroupId == group.id,
                                onClick = {
                                    viewModel.selectedGroupId.value = group.id
                                    scope.launch { drawerState.close() }
                                },
                                badge = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Badge { Text("${groupEventCounts[group.id] ?: 0}") }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { showGroupOptions = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = "Group Options",
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            DropdownMenu(
                                expanded = showGroupOptions,
                                onDismissRequest = { showGroupOptions = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete Group") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = {
                                        showGroupOptions = false
                                        viewModel.deleteGroup(group.id)
                                        if (selectedGroupId == group.id) {
                                            viewModel.selectedGroupId.value = null
                                        }
                                        Toast.makeText(context, "Group deleted", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showAddGroupDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Group")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "D-Day Count",
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            val activeGroupLabel = if (selectedGroupId == null) "All Events" else {
                                groups.find { it.id == selectedGroupId }?.name ?: "Group"
                            }
                            Text(
                                text = activeGroupLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Navigation Menu")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.isSortEditMode.value = !isSortEditMode }
                        ) {
                            Icon(
                                imageVector = if (isSortEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                                contentDescription = "Toggle Sort & Edit Mode",
                                tint = if (isSortEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Open Settings")
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                if (!isSortEditMode) {
                    FloatingActionButton(
                        onClick = onNavigateToAddEvent,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("add_event_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search events, notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Advanced sorting controls (Visible or customized during Sort & Edit)
                AnimatedVisibility(
                    visible = isSortEditMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sort & Select Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Reverse Order",
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Switch(
                                        checked = isReverseSort,
                                        onCheckedChange = { viewModel.isReverseSort.value = it },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                mapOf(
                                    "CLOSEST" to "Closest Date",
                                    "A-Z" to "Alphabetical",
                                    "MANUAL" to "Manual Reorder"
                                ).forEach { (key, label) ->
                                    val isSelected = sortBy == key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.sortBy.value = key },
                                        label = { Text(label, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Batch Operations Bar
                AnimatedVisibility(
                    visible = isSortEditMode && selectedEventIds.isNotEmpty(),
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedEventIds.size} selected",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.shareSelectedEvents()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export Card", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { viewModel.deleteSelectedEvents() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Event List
                if (events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Events Found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create a countdown or count-up event using the '+' button below.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.fillMaxWidth(0.8f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(events, key = { it.id }) { event ->
                            EventCardRow(
                                event = event,
                                settings = settings,
                                isSortEditMode = isSortEditMode,
                                isSelected = selectedEventIds.contains(event.id),
                                manualSortActive = sortBy == "MANUAL",
                                onCardClick = {
                                    if (isSortEditMode) {
                                        viewModel.toggleEventSelection(event.id)
                                    } else {
                                        onNavigateToDetail(event.id)
                                    }
                                },
                                onEditClick = { onNavigateToEditEvent(event.id) },
                                onMoveUp = { viewModel.moveEventUp(event) },
                                onMoveDown = { viewModel.moveEventDown(event) },
                                onSelectToggle = { viewModel.toggleEventSelection(event.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for Share Capped Limit
    if (showShareLimitDialog) {
        AlertDialog(
            onDismissRequest = { showShareLimitDialog = false },
            title = { Text("Export Limit Exceeded") },
            text = { Text("You can only select and export up to 100 events as a shared card layout image. Please deselect some events.") },
            confirmButton = {
                TextButton(onClick = { showShareLimitDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Dialog for adding group
    if (showAddGroupDialog) {
        var groupIcon by remember { mutableStateOf("folder") }

        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text("New Custom Group") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Choose Icon:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("folder", "favorite", "work", "school", "family", "star").forEach { identifier ->
                            val isIconSelected = groupIcon == identifier
                            IconButton(
                                onClick = { groupIcon = identifier },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isIconSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = when (identifier) {
                                        "favorite" -> Icons.Default.Favorite
                                        "work" -> Icons.Default.Work
                                        "school" -> Icons.Default.School
                                        "family" -> Icons.Default.People
                                        "star" -> Icons.Default.Star
                                        else -> Icons.Default.Folder
                                    },
                                    contentDescription = null,
                                    tint = if (isIconSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupName.isNotEmpty()) {
                            viewModel.insertGroup(newGroupName, groupIcon)
                            newGroupName = ""
                            showAddGroupDialog = false
                            Toast.makeText(context, "Group created successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EventCardRow(
    event: Event,
    settings: CalculationSettings,
    isSortEditMode: Boolean,
    isSelected: Boolean,
    manualSortActive: Boolean,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSelectToggle: () -> Unit
) {
    val ddayText = DDayCalculator.calculateEventDDay(event, settings)
    val colorHex = if (event.themeColorHex.startsWith("#")) event.themeColorHex else "#3B82F6"
    val parsedColor = remember(colorHex) {
        try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF3B82F6)
        }
    }

    val finalTextColor = remember(event.textColorHex) {
        try {
            Color(android.graphics.Color.parseColor(event.textColorHex))
        } catch (e: Exception) {
            Color.White
        }
    }

    val selectedFontFamily = remember(event.fontFamilyName) {
        when (event.fontFamilyName.lowercase()) {
            "serif" -> FontFamily.Serif
            "sans" -> FontFamily.SansSerif
            "monospace" -> FontFamily.Monospace
            "cursive" -> FontFamily.Cursive
            else -> FontFamily.Default
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Multi-select Checkbox in Edit/Sort Mode
        if (isSortEditMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectToggle() },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag("event_checkbox_${event.id}")
            )
        }

        // Main Event Card
        Card(
            modifier = Modifier
                .weight(1f)
                .height(130.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onCardClick() }
                .testTag("event_card_${event.id}"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = parsedColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Render
                if (event.backgroundType == "IMAGE" && !event.localImageUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = event.localImageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .let { if (event.isBlurredBg) it.blur(10.dp) else it },
                        contentScale = ContentScale.Crop
                    )

                    // Semi-transparent overlay to ensure extreme text readability
                    val overlayColor = remember(event.colorFilterHex) {
                        try {
                            Color(android.graphics.Color.parseColor(event.colorFilterHex ?: "#000000"))
                        } catch (e: Exception) {
                            Color.Black
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayColor.copy(alpha = event.colorFilterOpacity))
                    )
                } else if (event.backgroundType == "GRADIENT" && !event.gradientColorsHex.isNullOrEmpty()) {
                    val splitColors = event.gradientColorsHex.split(",")
                    val parsedColors = remember(splitColors) {
                        splitColors.map { hex ->
                            try {
                                Color(android.graphics.Color.parseColor(hex))
                            } catch (e: Exception) {
                                parsedColor
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(parsedColors))
                    )
                } else {
                    // Solid background gradient for gorgeous premium feel
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        parsedColor,
                                        parsedColor.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                }

                // Repeating background effect decoration
                RepeatingBackgroundEffect(effect = event.backgroundEffect ?: "NONE")

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Details
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (settings.displayIconOnEvents) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = finalTextColor.copy(alpha = 0.9f),
                                        modifier = Modifier.size((16 * event.textSizeAdjust).dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = event.title,
                                    color = finalTextColor,
                                    fontFamily = selectedFontFamily,
                                    fontSize = (20 * event.textSizeAdjust).sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = event.targetDate,
                                color = finalTextColor.copy(alpha = 0.8f),
                                fontSize = (12 * event.textSizeAdjust).sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!event.eventNote.isNullOrEmpty()) {
                            Text(
                                text = event.eventNote,
                                color = finalTextColor.copy(alpha = 0.7f),
                                fontSize = (11 * event.textSizeAdjust).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 200.dp)
                            )
                        }
                    }

                    // Right Column: Big Countdown text & Stickers
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Render asset sticker if selected
                        if (!event.stickerId.isNullOrEmpty()) {
                            val emojiText = when (event.stickerId) {
                                "sticker_love" -> "❤️"
                                "sticker_done" -> "🏆"
                                "sticker_smiley" -> "😊"
                                "sticker_star" -> "⭐"
                                "sticker_party" -> "🎉"
                                else -> ""
                            }
                            if (emojiText.isNotEmpty()) {
                                Text(
                                    text = emojiText,
                                    fontSize = 22.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = ddayText,
                            color = finalTextColor,
                            fontFamily = selectedFontFamily,
                            fontSize = (32 * event.textSizeAdjust).sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                    }
                }
            }
        }

        // Quick edit / sort buttons in Sort & Edit Mode
        if (isSortEditMode) {
            Column(
                modifier = Modifier.padding(start = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (manualSortActive) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowDropUp, contentDescription = "Move Up")
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Move Down")
                    }
                } else {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Event")
                    }
                }
            }
        }
    }
}

// Help scale helper
private fun Modifier.scale(scale: Float): Modifier = this
