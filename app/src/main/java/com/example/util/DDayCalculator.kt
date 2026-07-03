package com.example.util

import com.example.data.Event
import com.example.data.EventType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

data class CalculationSettings(
    val simpleDaysUnder100: Boolean = true,
    val precise30DaysMonth: Boolean = false,
    val weekdayAsWeek: Boolean = false,
    val absoluteDaysFormat: Boolean = true,
    val hidePastEvents: Boolean = false,
    val displayIconOnEvents: Boolean = true
)

object DDayCalculator {

    fun calculateEventDDay(
        event: Event,
        settings: CalculationSettings,
        currentLocalDate: LocalDate = LocalDate.now()
    ): String {
        val targetDate = try {
            LocalDate.parse(event.targetDate)
        } catch (e: Exception) {
            currentLocalDate
        }

        val calculationType = event.calculationType.uppercase()
        val repeatInterval = if (event.repeatInterval > 0) event.repeatInterval else 1

        when (calculationType) {
            "DAYS_LEFT" -> {
                val totalDays = ChronoUnit.DAYS.between(currentLocalDate, targetDate)
                if (totalDays == 0L) return "D-Day"
                if (totalDays < 0) {
                    val absDays = abs(totalDays)
                    return "D+$absDays"
                }

                if ((settings.simpleDaysUnder100 && totalDays <= 100) || settings.absoluteDaysFormat) {
                    return "D-$totalDays"
                } else {
                    val breakdown = getBreakdownText(currentLocalDate, targetDate, settings)
                    return "D-$breakdown"
                }
            }
            "DAYS_PASSED" -> {
                val totalDays = ChronoUnit.DAYS.between(targetDate, currentLocalDate)
                if (totalDays == 0L) return "D+0"
                if (totalDays < 0) {
                    val absDays = abs(totalDays)
                    return "D-$absDays"
                }

                if ((settings.simpleDaysUnder100 && totalDays <= 100) || settings.absoluteDaysFormat) {
                    return "D+$totalDays"
                } else {
                    val breakdown = getBreakdownText(targetDate, currentLocalDate, settings)
                    return "D+$breakdown"
                }
            }
            "MONTHS" -> {
                val totalDays = abs(ChronoUnit.DAYS.between(targetDate, currentLocalDate))
                val months = if (settings.precise30DaysMonth) {
                    totalDays / 30
                } else {
                    abs(ChronoUnit.MONTHS.between(targetDate, currentLocalDate))
                }
                return "${months}m"
            }
            "WEEKS" -> {
                val totalDays = abs(ChronoUnit.DAYS.between(targetDate, currentLocalDate))
                val weeks = if (settings.weekdayAsWeek) {
                    if (totalDays == 0L) 0L else 1 + (totalDays - 1) / 7
                } else {
                    totalDays / 7
                }
                return "${weeks}w"
            }
            "YEARS_MONTHS" -> {
                val start = if (targetDate.isBefore(currentLocalDate)) targetDate else currentLocalDate
                val end = if (targetDate.isBefore(currentLocalDate)) currentLocalDate else targetDate
                val breakdown = getBreakdownText(start, end, settings)
                return breakdown
            }
            "N_DAY" -> {
                val daysSince = ChronoUnit.DAYS.between(targetDate, currentLocalDate)
                if (daysSince <= 0) {
                    val diff = ChronoUnit.DAYS.between(currentLocalDate, targetDate)
                    return if (diff == 0L) "D-Day" else "D-$diff"
                } else {
                    val k = (daysSince + repeatInterval - 1) / repeatInterval
                    val nextOccurrence = targetDate.plusDays(k * repeatInterval)
                    val diff = ChronoUnit.DAYS.between(currentLocalDate, nextOccurrence)
                    return if (diff == 0L) "D-Day" else "D-$diff"
                }
            }
            "N_WEEK" -> {
                val daysSince = ChronoUnit.DAYS.between(targetDate, currentLocalDate)
                val intervalDays = repeatInterval * 7
                if (daysSince <= 0) {
                    val diff = ChronoUnit.DAYS.between(currentLocalDate, targetDate)
                    return if (diff == 0L) "D-Day" else "D-$diff"
                } else {
                    val k = (daysSince + intervalDays - 1) / intervalDays
                    val nextOccurrence = targetDate.plusDays(k * intervalDays)
                    val diff = ChronoUnit.DAYS.between(currentLocalDate, nextOccurrence)
                    return if (diff == 0L) "D-Day" else "D-$diff"
                }
            }
            "N_MONTH" -> {
                if (currentLocalDate.isBefore(targetDate)) {
                    val diff = ChronoUnit.DAYS.between(currentLocalDate, targetDate)
                    return if (diff == 0L) "D-Day" else "D-$diff"
                } else {
                    var nextOccurrence = targetDate
                    while (nextOccurrence.isBefore(currentLocalDate)) {
                        nextOccurrence = nextOccurrence.plusMonths(repeatInterval.toLong())
                    }
                    val diff = ChronoUnit.DAYS.between(currentLocalDate, nextOccurrence)
                    return if (diff == 0L) "D-Day" else "D-$diff"
                }
            }
            "N_YEAR" -> {
                if (currentLocalDate.isBefore(targetDate)) {
                    val diff = ChronoUnit.DAYS.between(currentLocalDate, targetDate)
                    return if (diff == 0L) "D-Day" else "D-$diff"
                } else {
                    var nextOccurrence = targetDate
                    while (nextOccurrence.isBefore(currentLocalDate)) {
                        nextOccurrence = nextOccurrence.plusYears(repeatInterval.toLong())
                    }
                    val diff = ChronoUnit.DAYS.between(currentLocalDate, nextOccurrence)
                    return if (diff == 0L) "D-Day" else "D-$diff"
                }
            }
            else -> {
                // Backward compatibility using EventType
                return calculateDDay(event.targetDate, event.eventType, settings, currentLocalDate)
            }
        }
    }

