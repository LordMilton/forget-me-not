package com.example.everydaytodolist.data

import java.time.DayOfWeek

/**
 * Util for translating between DayOfWeek and Calendar.DAY_OF_WEEK
 */
class DayOfWeekUtil {
    companion object Factory {
        fun calendarToTimeDayOfWeek(dayOfWeek: Int): DayOfWeek {
            return DayOfWeek.of(Math.floorMod(dayOfWeek - 2, 7) + 1)
        }

        fun timeToCalendarDayOfWeek(dayOfWeek: DayOfWeek): Int {
            return (Math.floorMod(dayOfWeek.value, 7) + 1)
        }
    }
}