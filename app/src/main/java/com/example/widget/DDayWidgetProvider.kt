package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.Event
import com.example.data.SettingsManager
import com.example.util.CalculationSettings
import com.example.util.DDayCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.InputStream

class DDayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val db = AppDatabase.getDatabase(context)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            val eventsFlow = db.eventDao().getAllEvents()
            val events = eventsFlow.firstOrNull() ?: emptyList()

            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, events)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        val db = AppDatabase.getDatabase(context)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            val eventsFlow = db.eventDao().getAllEvents()
            val events = eventsFlow.firstOrNull() ?: emptyList()
            updateWidget(context, appWidgetManager, appWidgetId, events, newOptions)
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, DDayWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, DDayWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            events: List<Event>,
            options: Bundle? = null
        ) {
            val views = RemoteViews(context.packageName, R.layout.dday_widget_layout)

            // Read specific event binding from SharedPreferences
            val prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
            val boundEventId = prefs.getLong("widget_$appWidgetId", -1L)

            val boundEvent = if (boundEventId != -1L) {
                events.find { it.id == boundEventId }
            } else {
                // Default fallback to first event
                events.firstOrNull()
            }

            // Detect size options and adapt layout sizing
            val opts = options ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val minHeight = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)

            // Size adapts dynamically
            val isLargeWidth = minWidth >= 180
            val isLargeHeight = minHeight >= 110

            if (boundEvent != null) {
                val settings = SettingsManager(context).loadSettings()
                val ddayText = DDayCalculator.calculateEventDDay(boundEvent, settings)

                views.setTextViewText(R.id.widget_title, boundEvent.title)
                views.setTextViewText(R.id.widget_days, ddayText)
                views.setTextViewText(R.id.widget_date, boundEvent.targetDate)

                // Adjust text sizes dynamically based on widget size class
                if (isLargeWidth && isLargeHeight) {
                    views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                    views.setTextViewTextSize(R.id.widget_days, android.util.TypedValue.COMPLEX_UNIT_SP, 36f)
                    views.setTextViewTextSize(R.id.widget_date, android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                    views.setViewVisibility(R.id.widget_title, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_date, View.VISIBLE)
                } else {
                    if (minWidth < 80 || minHeight < 80) {
                        views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
                        views.setTextViewTextSize(R.id.widget_days, android.util.TypedValue.COMPLEX_UNIT_SP, 18f)
                        views.setViewVisibility(R.id.widget_title, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_date, View.GONE)
                    } else {
                        views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                        views.setTextViewTextSize(R.id.widget_days, android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
                        views.setTextViewTextSize(R.id.widget_date, android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
                        views.setViewVisibility(R.id.widget_title, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_date, View.VISIBLE)
                    }
                }

                // Replicate visual background configurations inside widget
                var backgroundSet = false
                if (boundEvent.backgroundType == "IMAGE" && !boundEvent.localImageUri.isNullOrEmpty()) {
                    val bitmap = loadBitmapFromUri(context, boundEvent.localImageUri)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_background_image, bitmap)
                        views.setViewVisibility(R.id.widget_background_image, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_overlay, View.VISIBLE)
                        backgroundSet = true
                    }
                }

                if (!backgroundSet) {
                    views.setViewVisibility(R.id.widget_background_image, View.GONE)
                    views.setViewVisibility(R.id.widget_overlay, View.GONE)

                    try {
                        val colorHex = if (boundEvent.backgroundType == "GRADIENT" && !boundEvent.gradientColorsHex.isNullOrEmpty()) {
                            boundEvent.gradientColorsHex.split(",")[0]
                        } else {
                            boundEvent.themeColorHex
                        }
                        val parsedColor = AndroidColor.parseColor(colorHex)
                        views.setInt(R.id.widget_background_color, "setColorFilter", parsedColor)
                        views.setViewVisibility(R.id.widget_background_color, View.VISIBLE)
                    } catch (e: Exception) {
                        views.setInt(R.id.widget_background_color, "setColorFilter", AndroidColor.parseColor("#1D1E2C"))
                        views.setViewVisibility(R.id.widget_background_color, View.VISIBLE)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_background_color, View.GONE)
                }
            } else {
                views.setTextViewText(R.id.widget_title, "No active events")
                views.setTextViewText(R.id.widget_days, "D-0")
                views.setTextViewText(R.id.widget_date, "Create one in D-Day Count")

                if (minWidth < 80 || minHeight < 80) {
                    views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
                    views.setTextViewTextSize(R.id.widget_days, android.util.TypedValue.COMPLEX_UNIT_SP, 18f)
                    views.setViewVisibility(R.id.widget_title, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_date, View.GONE)
                } else {
                    views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                    views.setTextViewTextSize(R.id.widget_days, android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
                    views.setTextViewTextSize(R.id.widget_date, android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
                    views.setViewVisibility(R.id.widget_title, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_date, View.VISIBLE)
                }

                views.setViewVisibility(R.id.widget_background_image, View.GONE)
                views.setViewVisibility(R.id.widget_overlay, View.GONE)
                views.setInt(R.id.widget_background_color, "setColorFilter", AndroidColor.parseColor("#1D1E2C"))
                views.setViewVisibility(R.id.widget_background_color, View.VISIBLE)
            }

            // Tap action to launch main app
            val clickIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun loadBitmapFromUri(context: Context, uriStr: String): Bitmap? {
            return try {
                val uri = Uri.parse(uriStr)
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