    fun calculateDDay(
        targetDateStr: String,
        eventType: EventType,
        settings: CalculationSettings,
        currentLocalDate: LocalDate = LocalDate.now()
    ): String {
        val targetDate = try {
            LocalDate.parse(targetDateStr)
        } catch (e: Exception) {
            currentLocalDate
        }

        val totalDays = ChronoUnit.DAYS.between(currentLocalDate, targetDate)

        when (eventType) {
            EventType.COUNTDOWN -> {
                if (totalDays == 0L) return "D-Day"
                if (totalDays < 0) {
                    val absDays = abs(totalDays)
                    return "D+$absDays"
                }

                if ((settings.simpleDaysUnder100 && totalDays <= 100) || settings.absoluteDaysFormat) {
                    return "D-$totalDays"
                } else {
                    val breakdown = getBreakdownText(currentLocalDate, targetDate, settings)
                    return "D-$breakdown"
                }
            }
            EventType.COUNT_UP -> {
                if (totalDays == 0L) return "D+0"
                if (totalDays > 0) {
                    return "D-$totalDays"
                }

                val elapsedDays = abs(totalDays)
                if ((settings.simpleDaysUnder100 && elapsedDays <= 100) || settings.absoluteDaysFormat) {
                    return "D+$elapsedDays"
                } else {
                    val breakdown = getBreakdownText(targetDate, currentLocalDate, settings)
                    return "D+$breakdown"
                }
            }
            EventType.ANNUAL_REPEAT -> {
                if (currentLocalDate.isBefore(targetDate)) {
                    val diff = ChronoUnit.DAYS.between(currentLocalDate, targetDate)
                    return "D-$diff"
                }

                return getBreakdownText(targetDate, currentLocalDate, settings)
            }
        }
    }

    fun getBreakdownText(start: LocalDate, end: LocalDate, settings: CalculationSettings): String {
        if (start.isAfter(end)) return "0d"

        val totalDays = ChronoUnit.DAYS.between(start, end)
        if (totalDays <= 0) return "0d"

        if (settings.precise30DaysMonth) {
            val months = totalDays / 30
            val days = totalDays % 30
            val years = months / 12
            val remainingMonths = months % 12

            val sb = StringBuilder()
            if (years > 0) sb.append("${years}y ")
            if (remainingMonths > 0) sb.append("${remainingMonths}m ")
            if (days > 0 || sb.isEmpty()) sb.append("${days}d")
            return sb.toString().trim()
        } else {
            var temp = start
            var years = 0
            while (!temp.plusYears(1).isAfter(end)) {
                temp = temp.plusYears(1)
                years++
            }
            var months = 0
            while (!temp.plusMonths(1).isAfter(end)) {
                temp = temp.plusMonths(1)
                months++
            }
            val days = ChronoUnit.DAYS.between(temp, end)

            val sb = StringBuilder()
            if (years > 0) sb.append("${years}y ")
            if (months > 0) sb.append("${months}m ")
            if (days > 0 || sb.isEmpty()) sb.append("${days}d")
            return sb.toString().trim()
        }
    }

    fun getBreakdownDetailText(targetDateStr: String, currentLocalDate: LocalDate = LocalDate.now()): String {
        val targetDate = try {
            LocalDate.parse(targetDateStr)
        } catch (e: Exception) {
            return "0 days"
        }

        val totalDays = ChronoUnit.DAYS.between(currentLocalDate, targetDate)
        val absDays = abs(totalDays)
        val weeks = absDays / 7
        val remainingDays = absDays % 7

        var temp = if (totalDays >= 0) currentLocalDate else targetDate
        val end = if (totalDays >= 0) targetDate else currentLocalDate

        var years = 0
        while (!temp.plusYears(1).isAfter(end)) {
            temp = temp.plusYears(1)
            years++
        }
        var months = 0
        while (!temp.plusMonths(1).isAfter(end)) {
            temp = temp.plusMonths(1)
            months++
        }
        val days = ChronoUnit.DAYS.between(temp, end)

        val breakdownSb = StringBuilder()
        if (years > 0) breakdownSb.append("$years Years, ")
        if (months > 0) breakdownSb.append("$months Months, ")
        breakdownSb.append("$days Days")

        val weeksText = if (weeks > 0) "$weeks Weeks, $remainingDays Days" else "$remainingDays Days"

        val label = if (totalDays >= 0) "Time Remaining" else "Time Elapsed"
        return "$label:\n$breakdownSb\n($weeksText)\n($absDays Days total)"
    }
}
