package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Event
import com.example.data.EventType
import com.example.ui.DDayViewModel
import com.example.util.CalculationSettings
import com.example.util.DDayCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

data class PresetEvent(
    val title: String,
    val calculationType: String,
    val repeatInterval: Int = 1,
    val themeColorHex: String,
    val backgroundType: String = "SOLID",
    val gradientColorsHex: String? = null,
    val backgroundEffect: String? = "NONE",
    val fontFamilyName: String = "Default",
    val customLocalIconId: String = "alarm",
    val stickerId: String? = null,
    val eventNote: String = ""
)

val PRESET_TILES = listOf(
    PresetEvent("Our Love Story", "DAYS_PASSED", 1, "#EC4899", "GRADIENT", "#EC4899,#EF4444", "HEARTS", "Cursive", "favorite", "sticker_love", "Our journey together"),
    PresetEvent("Final Exam", "DAYS_LEFT", 1, "#3B82F6", "SOLID", null, "STARS", "Monospace", "school", null, "Studying hard!"),
    PresetEvent("BTS Concert", "DAYS_LEFT", 1, "#8B5CF6", "GRADIENT", "#8B5CF6,#EC4899", "SPARKLES", "Sans", "star", null, "Army power"),
    PresetEvent("My Birthday", "N_YEAR", 1, "#F59E0B", "SOLID", null, "NONE", "Default", "cake", "sticker_party", "Another year older"),
    PresetEvent("Diet Challenge", "DAYS_PASSED", 1, "#10B981", "GRADIENT", "#10B981,#06B6D4", "CHERRIES", "Default", "restaurant", null, "Eating healthy"),
    PresetEvent("No Smoking", "DAYS_PASSED", 1, "#111827", "SOLID", null, "NONE", "Default", "block", "sticker_done", "Clean lungs")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventScreen(
    viewModel: DDayViewModel,
    eventId: Long?, // Null if creating
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val groups by viewModel.allGroupsFlow.collectAsState()
    val events by viewModel.allEventsFlow.collectAsState()
    val globalSettings by viewModel.settings.collectAsState()

    // Find event to edit
    val eventToEdit = remember(eventId, events) {
        events.find { it.id == eventId }
    }

    // Input States
    var title by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var targetTime by remember { mutableStateOf<String?>(null) }
    var eventType by remember { mutableStateOf(EventType.COUNTDOWN) }
    var themeColorHex by remember { mutableStateOf("#3B82F6") }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var eventNote by remember { mutableStateOf("") }
    var localImageUri by remember { mutableStateOf<String?>(null) }

    // New configuration states
    var calculationType by remember { mutableStateOf("DAYS_LEFT") }
    var repeatInterval by remember { mutableStateOf(1) }
    var backgroundType by remember { mutableStateOf("SOLID") }
    var gradientColorsHex by remember { mutableStateOf<String?>(null) }
    var backgroundEffect by remember { mutableStateOf<String?>("NONE") }
    var colorFilterHex by remember { mutableStateOf<String?>("#000000") }
    var colorFilterOpacity by remember { mutableStateOf(0.4f) }
    var isBlurredBg by remember { mutableStateOf(false) }
    var fontFamilyName by remember { mutableStateOf("Default") }
    var textColorHex by remember { mutableStateOf("#FFFFFF") }
    var textSizeAdjust by remember { mutableStateOf(1.0f) }
    var stickerId by remember { mutableStateOf<String?>(null) }

    // Cropper aspect ratio (Default is Original)
    var cropperRatioLabel by remember { mutableStateOf("Original") }

    // Show date picker state
    var showCustomDatePickerDialog by remember { mutableStateOf(false) }

    // Sticker lock states
    var showUnlockStickerDialog by remember { mutableStateOf(false) }
    var stickerToUnlock by remember { mutableStateOf<String?>(null) }
    val unlockedStickers = remember { mutableStateListOf("sticker_love", "sticker_star") }

    // Preset color palettes
    val colorPresets = listOf(
        "#EF4444", // Crimson Red
        "#F97316", // Vibrant Orange
        "#F59E0B", // Amber Gold
        "#10B981", // Emerald Green
        "#3B82F6", // Classic Blue
        "#6366F1", // Indigo Violet
        "#8B5CF6", // Amethyst Purple
        "#EC4899", // Deep Pink
        "#111827", // Cosmic Charcoal Black
        "#14B8A6", // Teal Cyan
        "#06B6D4", // Ocean Turquoise
        "#A855F7"  // Lavender Bloom
    )

    // Load state if editing
    LaunchedEffect(eventToEdit) {
        if (eventToEdit != null) {
            title = eventToEdit.title
            targetDate = eventToEdit.targetDate
            targetTime = eventToEdit.targetTime
            eventType = eventToEdit.eventType
            themeColorHex = eventToEdit.themeColorHex
            selectedGroupId = eventToEdit.groupId
            eventNote = eventToEdit.eventNote ?: ""
            localImageUri = eventToEdit.localImageUri
            
            calculationType = eventToEdit.calculationType
            repeatInterval = eventToEdit.repeatInterval
            backgroundType = eventToEdit.backgroundType
            gradientColorsHex = eventToEdit.gradientColorsHex
            backgroundEffect = eventToEdit.backgroundEffect ?: "NONE"
            colorFilterHex = eventToEdit.colorFilterHex ?: "#000000"
            colorFilterOpacity = eventToEdit.colorFilterOpacity
            isBlurredBg = eventToEdit.isBlurredBg
            fontFamilyName = eventToEdit.fontFamilyName
            textColorHex = eventToEdit.textColorHex
            textSizeAdjust = eventToEdit.textSizeAdjust
            stickerId = eventToEdit.stickerId
        }
    }

    // Photo Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            localImageUri = uri.toString()
            backgroundType = "IMAGE"
        }
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = if (eventId == null) "Create D-Day" else "Edit D-Day",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val newEvent = Event(
                                id = eventId ?: 0,
                                title = title.trim(),
                                targetDate = targetDate,
                                targetTime = targetTime,
                                eventType = eventType,
                                themeColorHex = themeColorHex,
                                customLocalIconId = "alarm",
                                groupId = selectedGroupId,
                                manualSortOrder = eventToEdit?.manualSortOrder ?: 0,
                                eventNote = eventNote.trim().takeIf { it.isNotEmpty() },
                                localImageUri = localImageUri,
                                calculationType = calculationType,
                                repeatInterval = repeatInterval,
                                backgroundType = backgroundType,
                                gradientColorsHex = gradientColorsHex,
                                backgroundEffect = backgroundEffect,
                                colorFilterHex = colorFilterHex,
                                colorFilterOpacity = colorFilterOpacity,
                                isBlurredBg = isBlurredBg,
                                fontFamilyName = fontFamilyName,
                                textColorHex = textColorHex,
                                textSizeAdjust = textSizeAdjust,
                                stickerId = stickerId
                            )

                            viewModel.insertEvent(newEvent)
                            Toast.makeText(context, "Event saved successfully", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_event_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Live Preview Card
            Text(
                text = "Live Preview",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))

            val previewEvent = Event(
                title = if (title.isEmpty()) "Your Title" else title,
                targetDate = targetDate,
                targetTime = targetTime,
                eventType = eventType,
                themeColorHex = themeColorHex,
                customLocalIconId = "alarm",
                groupId = selectedGroupId,
                manualSortOrder = 0,
                eventNote = eventNote,
                localImageUri = localImageUri,
                calculationType = calculationType,
                repeatInterval = repeatInterval,
                backgroundType = backgroundType,
                gradientColorsHex = gradientColorsHex,
                backgroundEffect = backgroundEffect,
                colorFilterHex = colorFilterHex,
                colorFilterOpacity = colorFilterOpacity,
                isBlurredBg = isBlurredBg,
                fontFamilyName = fontFamilyName,
                textColorHex = textColorHex,
                textSizeAdjust = textSizeAdjust,
                stickerId = stickerId
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            ) {
                // Card Rendering
                LiveEventCardPreview(event = previewEvent, settings = globalSettings)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preset Config Tiles Section
            Text(
                text = "Quick Presets",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PRESET_TILES.forEach { preset ->
                    Card(
                        onClick = {
                            title = preset.title
                            calculationType = preset.calculationType
                            repeatInterval = preset.repeatInterval
                            themeColorHex = preset.themeColorHex
                            backgroundType = preset.backgroundType
                            gradientColorsHex = preset.gradientColorsHex
                            backgroundEffect = preset.backgroundEffect
                            fontFamilyName = preset.fontFamilyName
                            stickerId = preset.stickerId
                            eventNote = preset.eventNote
                            if (preset.calculationType == "DAYS_PASSED") {
                                eventType = EventType.COUNT_UP
                            } else if (preset.calculationType.startsWith("N_")) {
                                eventType = EventType.ANNUAL_REPEAT
                            } else {
                                eventType = EventType.COUNTDOWN
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (preset.customLocalIconId) {
                                    "favorite" -> Icons.Default.Favorite
                                    "school" -> Icons.Default.School
                                    "star" -> Icons.Default.Star
                                    "cake" -> Icons.Default.Cake
                                    "restaurant" -> Icons.Default.Restaurant
                                    else -> Icons.Default.Block
                                },
                                contentDescription = null,
                                tint = Color(android.graphics.Color.parseColor(preset.themeColorHex)),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = preset.title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Event Title") },
                placeholder = { Text("e.g., Anniversary, Exam...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Expanded Date Calculation Types Selector
            Text(
                text = "Calculation Type",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Simple Tabs for calculation modes
            var calcCategoryTab by remember { mutableStateOf(0) } // 0: Day Count, 1: Repeat Rules
            TabRow(selectedTabIndex = calcCategoryTab) {
                Tab(selected = calcCategoryTab == 0, onClick = { calcCategoryTab = 0 }) {
                    Text("Day Counting", modifier = Modifier.padding(vertical = 12.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Tab(selected = calcCategoryTab == 1, onClick = { calcCategoryTab = 1 }) {
                    Text("Repeat Rules", modifier = Modifier.padding(vertical = 12.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (calcCategoryTab == 0) {
                // Day Counting Sub-Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mapOf(
                        "DAYS_LEFT" to "Days Left",
                        "DAYS_PASSED" to "Days Passed",
                        "MONTHS" to "Months",
                        "WEEKS" to "Weeks",
                        "YEARS_MONTHS" to "Yr & Mo"
                    ).forEach { (key, label) ->
                        val isSelected = calculationType == key
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                calculationType = key
                                eventType = when (key) {
                                    "DAYS_PASSED" -> EventType.COUNT_UP
                                    else -> EventType.COUNTDOWN
                                }
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Repeat Rules Selector
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mapOf(
                            "N_DAY" to "Every X Days",
                            "N_WEEK" to "Every X Wks",
                            "N_MONTH" to "Every X Mos",
                            "N_YEAR" to "Every X Yrs"
                        ).forEach { (key, label) ->
                            val isSelected = calculationType == key
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    calculationType = key
                                    eventType = EventType.ANNUAL_REPEAT
                                },
                                label = { Text(label, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = repeatInterval.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull() ?: 1
                            repeatInterval = if (v > 0) v else 1
                        },
                        label = { Text("Repeat Interval (X)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Date picker Trigger
            Text(
                text = "Target Date & Time",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedCard(
                    onClick = { showCustomDatePickerDialog = true },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = targetDate, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Time picker
                OutlinedCard(
                    onClick = {
                        val nowTime = LocalTime.now()
                        android.app.TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                targetTime = String.format("%02d:%02d", hour, minute)
                            },
                            nowTime.hour,
                            nowTime.minute,
                            true
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = targetTime ?: "Anytime", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        if (targetTime != null) {
                            IconButton(onClick = { targetTime = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group Assign Selection
            Text(
                text = "Assign Group",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            var showGroupDropdown by remember { mutableStateOf(false) }
            val groupLabel = groups.find { it.id == selectedGroupId }?.name ?: "No Group / Unassigned"

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    onClick = { showGroupDropdown = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = groupLabel, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = showGroupDropdown,
                    onDismissRequest = { showGroupDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DropdownMenuItem(
                        text = { Text("Unassigned / No Group") },
                        onClick = {
                            selectedGroupId = null
                            showGroupDropdown = false
                        }
                    )
                    groups.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g.name) },
                            onClick = {
                                selectedGroupId = g.id
                                showGroupDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fine-Grained Styling Box
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fine-Grained Aesthetic Controls",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Background Options Selection
                    Text("Background Style", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mapOf(
                            "SOLID" to "Solid Preset",
                            "GRADIENT" to "Gradient preset",
                            "IMAGE" to "Photo Picker"
                        ).forEach { (key, label) ->
                            val isSelected = backgroundType == key
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    backgroundType = key
                                    if (key == "GRADIENT" && gradientColorsHex == null) {
                                        gradientColorsHex = "$themeColorHex,#111827"
                                    } else if (key == "IMAGE" && localImageUri == null) {
                                        imagePickerLauncher.launch("image/*")
                                    }
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (backgroundType == "SOLID") {
                        // 12 Preset colors hex row
                        Text("Pick Preset Color", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorPresets.forEach { colorHex ->
                                val colorVal = Color(android.graphics.Color.parseColor(colorHex))
                                val isSelected = themeColorHex == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colorVal)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            themeColorHex = colorHex
                                        }
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp).align(Alignment.Center))
                                    }
                                }
                            }
                        }
                    } else if (backgroundType == "GRADIENT") {
                        // Picker for gradient selections
                        Text("Gradient Presets", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val gradientPresets = listOf(
                                "#EF4444,#991B1B" to "Crimson Fire",
                                "#F97316,#C2410C" to "Vibrant Sun",
                                "#F59E0B,#B45309" to "Amber Gold",
                                "#10B981,#047857" to "Emerald Green",
                                "#3B82F6,#1D4ED8" to "Classic Blue",
                                "#6366F1,#4338CA" to "Indigo Sky",
                                "#8B5CF6,#6D28D9" to "Amethyst Purple",
                                "#EC4899,#BE185D" to "Deep Pink",
                                "#111827,#374151" to "Cosmic Charcoal",
                                "#14B8A6,#0F766E" to "Teal Wave"
                            )

                            gradientPresets.forEach { (colors, name) ->
                                val isSelected = gradientColorsHex == colors
                                val split = colors.split(",")
                                val brush = Brush.linearGradient(listOf(Color(android.graphics.Color.parseColor(split[0])), Color(android.graphics.Color.parseColor(split[1]))))
                                Box(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .width(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(brush)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            gradientColorsHex = colors
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    } else if (backgroundType == "IMAGE") {
                        // Cropper aspect ratio & Image manipulator controls
                        Text("Built-in Offline Cropper Aspect Ratios", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Original", "1:1", "3:4", "3:2", "16:9").forEach { ratio ->
                                val isSelected = cropperRatioLabel == ratio
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        cropperRatioLabel = ratio
                                    },
                                    label = { Text(ratio, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isBlurredBg, onCheckedChange = { isBlurredBg = it })
                            Text("Full Blurred-Background Effect", fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Color Filter Overlay picker
                        Text("Color Filter Overlay", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("#000000", "#EF4444", "#3B82F6", "#10B981", "#EC4899").forEach { overlayHex ->
                                val isSel = colorFilterHex == overlayHex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(overlayHex)))
                                        .border(
                                            width = if (isSel) 3.dp else 1.dp,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                        .clickable { colorFilterHex = overlayHex }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Overlay Opacity: ${(colorFilterOpacity * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Slider(
                            value = colorFilterOpacity,
                            onValueChange = { colorFilterOpacity = it },
                            valueRange = 0.0f..1.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Repeating Background Effects
                    Text("Native Repeating Background Effects", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("NONE" to "No Effect", "HEARTS" to "Hearts ❤️", "STARS" to "Stars ⭐", "CHERRIES" to "Cherries 🍒", "SPARKLES" to "Sparkles ✨").forEach { (key, label) ->
                            val isSelected = backgroundEffect == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { backgroundEffect = key },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Font options
                    Text("Font Typeface", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Default", "Serif", "Sans", "Monospace", "Cursive").forEach { font ->
                            val isSelected = fontFamilyName == font
                            FilterChip(
                                selected = isSelected,
                                onClick = { fontFamilyName = font },
                                label = { Text(font, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text size scale slider
                    Text("Text Size Scale: ${String.format("%.1fx", textSizeAdjust)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Slider(
                        value = textSizeAdjust,
                        onValueChange = { textSizeAdjust = it },
                        valueRange = 0.8f..1.6f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text color hex picker
                    Text("Text Color", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#FFFFFF", "#E5E7EB", "#FEF3C7", "#FCE7F3", "#ECFDF5", "#EFF6FF").forEach { textColor ->
                            val isSelected = textColorHex.uppercase() == textColor.uppercase()
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(textColor)))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .clickable { textColorHex = textColor }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Native asset stickers picker (un-lockable)
                    Text("Local Asset Stickers (Unlockable Offline)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stickers List
                        val stickerPresets = listOf(
                            "sticker_love" to "❤️ Love",
                            "sticker_done" to "🏆 Well Done",
                            "sticker_smiley" to "😊 Smiley",
                            "sticker_star" to "⭐ Star",
                            "sticker_party" to "🎉 Celebration"
                        )

                        // Clear Sticker option
                        FilterChip(
                            selected = stickerId == null,
                            onClick = { stickerId = null },
                            label = { Text("No Sticker", fontSize = 10.sp) }
                        )

                        stickerPresets.forEach { (id, label) ->
                            val isSelected = stickerId == id
                            val isUnlocked = unlockedStickers.contains(id)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isUnlocked) {
                                        stickerId = id
                                    } else {
                                        stickerToUnlock = id
                                        showUnlockStickerDialog = true
                                    }
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, fontSize = 11.sp)
                                        if (!isUnlocked) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notes field
            OutlinedTextField(
                value = eventNote,
                onValueChange = { eventNote = it },
                label = { Text("Event Notes / Description (Optional)") },
                placeholder = { Text("Write reminders or anniversary notes here...") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    // Custom Date Picker Dialog (Grid Calendar View + Spinner Date Picker)
    if (showCustomDatePickerDialog) {
        var pickerModeGrid by remember { mutableStateOf(true) } // true: Calendar Grid, false: Scrollable Spinners
        val initialLocalDate = try {
            LocalDate.parse(targetDate)
        } catch (e: Exception) {
            LocalDate.now()
        }
        var tempSelectedDate by remember { mutableStateOf(initialLocalDate) }

        AlertDialog(
            onDismissRequest = { showCustomDatePickerDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Target Date", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        IconButton(
                            onClick = { pickerModeGrid = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (pickerModeGrid) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Grid Calendar", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { pickerModeGrid = false },
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (!pickerModeGrid) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                        ) {
                            Icon(Icons.Default.Dehaze, contentDescription = "Scrollable Spinners", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                ) {
                    if (pickerModeGrid) {
                        // Grid Calendar Composable
                        CalendarGridView(
                            selectedDate = tempSelectedDate,
                            onDateSelected = { tempSelectedDate = it }
                        )
                    } else {
                        // Scrollable Spinner Wheel Composable
                        DateSpinnerView(
                            selectedDate = tempSelectedDate,
                            onDateSelected = { tempSelectedDate = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        targetDate = tempSelectedDate.toString()
                        showCustomDatePickerDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Unlock Sticker local dialog
    if (showUnlockStickerDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockStickerDialog = false },
            title = { Text("Unlock Sticker Offline") },
            text = { Text("Celebrate! Tap 'Unlock' to instantly unlock this decorative sticker via offline achievement. Zero trackers, zero ads required.") },
            confirmButton = {
                Button(
                    onClick = {
                        stickerToUnlock?.let {
                            unlockedStickers.add(it)
                            stickerId = it
                        }
                        showUnlockStickerDialog = false
                        Toast.makeText(context, "Sticker unlocked successfully!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockStickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Live preview event card
@Composable
fun LiveEventCardPreview(event: Event, settings: CalculationSettings) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Render
        if (event.backgroundType == "IMAGE" && !event.localImageUri.isNullOrEmpty()) {
            AsyncImage(
                model = event.localImageUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (event.isBlurredBg) it.blur(12.dp) else it },
                contentScale = ContentScale.Crop
            )

            // Semi-transparent color filter overlay
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
            // Solid preset background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(parsedColor)
            )
        }

        // Repeating background effect decoration
        RepeatingBackgroundEffect(effect = event.backgroundEffect ?: "NONE")

        // Card details row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = finalTextColor.copy(alpha = 0.9f),
                            modifier = Modifier.size((16 * event.textSizeAdjust).dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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

                // Event description/notes
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

            // Big countdown indicator & Stickers
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
                            fontSize = 32.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
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

@Composable
fun RepeatingBackgroundEffect(effect: String, modifier: Modifier = Modifier) {
    if (effect == "NONE" || effect.isEmpty()) return
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val color = Color.White.copy(alpha = 0.08f)
        
        when (effect.uppercase()) {
            "HEARTS" -> {
                val stepX = 80.dp.toPx()
                val stepY = 80.dp.toPx()
                var y = 20.dp.toPx()
                while (y < height) {
                    var x = 20.dp.toPx()
                    while (x < width) {
                        drawContext.canvas.save()
                        drawContext.canvas.translate(x, y)
                        val path = android.graphics.Path().apply {
                            moveTo(0f, -5f)
                            cubicTo(-10f, -15f, -20f, -5f, -20f, 10f)
                            cubicTo(-20f, 25f, 0f, 35f, 0f, 40f)
                            cubicTo(0f, 35f, 20f, 25f, 20f, 10f)
                            cubicTo(20f, -5f, 10f, -15f, 0f, -5f)
                            close()
                        }
                        drawContext.canvas.nativeCanvas.drawPath(path, android.graphics.Paint().apply {
                            this.color = android.graphics.Color.argb(20, 255, 255, 255)
                            style = android.graphics.Paint.Style.FILL
                        })
                        drawContext.canvas.restore()
                        x += stepX
                    }
                    y += stepY
                }
            }
            "STARS" -> {
                val stepX = 70.dp.toPx()
                val stepY = 70.dp.toPx()
                var y = 15.dp.toPx()
                while (y < height) {
                    var x = 15.dp.toPx()
                    while (x < width) {
                        drawContext.canvas.save()
                        drawContext.canvas.translate(x, y)
                        val path = android.graphics.Path().apply {
                            moveTo(0f, -12f)
                            lineTo(3f, -3f)
                            lineTo(12f, -3f)
                            lineTo(5f, 2f)
                            lineTo(8f, 10f)
                            lineTo(0f, 5f)
                            lineTo(-8f, 10f)
                            lineTo(-5f, 2f)
                            lineTo(-12f, -3f)
                            lineTo(-3f, -3f)
                            close()
                        }
                        drawContext.canvas.nativeCanvas.drawPath(path, android.graphics.Paint().apply {
                            this.color = android.graphics.Color.argb(18, 255, 255, 255)
                            style = android.graphics.Paint.Style.FILL
                        })
                        drawContext.canvas.restore()
                        x += stepX
                    }
                    y += stepY
                }
            }
            "CHERRIES" -> {
                val stepX = 95.dp.toPx()
                val stepY = 95.dp.toPx()
                var y = 25.dp.toPx()
                while (y < height) {
                    var x = 25.dp.toPx()
                    while (x < width) {
                        drawCircle(color = color, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x - 8f, y + 8f))
                        drawCircle(color = color, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x + 8f, y + 10f))
                        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(x - 8f, y + 2f), end = androidx.compose.ui.geometry.Offset(x, y - 8f), strokeWidth = 1.5.dp.toPx())
                        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(x + 8f, y + 4f), end = androidx.compose.ui.geometry.Offset(x, y - 8f), strokeWidth = 1.5.dp.toPx())
                        x += stepX
                    }
                    y += stepY
                }
            }
            "SPARKLES" -> {
                val stepX = 80.dp.toPx()
                val stepY = 80.dp.toPx()
                var y = 20.dp.toPx()
                while (y < height) {
                    var x = 20.dp.toPx()
                    while (x < width) {
                        drawContext.canvas.save()
                        drawContext.canvas.translate(x, y)
                        val path = android.graphics.Path().apply {
                            moveTo(0f, -10f)
                            quadTo(0f, 0f, 10f, 0f)
                            quadTo(0f, 0f, 0f, 10f)
                            quadTo(0f, 0f, -10f, 0f)
                            quadTo(0f, 0f, 0f, -10f)
                            close()
                        }
                        drawContext.canvas.nativeCanvas.drawPath(path, android.graphics.Paint().apply {
                            this.color = android.graphics.Color.argb(22, 255, 255, 255)
                            style = android.graphics.Paint.Style.FILL
                        })
                        drawContext.canvas.restore()
                        x += stepX
                    }
                    y += stepY
                }
            }
        }
    }
}

// 1. Grid Calendar View Composable
@Composable
fun CalendarGridView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var currentMonth by remember { mutableStateOf(selectedDate.withDayOfMonth(1)) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
    val emptyPrecedingDays = if (firstDayOfWeek == 7) 0 else firstDayOfWeek // Map Mon-Sun to correct offset

    Column(modifier = Modifier.fillMaxSize()) {
        // Month pagination header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev Month")
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Weekdays initials
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Days Grid inside a vertical scroll/grid emulation
        val totalCells = emptyPrecedingDays + daysInMonth
        val rows = (totalCells + 6) / 7

        Column(modifier = Modifier.weight(1f)) {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - emptyPrecedingDays + 1
                        val isValidDay = dayNumber in 1..daysInMonth

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isValidDay && currentMonth.withDayOfMonth(dayNumber) == selectedDate)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable(enabled = isValidDay) {
                                    onDateSelected(currentMonth.withDayOfMonth(dayNumber))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isValidDay) {
                                Text(
                                    text = dayNumber.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (currentMonth.withDayOfMonth(dayNumber) == selectedDate) FontWeight.Black else FontWeight.Normal,
                                    color = if (currentMonth.withDayOfMonth(dayNumber) == selectedDate)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. Scrollable Spinner Wheel Date Picker Composable
@Composable
fun DateSpinnerView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var selMonth by remember { mutableStateOf(selectedDate.monthValue) }
    var selDay by remember { mutableStateOf(selectedDate.dayOfMonth) }
    var selYear by remember { mutableStateOf(selectedDate.year) }

    val months = (1..12).toList()
    val years = (1950..2100).toList()
    
    // Number of days adjusts dynamically with selected month and year
    val daysInMonth = remember(selMonth, selYear) {
        try {
            LocalDate.of(selYear, selMonth, 1).lengthOfMonth()
        } catch (e: Exception) {
            31
        }
    }
    val days = (1..daysInMonth).toList()

    LaunchedEffect(selMonth, selDay, selYear) {
        val safeDay = if (selDay > daysInMonth) daysInMonth else selDay
        onDateSelected(LocalDate.of(selYear, selMonth, safeDay))
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Month Spinner
        Box(modifier = Modifier.weight(1.5f)) {
            SpinnerColumn(
                items = months,
                selectedItem = selMonth,
                onItemSelected = { selMonth = it },
                labelFormatter = {
                    java.time.Month.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                }
            )
        }

        // Day Spinner
        Box(modifier = Modifier.weight(1f)) {
            SpinnerColumn(
                items = days,
                selectedItem = if (selDay > daysInMonth) daysInMonth else selDay,
                onItemSelected = { selDay = it }
            )
        }

        // Year Spinner
        Box(modifier = Modifier.weight(1.2f)) {
            SpinnerColumn(
                items = years,
                selectedItem = selYear,
                onItemSelected = { selYear = it }
            )
        }
    }
}

@Composable
fun <T> SpinnerColumn(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelFormatter: (T) -> String = { it.toString() }
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val centerIndex = remember(selectedItem, items) {
        val idx = items.indexOf(selectedItem)
        if (idx != -1) idx else 0
    }

    // Snap and scroll to initial selection
    LaunchedEffect(centerIndex) {
        listState.scrollToItem(centerIndex)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        contentPadding = PaddingValues(vertical = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(items) { item ->
            val isSelected = item == selectedItem
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable {
                        onItemSelected(item)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelFormatter(item),
                    fontSize = if (isSelected) 16.sp else 13.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
