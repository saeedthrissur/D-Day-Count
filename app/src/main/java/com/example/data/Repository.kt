package com.example.data

import kotlinx.coroutines.flow.Flow

class Repository(private val database: AppDatabase) {
    private val eventDao = database.eventDao()
    private val groupDao = database.groupDao()

    val allEvents: Flow<List<Event>> = eventDao.getAllEvents()
    val allGroups: Flow<List<Group>> = groupDao.getAllGroups()

    suspend fun insertEvent(event: Event): Long = eventDao.insertEvent(event)
    suspend fun insertEvents(events: List<Event>) = eventDao.insertEvents(events)
    suspend fun deleteEvent(event: Event) = eventDao.deleteEvent(event)
    suspend fun deleteEventById(id: Long) = eventDao.deleteEventById(id)
    suspend fun updateEventSortOrders(orderedEvents: List<Event>) = eventDao.updateEventSortOrders(orderedEvents)

    suspend fun insertGroup(group: Group): Long = groupDao.insertGroup(group)
    suspend fun deleteGroupById(id: Long) = groupDao.deleteGroupById(id)
}
