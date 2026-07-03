package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class EventType {
    COUNTDOWN,
    COUNT_UP,
    ANNUAL_REPEAT
}

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconIdentifier: String
)

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetDate: String, // format: "yyyy-MM-dd"
    val targetTime: String?, // format: "HH:mm" (optional)
    val eventType: EventType,
    val themeColorHex: String,
    val customLocalIconId: String,
    val groupId: Long?,
    val manualSortOrder: Int,
    val eventNote: String?,
    val localImageUri: String?,
    
    // Custom calculation & styling fields
    val calculationType: String = "DAYS_LEFT",
    val repeatInterval: Int = 1,
    val backgroundType: String = "SOLID",
    val gradientColorsHex: String? = null,
    val backgroundEffect: String? = "NONE",
    val colorFilterHex: String? = "#000000",
    val colorFilterOpacity: Float = 0.4f,
    val isBlurredBg: Boolean = false,
    val fontFamilyName: String = "Default",
    val textColorHex: String = "#FFFFFF",
    val textSizeAdjust: Float = 1.0f,
    val stickerId: String? = null
)

class Converters {
    @TypeConverter
    fun fromEventType(value: EventType): String {
        return value.name
    }

    @TypeConverter
    fun toEventType(value: String): EventType {
        return try {
            EventType.valueOf(value)
        } catch (e: Exception) {
            EventType.COUNTDOWN
        }
    }
}
