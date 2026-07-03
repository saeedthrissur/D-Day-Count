package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import com.example.data.Event
import com.example.data.EventType
import com.example.data.Group
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.firstOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

object BackupHelper {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun exportToCsv(context: Context, uri: Uri, db: AppDatabase): Boolean {
        return try {
            val events = db.eventDao().getAllEvents().firstOrNull() ?: emptyList()
            val groups = db.groupDao().getAllGroups().firstOrNull() ?: emptyList()
            val groupMap = groups.associateBy { it.id }

            val csvContent = StringBuilder()
            csvContent.append("Title,Target Date,Target Time,Event Type,Group,Note\n")

            for (event in events) {
                val groupName = groupMap[event.groupId]?.name ?: ""
                val line = listOf(
                    event.title,
                    event.targetDate,
                    event.targetTime ?: "",
                    event.eventType.name,
                    groupName,
                    event.eventNote ?: ""
                ).joinToString(",") { it.escapeCsv() }
                csvContent.append(line).append("\n")
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importFromCsv(context: Context, uri: Uri, db: AppDatabase): Int {
        return try {
            var importCount = 0
            val existingGroups = db.groupDao().getAllGroups().firstOrNull() ?: emptyList()
            val groupMap = existingGroups.associateBy { it.name.lowercase() }.toMutableMap()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val header = reader.readLine()
                    var line = reader.readLine()
                    var manualIndex = 1000

                    while (line != null) {
                        if (line.trim().isEmpty()) {
                            line = reader.readLine()
                            continue
                        }
                        val tokens = parseCsvLine(line)
                        if (tokens.size >= 4) {
                            val title = tokens[0]
                            val targetDate = tokens[1]
                            val targetTime = tokens.getOrNull(2)?.takeIf { it.isNotEmpty() }
                            val eventTypeStr = tokens.getOrNull(3) ?: "COUNTDOWN"
                            val groupName = tokens.getOrNull(4) ?: ""
                            val note = tokens.getOrNull(5)

                            val isValidDate = try {
                                LocalDate.parse(targetDate)
                                true
                            } catch (e: Exception) {
                                false
                            }

                            if (isValidDate && title.isNotEmpty()) {
                                val eventType = try {
                                    EventType.valueOf(eventTypeStr.uppercase())
                                } catch (e: Exception) {
                                    EventType.COUNTDOWN
                                }

                                var groupId: Long? = null
                                if (groupName.isNotEmpty()) {
                                    val key = groupName.lowercase()
                                    var groupObj = groupMap[key]
                                    if (groupObj == null) {
                                        val newId = db.groupDao().insertGroup(Group(name = groupName, iconIdentifier = "folder"))
                                        groupObj = Group(id = newId, name = groupName, iconIdentifier = "folder")
                                        groupMap[key] = groupObj
                                    }
                                    groupId = groupObj.id
                                }

                                val event = Event(
                                    title = title,
                                    targetDate = targetDate,
                                    targetTime = targetTime,
                                    eventType = eventType,
                                    themeColorHex = "#3B82F6",
                                    customLocalIconId = "alarm",
                                    groupId = groupId,
                                    manualSortOrder = manualIndex++,
                                    eventNote = note,
                                    localImageUri = null
                                )
                                db.eventDao().insertEvent(event)
                                importCount++
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
            importCount
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    suspend fun exportToJson(context: Context, uri: Uri, db: AppDatabase): Boolean {
        return try {
            val events = db.eventDao().getAllEvents().firstOrNull() ?: emptyList()
            val groups = db.groupDao().getAllGroups().firstOrNull() ?: emptyList()

            val adapter = moshi.adapter(AppBackup::class.java)
            val backup = AppBackup(groups, events)
            val jsonString = adapter.toJson(backup)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromJson(context: Context, uri: Uri, db: AppDatabase): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val adapter = moshi.adapter(AppBackup::class.java)
                val backup = adapter.fromJson(jsonString) ?: return false

                for (group in backup.groups) {
                    db.groupDao().insertGroup(group)
                }
                db.eventDao().insertEvents(backup.events)
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun String.escapeCsv(): String {
        if (this.contains(",") || this.contains("\"") || this.contains("\n")) {
            return "\"" + this.replace("\"", "\"\"") + "\""
        }
        return this
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        val sb = java.lang.StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class AppBackup(
    val groups: List<Group>,
    val events: List<Event>
)
