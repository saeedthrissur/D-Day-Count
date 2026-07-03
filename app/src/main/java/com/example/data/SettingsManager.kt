package com.example.data

import android.content.Context
import com.example.util.CalculationSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ddaycount_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<CalculationSettings> = _settingsFlow.asStateFlow()

    private val _themeFlow = MutableStateFlow(loadTheme())
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    fun loadSettings(): CalculationSettings {
        return CalculationSettings(
            simpleDaysUnder100 = prefs.getBoolean("simpleDaysUnder100", true),
            precise30DaysMonth = prefs.getBoolean("precise30DaysMonth", false),
            weekdayAsWeek = prefs.getBoolean("weekdayAsWeek", false),
            absoluteDaysFormat = prefs.getBoolean("absoluteDaysFormat", true),
            hidePastEvents = prefs.getBoolean("hidePastEvents", false),
            displayIconOnEvents = prefs.getBoolean("displayIconOnEvents", true)
        )
    }

    fun updateSettings(settings: CalculationSettings) {
        prefs.edit()
            .putBoolean("simpleDaysUnder100", settings.simpleDaysUnder100)
            .putBoolean("precise30DaysMonth", settings.precise30DaysMonth)
            .putBoolean("weekdayAsWeek", settings.weekdayAsWeek)
            .putBoolean("absoluteDaysFormat", settings.absoluteDaysFormat)
            .putBoolean("hidePastEvents", settings.hidePastEvents)
            .putBoolean("displayIconOnEvents", settings.displayIconOnEvents)
            .apply()
        _settingsFlow.value = settings
    }

    fun loadTheme(): String {
        return prefs.getString("app_theme", "System") ?: "System"
    }

    fun updateTheme(theme: String) {
        prefs.edit().putString("app_theme", theme).apply()
        _themeFlow.value = theme
    }
}
