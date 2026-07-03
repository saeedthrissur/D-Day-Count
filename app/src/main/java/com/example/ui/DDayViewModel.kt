package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Event
import com.example.data.Group
import com.example.data.Repository
import com.example.data.SettingsManager
import com.example.util.BackupHelper
import com.example.util.CalculationSettings
import com.example.util.EventExporter
import com.example.widget.DDayWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DDayViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = Repository(db)
    private val settingsManager = SettingsManager(application)

    val allEventsFlow: StateFlow<List<Event>> = repository.allEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allGroupsFlow: StateFlow<List<Group>> = repository.allGroups.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val settings: StateFlow<CalculationSettings> = settingsManager.settingsFlow
    val currentTheme: StateFlow<String> = settingsManager.themeFlow

    val selectedGroupId = MutableStateFlow<Long?>(null)
    val searchQuery = MutableStateFlow("")
    val isSortEditMode = MutableStateFlow(false)
    val selectedEventIds = MutableStateFlow<Set<Long>>(emptySet())

    val sortBy = MutableStateFlow("CLOSEST")
    val isReverseSort = MutableStateFlow(false)

    val filteredEvents: StateFlow<List<Event>> = combine(
        allEventsFlow,
        selectedGroupId,
        searchQuery,
        sortBy,
        isReverseSort,
        settings
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val events = args[0] as List<Event>
        val groupId = args[1] as? Long
        val query = args[2] as String
        val sort = args[3] as String
        val reverse = args[4] as Boolean
        val calSettings = args[5] as CalculationSettings

        var list = events

        if (groupId != null) {
            list = list.filter { it.groupId == groupId }
        }

        if (query.isNotEmpty()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        (it.eventNote?.contains(query, ignoreCase = true) == true)
            }
        }

        if (calSettings.hidePastEvents) {
            val nowStr = java.time.LocalDate.now().toString()
            list = list.filter { it.targetDate >= nowStr }
        }

        list = when (sort) {
            "A-Z" -> list.sortedBy { it.title.lowercase() }
            "MANUAL" -> list.sortedBy { it.manualSortOrder }
            else -> list.sortedBy { it.targetDate }
        }

        if (reverse) {
            list = list.reversed()
        }

        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insertEvent(event: Event) = viewModelScope.launch {
        repository.insertEvent(event)
        triggerWidgetUpdate()
    }

    fun deleteEvent(event: Event) = viewModelScope.launch {
        repository.deleteEvent(event)
        triggerWidgetUpdate()
    }

    fun deleteEventById(id: Long) = viewModelScope.launch {
        repository.deleteEventById(id)
        triggerWidgetUpdate()
    }

    fun insertGroup(name: String, iconIdentifier: String) = viewModelScope.launch {
        repository.insertGroup(Group(name = name, iconIdentifier = iconIdentifier))
    }

    fun deleteGroup(groupId: Long) = viewModelScope.launch {
        repository.deleteGroupById(groupId)
    }

    fun updateSettings(newSettings: CalculationSettings) {
        settingsManager.updateSettings(newSettings)
        triggerWidgetUpdate()
    }

    fun updateTheme(themeName: String) {
        settingsManager.updateTheme(themeName)
    }

    fun toggleEventSelection(eventId: Long) {
        val current = selectedEventIds.value
        selectedEventIds.value = if (current.contains(eventId)) {
            current - eventId
        } else {
            current + eventId
        }
    }

    fun clearSelections() {
        selectedEventIds.value = emptySet()
    }

    fun deleteSelectedEvents() = viewModelScope.launch {
        val idsToDelete = selectedEventIds.value
        for (id in idsToDelete) {
            repository.deleteEventById(id)
        }
        selectedEventIds.value = emptySet()
        isSortEditMode.value = false
        triggerWidgetUpdate()
    }

    fun shareSelectedEvents() {
        val selectedIds = selectedEventIds.value
        val eventsToShare = allEventsFlow.value.filter { selectedIds.contains(it.id) }
        if (eventsToShare.isNotEmpty()) {
            EventExporter.shareEvents(getApplication(), eventsToShare, settings.value)
        }
    }

    fun moveEventUp(event: Event) = viewModelScope.launch {
        val currentList = filteredEvents.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == event.id }
        if (index > 0) {
            val temp = currentList[index]
            currentList[index] = currentList[index - 1]
            currentList[index - 1] = temp
            repository.updateEventSortOrders(currentList)
            triggerWidgetUpdate()
        }
    }

    fun moveEventDown(event: Event) = viewModelScope.launch {
        val currentList = filteredEvents.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == event.id }
        if (index >= 0 && index < currentList.size - 1) {
            val temp = currentList[index]
            currentList[index] = currentList[index + 1]
            currentList[index + 1] = temp
            repository.updateEventSortOrders(currentList)
            triggerWidgetUpdate()
        }
    }

    fun exportCsv(uri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val success = BackupHelper.exportToCsv(getApplication(), uri, db)
        onResult(success)
    }

    fun importCsv(uri: Uri, onResult: (Int) -> Unit) = viewModelScope.launch {
        val count = BackupHelper.importFromCsv(getApplication(), uri, db)
        onResult(count)
        if (count > 0) {
            triggerWidgetUpdate()
        }
    }

    fun exportJson(uri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val success = BackupHelper.exportToJson(getApplication(), uri, db)
        onResult(success)
    }

    fun restoreJson(uri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val success = BackupHelper.restoreFromJson(getApplication(), uri, db)
        onResult(success)
        if (success) {
            triggerWidgetUpdate()
        }
    }

    private fun triggerWidgetUpdate() {
        DDayWidgetProvider.triggerUpdate(getApplication())
    }
}
