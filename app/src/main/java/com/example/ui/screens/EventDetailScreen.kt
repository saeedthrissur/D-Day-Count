package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Event
import com.example.data.EventType
import com.example.ui.DDayViewModel
import com.example.util.DDayCalculator
import com.example.util.EventExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: DDayViewModel,
    eventId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val events by viewModel.allEventsFlow.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val event = remember(eventId, events) {
        events.find { it.id == eventId }
    }

    if (event == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Event not found or deleted")
        }
        return
    }

    val ddayText = DDayCalculator.calculateEventDDay(event, settings)
    val breakdownDetail = DDayCalculator.getBreakdownDetailText(event.targetDate)

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

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showShareOptions by remember { mutableStateOf(false) }
    var includeBgPhotoInShare by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Render
        if (event.backgroundType == "IMAGE" && !event.localImageUri.isNullOrEmpty()) {
            AsyncImage(
                model = event.localImageUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (event.isBlurredBg) it.blur(20.dp) else it },
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
                    .background(Brush.verticalGradient(parsedColors))
            )
        } else {
            // Solid preset background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(parsedColor, parsedColor.copy(alpha = 0.6f), Color(0xFF11131E))
                        )
                    )
            )
        }

        // Repeating background effect decoration
        RepeatingBackgroundEffect(effect = event.backgroundEffect ?: "NONE")

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .size(40.dp)
                                .testTag("detail_back_button")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Navigate back", tint = Color.White)
                        }
                    },
                    actions = {
                        // Share Button
                        IconButton(
                            onClick = { showShareOptions = true },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .size(40.dp)
                                .testTag("detail_share_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share card", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Edit Button
                        IconButton(
                            onClick = { onNavigateToEdit(event.id) },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .size(40.dp)
                                .testTag("detail_edit_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit event", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Delete Button
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .size(40.dp)
                                .testTag("detail_delete_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete event", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Optional sticker emoji overlay
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
                            fontSize = 64.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                Text(
                    text = event.title,
                    color = finalTextColor,
                    fontFamily = selectedFontFamily,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val typeLabel = when (event.calculationType.uppercase()) {
                    "DAYS_LEFT" -> "Days Left (D-)"
                    "DAYS_PASSED" -> "Days Passed (D+)"
                    "MONTHS" -> "Months Duration"
                    "WEEKS" -> "Weeks Duration"
                    "YEARS_MONTHS" -> "Years & Months"
                    "N_DAY" -> "Every ${event.repeatInterval} Days"
                    "N_WEEK" -> "Every ${event.repeatInterval} Weeks"
                    "N_MONTH" -> "Every ${event.repeatInterval} Months"
                    "N_YEAR" -> "Every ${event.repeatInterval} Years"
                    else -> "D-Day Event"
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = typeLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = ddayText,
                    color = finalTextColor,
                    fontFamily = selectedFontFamily,
                    fontSize = 82.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                val targetTimeLabel = if (event.targetTime.isNullOrEmpty()) "" else " at ${event.targetTime}"
                Text(
                    text = "${event.targetDate}$targetTimeLabel",
                    color = finalTextColor.copy(alpha = 0.85f),
                    fontFamily = selectedFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarViewMonth,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = breakdownDetail,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        )
                    }
                }

                if (!event.eventNote.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notes,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Notes",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = event.eventNote,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to permanently delete this event? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvent(event)
                        showDeleteConfirmation = false
                        Toast.makeText(context, "Event deleted successfully", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Share Options Dialog (Custom photo inclusion toggle)
    if (showShareOptions) {
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            title = { Text("Share D-Day Event Card") },
            text = {
                Column {
                    Text("Prepare high-resolution, pixel-perfect exported image. Customize how you share:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeBgPhotoInShare,
                            onCheckedChange = { includeBgPhotoInShare = it },
                            enabled = (event.backgroundType == "IMAGE" && !event.localImageUri.isNullOrEmpty())
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Include Custom Background Photo",
                            fontSize = 14.sp,
                            color = if (event.backgroundType == "IMAGE") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showShareOptions = false
                        EventExporter.shareEvents(
                            context = context,
                            events = listOf(event),
                            settings = settings,
                            includeBackgroundPhotos = includeBgPhotoInShare
                        )
                    }
                ) {
                    Text("Share Image")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
