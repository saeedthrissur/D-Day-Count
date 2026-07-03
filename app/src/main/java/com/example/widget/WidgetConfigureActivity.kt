package com.example.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.Event
import com.example.data.SettingsManager
import com.example.util.DDayCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Find App Widget ID from Intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // Set default cancel result
        setResult(Activity.RESULT_CANCELED)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            var events by remember { mutableStateOf<List<Event>>(emptyList()) }
            var searchQuery by remember { mutableStateOf("") }
            val coroutineScope = rememberCoroutineScope()

            // Load events offline
            LaunchedEffect(Unit) {
                val db = AppDatabase.getDatabase(this@WidgetConfigureActivity)
                val allEvents = db.eventDao().getAllEvents().firstOrNull() ?: emptyList()
                events = allEvents
            }

            val filteredEvents = remember(events, searchQuery) {
                events.filter { it.title.contains(searchQuery, ignoreCase = true) }
            }

            MaterialTheme {
                Scaffold(
                    topBar = {
                        MediumTopAppBar(
                            title = { Text("Configure Widget", fontWeight = FontWeight.Bold) },
                            actions = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Select a D-Day Event to bind to this home screen widget:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Search
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search Events") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )

                        if (filteredEvents.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No events found. Create one first!")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredEvents) { event ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                coroutineScope.launch {
                                                    bindWidget(event)
                                                }
                                            },
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = event.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = event.targetDate,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            val settings = SettingsManager(this@WidgetConfigureActivity).loadSettings()
                                            val ddayText = DDayCalculator.calculateEventDDay(
                                                event,
                                                settings
                                            )
                                            Text(
                                                text = ddayText,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun bindWidget(event: Event) {
        val context = this@WidgetConfigureActivity
        withContext(Dispatchers.IO) {
            // Save binding to SharedPreferences
            val prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
            prefs.edit().putLong("widget_$appWidgetId", event.id).apply()

            // Update Widget UI using DDayWidgetProvider's shared render method
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val db = AppDatabase.getDatabase(context)
            val allEvents = db.eventDao().getAllEvents().firstOrNull() ?: emptyList()

            DDayWidgetProvider.updateWidget(context, appWidgetManager, appWidgetId, allEvents)
        }

        // Return SUCCESS result and finish configuration
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        Toast.makeText(context, "Widget configured successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
