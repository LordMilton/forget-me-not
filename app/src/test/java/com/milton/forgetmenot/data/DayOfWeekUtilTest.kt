package com.milton.forgetmenot.data

import com.milton.forgetmenot.data.DayOfWeekUtil.Factory.timeToCalendarDayOfWeek
import com.milton.forgetmenot.data.DayOfWeekUtil.Factory.calendarToTimeDayOfWeek
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.util.Calendar

class DayOfWeekUtilTest {

    @Test
    fun `DayOfWeek translates to Calendar DAY_OF_WEEK`() {
        val saturday = DayOfWeek.SATURDAY
        val monday = DayOfWeek.MONDAY
        val tuesday = DayOfWeek.TUESDAY
        val wednesday = DayOfWeek.WEDNESDAY

        val saturdayExpected = Calendar.SATURDAY
        val mondayExpected = Calendar.MONDAY
        val tuesdayExpected = Calendar.TUESDAY
        val wednesdayExpected = Calendar.WEDNESDAY

        val saturdayTranslated = timeToCalendarDayOfWeek(saturday)
        val mondayTranslated = timeToCalendarDayOfWeek(monday)
        val tuesdayTranslated = timeToCalendarDayOfWeek(tuesday)
        val wednesdayTranslated = timeToCalendarDayOfWeek(wednesday)

        assertEquals(saturdayExpected, saturdayTranslated)
        assertEquals(mondayExpected, mondayTranslated)
        assertEquals(tuesdayExpected, tuesdayTranslated)
        assertEquals(wednesdayExpected, wednesdayTranslated)
    }

    @Test
    fun `Calendar DAY_OF_WEEK translates to DayOfWeek`() {
        val saturday = Calendar.SATURDAY
        val monday = Calendar.MONDAY
        val tuesday = Calendar.TUESDAY
        val wednesday = Calendar.WEDNESDAY

        val saturdayExpected = DayOfWeek.SATURDAY
        val mondayExpected = DayOfWeek.MONDAY
        val tuesdayExpected = DayOfWeek.TUESDAY
        val wednesdayExpected = DayOfWeek.WEDNESDAY

        val saturdayTranslated = calendarToTimeDayOfWeek(saturday)
        val mondayTranslated = calendarToTimeDayOfWeek(monday)
        val tuesdayTranslated = calendarToTimeDayOfWeek(tuesday)
        val wednesdayTranslated = calendarToTimeDayOfWeek(wednesday)

        assertEquals(saturdayExpected, saturdayTranslated)
        assertEquals(mondayExpected, mondayTranslated)
        assertEquals(tuesdayExpected, tuesdayTranslated)
        assertEquals(wednesdayExpected, wednesdayTranslated)
    }
}
