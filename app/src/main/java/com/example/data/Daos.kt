package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY id ASC")
    fun getAllGroups(): Flow<List<Group>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY manualSortOrder ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<Event>)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Transaction
    suspend fun updateEventSortOrders(orderedEvents: List<Event>) {
        val updatedEvents = orderedEvents.mapIndexed { index, event ->
            event.copy(manualSortOrder = index)
        }
        insertEvents(updatedEvents)
    }
}
