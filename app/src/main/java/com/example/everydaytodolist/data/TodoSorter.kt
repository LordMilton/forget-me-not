package com.example.everydaytodolist.data

import com.example.everydaytodolist.data.todos.ITodo
import kotlin.collections.sortWith

class TodoSorter {
    enum class SortMethod {
        DUE_DATE,
        SNOOZE_COUNT,
        CREATED_DATE,
        TITLE_ALPHANUM
    }

    companion object Factory {
        val dueDateComparator = Comparator { todo1: ITodo, todo2: ITodo ->
            val todo1Time = todo1.getNextOccurrence().time
            val todo2Time = todo2.getNextOccurrence().time
            // Can't just do the math and convert to int (comparator needs an int), it overflows when there's only like a month difference
            if(todo1Time > todo2Time)       1
            else if(todo1Time < todo2Time) -1
            else                            0
        }
        val snoozeComparator = Comparator { todo1: ITodo, todo2: ITodo ->
            todo1.getTimesSnoozedSinceLastCompletion() - todo2.getTimesSnoozedSinceLastCompletion()
        }.then(dueDateComparator)
        val createdComparator = Comparator { todo1: ITodo, todo2: ITodo ->
            todo1.uniqueId - todo2.uniqueId
        }
        val titleComparator = Comparator { todo1: ITodo, todo2: ITodo ->
            todo1.title.compareTo(todo2.title, ignoreCase = true)
        }


        fun sort(todoList: MutableList<ITodo>, method: SortMethod, reversed: Boolean = false) {
            val comparator = when(method) {
                SortMethod.DUE_DATE -> dueDateComparator
                SortMethod.SNOOZE_COUNT -> snoozeComparator.reversed()
                SortMethod.CREATED_DATE -> createdComparator
                SortMethod.TITLE_ALPHANUM -> titleComparator
            }
            todoList.sortWith(if(reversed) comparator.reversed() else comparator)
        }
    }
}