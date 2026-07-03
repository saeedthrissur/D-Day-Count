package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DDayViewModel
import com.example.util.CalculationSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DDayViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val theme by viewModel.currentTheme.collectAsState()

    // SAF File Launchers
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(uri) { success ->
                Toast.makeText(
                    context,
                    if (success) "CSV exported successfully!" else "CSV Export failed.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCsv(uri) { count ->
                if (count >= 0) {
                    Toast.makeText(context, "Successfully imported $count events!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "CSV Parsing failed. Check column formats.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportJson(uri) { success ->
                Toast.makeText(
                    context,
                    if (success) "Full state JSON exported!" else "JSON Export failed.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val restoreJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreJson(uri) { success ->
                Toast.makeText(
                    context,
                    if (success) "Full app state restored successfully!" else "JSON Restore failed.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Navigate back")
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
            // Section 1: Themes & Visuals
            Text(
                text = "Appearance & Interface",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    // Theme Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Application Theme", fontWeight = FontWeight.Bold)
                            Text("Current: $theme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }

                        var showThemeMenu by remember { mutableStateOf(false) }
                        Box {
                            Button(onClick = { showThemeMenu = true }) {
                                Text(theme)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                                listOf("System", "Light", "Dark").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            viewModel.updateTheme(option)
                                            showThemeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Divider()

                    // Hide past events switch
                    SettingsSwitchRow(
                        title = "Hide Past Events Globally",
                        description = "Do not display countdown/up events whose target dates are in the past.",
                        checked = settings.hidePastEvents,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(hidePastEvents = it))
                        }
                    )

                    Divider()

                    // Show icons switch
                    SettingsSwitchRow(
                        title = "Display Calendar Icons",
                        description = "Render dynamic calendar indicators directly inside event cards.",
                        checked = settings.displayIconOnEvents,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(displayIconOnEvents = it))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: D-Day Computation Rules
            Text(
                text = "D-Day Calculation Rules",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SettingsSwitchRow(
                        title = "Simple Days Under 100",
                        description = "Display values <= 100 as basic count-down (e.g. D-45) and switch to breakdown format afterwards.",
                        checked = settings.simpleDaysUnder100,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(simpleDaysUnder100 = it))
                        }
                    )

                    Divider()

                    SettingsSwitchRow(
                        title = "Global Absolute Format",
                        description = "Keep D-X / D+X formatting standard regardless of the target duration's length.",
                        checked = settings.absoluteDaysFormat,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(absoluteDaysFormat = it))
                        }
                    )

                    Divider()

                    SettingsSwitchRow(
                        title = "Exact 30-Day Months",
                        description = "Treat exactly 30 elapsed days as a calendar month breakdown.",
                        checked = settings.precise30DaysMonth,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(precise30DaysMonth = it))
                        }
                    )

                    Divider()

                    SettingsSwitchRow(
                        title = "Count same weekday",
                        description = "Standardize weeks with the 8th day starting week 2 (cycles of 7 days).",
                        checked = settings.weekdayAsWeek,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(weekdayAsWeek = it))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Data Import & Export (SAF-based Backup)
            Text(
                text = "Local Backup & Recovery (Offline)",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    // Export CSV Button
                    SettingsActionRow(
                        title = "Export Events to CSV Sheet",
                        description = "Generates a standard Excel-compatible comma-separated file.",
                        icon = Icons.Default.FileDownload,
                        onClick = { exportCsvLauncher.launch("ddaycount_events_backup.csv") }
                    )

                    Divider()

                    // Import CSV Button
                    SettingsActionRow(
                        title = "Import Events from CSV Sheet",
                        description = "Parses and inserts valid rows client-side into Room database.",
                        icon = Icons.Default.FileUpload,
                        onClick = { importCsvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) }
                    )

                    Divider()

                    // JSON Backup Button
                    SettingsActionRow(
                        title = "Backup App State (JSON)",
                        description = "Exports structured backup of groups, orders, and event settings.",
                        icon = Icons.Default.Backup,
                        onClick = { exportJsonLauncher.launch("ddaycount_appstate_backup.json") }
                    )

                    Divider()

                    // JSON Restore Button
                    SettingsActionRow(
                        title = "Restore App State (JSON)",
                        description = "Re-imports full application configurations and events offline.",
                        icon = Icons.Default.Restore,
                        onClick = { restoreJsonLauncher.launch(arrayOf("application/json")) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
