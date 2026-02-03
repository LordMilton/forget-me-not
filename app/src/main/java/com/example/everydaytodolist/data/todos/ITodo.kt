package com.example.everydaytodolist.data.todos

import java.time.LocalTime
import java.util.Calendar
import java.util.Date

interface ITodo: Cloneable {
    val title: String
    val frequency: Int
    val alarmTime: LocalTime
    val uniqueId: Int
    val maxOccurrences: Int?
    val endDate: Calendar?

    // Can't have last/nextOccurrence in here due to secondary constructor needing them to be vars
    //   and I'm not letting them be public
    // Kinda screws up DRY for any repeated functions requiring them, but alas
    fun getLastOccurrence(): Date
    fun getNextOccurrence(): Date
    fun getNumOccurrences(): Int

    fun getTimesSnoozedSinceLastCompletion(): Int

    fun dueToday(): Boolean
    fun dueBeforeToday(): Boolean
    fun completedToday(): Boolean

    /**
     * Marks the TodoObject as completed today
     *
     * @return True if the TodoObject should be repeated again, False otherwise
     */
    fun markCompleted(): Boolean
    fun snooze(snoozeLength: Int = 1)

    fun snoozeUntil(calendarDate: Calendar, matchAlarmTime: Boolean = true)

    override fun toString(): String

    fun fromPropertiesMap(propertyMap: Map<String,String>): ITodo?

    public override fun clone(): Any

    companion object Factory {
        private var lastUniqueId = 1

        internal fun bumpLastUniqueId(id: Int) {
            lastUniqueId = lastUniqueId.coerceAtLeast(id)
        }

        val defaultName = "New Todo" //TODO Get this into a string resource
        val defaultFrequency = 1
        val defaultAlarmTime = LocalTime.of(9,0)

        fun getNextUniqueId(): Int{
            return ++lastUniqueId
        }
    }
}