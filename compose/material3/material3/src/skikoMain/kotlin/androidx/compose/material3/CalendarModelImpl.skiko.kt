/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material3

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime


// TODO Support different locales

/**
 * A [CalendarModel] implementation for API >= 26.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal class CalendarModelImpl : CalendarModel {

    override val today
        get(): CalendarDate {
            val systemLocalDate = LocalDate.now()
            return CalendarDate(
                year = systemLocalDate.year,
                month = systemLocalDate.monthNumber,
                dayOfMonth = systemLocalDate.dayOfMonth,
                utcTimeMillis = systemLocalDate.startOfDayMillis
            )
        }

    // TODO Support different locales
    override val firstDayOfWeek: Int = 1

    // TODO Support different locales
    override val weekdayNames: List<Pair<String, String>> = listOf(
        "Monday" to "M",
        "Tuesday" to "T",
        "Wednesday" to "W",
        "Thursday" to "T",
        "Friday" to "F",
        "Saturday" to "S",
        "Sunday" to "S",
    )

    // TODO Support different locales
    override fun getDateInputFormat(locale: CalendarLocale): DateInputFormat {
        return datePatternAsInputFormat(
            "yyyy-MM-dd" // ISO date format
        )
    }

    override fun getCanonicalDate(timeInMillis: Long): CalendarDate {
        val localDate =
            Instant.fromEpochMilliseconds(timeInMillis).toLocalDateTime(utcTimeZoneId)
        return CalendarDate(
            year = localDate.year,
            month = localDate.monthNumber,
            dayOfMonth = localDate.dayOfMonth,
            utcTimeMillis = localDate.startOfDayMillis
        )
    }

    override fun getMonth(timeInMillis: Long): CalendarMonth {
        return getMonth(
            Instant
                .fromEpochMilliseconds(timeInMillis)
                .toLocalDateTime(utcTimeZoneId)
                .date
                .run {
                    LocalDate(year, month, 1)
                }
        )
    }

    override fun getMonth(date: CalendarDate): CalendarMonth {
        return getMonth(LocalDate(date.year, date.month, 1))
    }

    override fun getMonth(year: Int, month: Int): CalendarMonth {
        return getMonth(LocalDate(year, month, 1))
    }

    override fun getDayOfWeek(date: CalendarDate): Int {
        return date.toLocalDate().dayOfWeek.ordinal
    }

    override fun plusMonths(from: CalendarMonth, addedMonthsCount: Int): CalendarMonth {
        if (addedMonthsCount <= 0) return from

        val firstDayLocalDate = from.toLocalDate()
        val laterMonth = firstDayLocalDate + DatePeriod(months = addedMonthsCount)
        return getMonth(laterMonth)
    }

    override fun minusMonths(from: CalendarMonth, subtractedMonthsCount: Int): CalendarMonth {
        if (subtractedMonthsCount <= 0) return from

        val firstDayLocalDate = from.toLocalDate()
        val earlierMonth = firstDayLocalDate - DatePeriod(months = subtractedMonthsCount)
        return getMonth(earlierMonth)
    }

    // TODO Support different locales
    override fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
        locale: CalendarLocale
    ): String =
        CalendarModelImpl.formatWithPattern(utcTimeMillis, pattern, locale)

    // TODO Support different locales
    override fun parse(date: String, pattern: String): CalendarDate? {
        require(pattern == "YYYY-MM-DD") {
            "Only ISO date pattern is supported"
        }
        return try {
            val localDate = LocalDate.parse(date)
            CalendarDate(
                year = localDate.year,
                month = localDate.monthNumber,
                dayOfMonth = localDate.dayOfMonth,
                utcTimeMillis = localDate.startOfDayMillis
            )
        } catch (pe: Exception) {
            null
        }
    }

    override fun toString(): String {
        return "CalendarModel"
    }

    companion object {

        /**
         * Formats a UTC timestamp into a string with a given date format pattern.
         *
         * @param utcTimeMillis a UTC timestamp to format (milliseconds from epoch)
         * @param pattern a date format pattern
         * @param locale the [Locale] to use when formatting the given timestamp
         */
        // TODO Support different locales
        @Suppress("UNUSED_PARAMETER")
        fun formatWithPattern(
            utcTimeMillis: Long,
            pattern: String,
            locale: CalendarLocale
        ): String {
            return Instant
                .fromEpochMilliseconds(utcTimeMillis)
                .toLocalDateTime(utcTimeZoneId)
                .date
                .run {
                    val year = year.toString().padStart(4)
                    val month = monthNumber.toString().padStart(2)
                    val day = dayOfMonth.toString().padStart(2)
                    "$year-$month-$day"
                }
        }
    }

    private fun getMonth(firstDayLocalDate: LocalDate): CalendarMonth {
        val difference = firstDayLocalDate.dayOfWeek.ordinal - firstDayOfWeek
        val daysFromStartOfWeekToFirstOfMonth = if (difference < 0) {
            difference + DaysInWeek
        } else {
            difference
        }
        val firstDayEpochMillis = firstDayLocalDate.startOfDayMillis
        return CalendarMonth(
            year = firstDayLocalDate.year,
            month = firstDayLocalDate.monthNumber,
            numberOfDays = firstDayLocalDate.monthLength(),
            daysFromStartOfWeekToFirstOfMonth = daysFromStartOfWeekToFirstOfMonth,
            startUtcTimeMillis = firstDayEpochMillis
        )
    }

    private fun CalendarMonth.toLocalDate(): LocalDate {
        return Instant.fromEpochMilliseconds(startUtcTimeMillis).toLocalDateTime(utcTimeZoneId).date
    }

    private fun CalendarDate.toLocalDate(): LocalDate {
        return LocalDate(
            this.year,
            this.month,
            this.dayOfMonth
        )
    }
}

internal val utcTimeZoneId: TimeZone = TimeZone.UTC

fun LocalDate.Companion.now() =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LocalDate.monthLength() = monthNumber.monthLength(isLeapYear(year))

// a copy from kotlinx-datetime/dateCalculations.kt
internal fun isLeapYear(year: Int): Boolean {
    val prolepticYear: Long = year.toLong()
    return prolepticYear and 3 == 0L && (prolepticYear % 100 != 0L || prolepticYear % 400 == 0L)
}

// a copy from kotlinx-datetime/dateCalculations.kt
internal fun Int.monthLength(isLeapYear: Boolean): Int =
    when (this) {
        2 -> if (isLeapYear) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

private val LocalDateTime.startOfDayMillis: Long get() =
    LocalDateTime(year, month, dayOfMonth, 0, 0, 0)
        .toInstant(utcTimeZoneId)
        .toEpochMilliseconds()

private val LocalDate.startOfDayMillis: Long get() = atTime(LocalTime(0, 0, 0, 0))
    .toInstant(utcTimeZoneId)
    .toEpochMilliseconds()