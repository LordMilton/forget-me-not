package com.example.everydaytodolist.data

import kotlin.collections.sortWith

class TodoSorter {
    enum class SortMethod {
        DUE_DATE,
        SNOOZE_COUNT,
        CREATED_DATE,
        TITLE_ALPHANUM
    }

    companion object Factory {
        val dueDateComparator = Comparator { todo1: Todo, todo2: Todo ->
            (todo1.getNextOccurrenceTime().time - todo2.getNextOccurrenceTime().time).toInt()
        }
        val snoozeComparator = Comparator { todo1: Todo, todo2: Todo ->
            todo1.timesSnoozedSinceLastCompletion - todo2.timesSnoozedSinceLastCompletion
        }.then(dueDateComparator)
        val createdComparator = Comparator { todo1: Todo, todo2: Todo ->
            todo1.getUniqueId() - todo2.getUniqueId()
        }
        val titleComparator = Comparator { todo1: Todo, todo2: Todo ->
            todo1.title.compareTo(todo2.title, ignoreCase = true)
        }


        fun sort(todoList: MutableList<Todo>, method: SortMethod, reversed: Boolean = false) {
            val comparator = when(method) {
                SortMethod.DUE_DATE -> dueDateComparator
                SortMethod.SNOOZE_COUNT -> snoozeComparator
                SortMethod.CREATED_DATE -> createdComparator
                SortMethod.TITLE_ALPHANUM -> titleComparator
            }
            todoList.sortWith(if(reversed) comparator.reversed() else comparator)
        }
    }
}