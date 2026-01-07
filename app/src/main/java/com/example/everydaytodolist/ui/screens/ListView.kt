package com.example.everydaytodolist.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydaytodolist.R
import com.example.everydaytodolist.data.DailyTodo
import com.example.everydaytodolist.data.ITodo
import com.example.everydaytodolist.data.TodoSorter
import com.example.everydaytodolist.ui.components.ListItem
import com.example.everydaytodolist.ui.theme.EverydayToDoListTheme
import java.time.LocalTime

const val PARTY_POPPER_EMOJI = "\uD83C\uDF89"

@Composable
fun ListView(
    data: List<ITodo>,
    sortMethod: TodoSorter.SortMethod,
    onFabClicked: () -> Unit,
    onItemEditClicked: (Int) -> Unit,
    onItemDeleteClicked: (Int) -> Unit,
    onItemCompletedClicked: (Int) -> Unit,
    onItemSnoozeClicked: (Int, Int?) -> Unit,
    onSortClicked: (TodoSorter.SortMethod, Boolean) -> Unit,
    modifier: Modifier = Modifier)
{
    val dataComposables = turnDataIntoLazyComposableItems(data, sortMethod, onItemEditClicked, onItemDeleteClicked, onItemCompletedClicked, onItemSnoozeClicked)
    Box(modifier.fillMaxSize()) {
        Column() {
            // MENUBAR
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                var menuDropdownExpanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .padding(0.dp)
                ) {
                    IconButton(onClick = { menuDropdownExpanded = !menuDropdownExpanded }) {
                        Icon(
                            painterResource(R.drawable.baseline_sort_24),
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.background) // Change to onPrimary when implemented
                    }
                    DropdownMenu(
                        expanded = menuDropdownExpanded,
                        onDismissRequest = { menuDropdownExpanded = false }
                    ) {}
                }
                Text(
                    "Todo List",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(4.dp)
                )
                var sortDropdownExpanded by remember { mutableStateOf(false) }
                val onSortDropdownItemClicked: (TodoSorter.SortMethod, Boolean) -> Unit =
                    {   sortMethod: TodoSorter.SortMethod, reversed: Boolean ->
                        sortDropdownExpanded = false
                        onSortClicked(sortMethod, reversed)
                    }
                Box(
                    modifier = Modifier
                        .padding(0.dp)
                ) {
                    IconButton(onClick = { sortDropdownExpanded = !sortDropdownExpanded }) {
                        Icon(
                            painterResource(R.drawable.baseline_sort_24),
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    DropdownMenu(
                        expanded = sortDropdownExpanded,
                        onDismissRequest = { sortDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Due Date") },
                            onClick = { onSortDropdownItemClicked(TodoSorter.SortMethod.DUE_DATE, false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Name") },
                            onClick = { onSortDropdownItemClicked(TodoSorter.SortMethod.TITLE_ALPHANUM, false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Snoozes") },
                            onClick = { onSortDropdownItemClicked(TodoSorter.SortMethod.SNOOZE_COUNT, false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Date Made") },
                            onClick = { onSortDropdownItemClicked(TodoSorter.SortMethod.CREATED_DATE, false) }
                        )
                    }
                }
            }
            // LIST OF TODOS
            LazyColumn(
                modifier = Modifier
            ) {
                items(
                    items = dataComposables,
                    key = { it.uniqueId }
                ) { item ->
                    (item.composable)()
                }
            }
        }
        FloatingActionButton(
            onFabClicked,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(60.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new Todo", //TODO make this a string resource
                Modifier.size(40.dp)
            )
        }
    }
}

fun turnDataIntoLazyComposableItems(
    data: List<ITodo>,
    sortMethod: TodoSorter.SortMethod,
    onItemEditClicked: (Int) -> Unit,
    onItemDeleteClicked: (Int) -> Unit,
    onItemCompletedClicked: (Int) -> Unit,
    onItemSnoozeClicked: (Int, Int?) -> Unit)
: List<LazyComposableItem>
{
    val composableList = mutableListOf<LazyComposableItem>()
    when(data.size) {
        0 -> {
            composableList.add(nothingToDoItem())
        }
        else -> {
            when (sortMethod) {
                TodoSorter.SortMethod.DUE_DATE -> {
                    var firstTodo = true
                    var dueTodayCount = 0
                    var doneWithToday = false
                    for (todo in data) {
                        if (firstTodo) {
                            composableList.add(dueTodayItem())
                            firstTodo = false
                        }
                        if (todo.dueToday()) {
                            dueTodayCount++
                        } else if (!doneWithToday) {
                            doneWithToday = true
                            if (dueTodayCount == 0) {
                                composableList.add(nothingDueTodayItem())
                            }
                            composableList.add(dueLaterItem())
                        }
                        composableList.add(
                            todoToLazyComposableItem(
                                todo,
                                onItemEditClicked,
                                onItemDeleteClicked,
                                onItemCompletedClicked,
                                onItemSnoozeClicked
                            )
                        )
                    }
                }

                else -> {
                    for (todo in data) {
                        composableList.add(
                            todoToLazyComposableItem(
                                todo,
                                onItemEditClicked,
                                onItemDeleteClicked,
                                onItemCompletedClicked,
                                onItemSnoozeClicked
                            )
                        )
                    }
                }
            }
        }
    }

    return composableList
}

fun smallListLabelItem(displayText: String, itemId: Int): LazyComposableItem {
    return LazyComposableItem(
        itemId,
        @Composable {
            Box(
                Modifier.padding(4.dp)
            ) {
                Text(
                    displayText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    )
}

fun dueTodayItem(): LazyComposableItem {
    return smallListLabelItem("Due Today", -1)
}

fun dueLaterItem(): LazyComposableItem {
    return smallListLabelItem("Due Later", -3)
}

fun listPlaceholderItem(displayText: String, itemId: Int): LazyComposableItem {
    return LazyComposableItem(itemId, @Composable {
        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(4.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "$PARTY_POPPER_EMOJI$PARTY_POPPER_EMOJI$PARTY_POPPER_EMOJI",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    displayText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .padding(12.dp)
                )
            }
        }
    })
}

fun nothingDueTodayItem(): LazyComposableItem {
    return listPlaceholderItem("Nothing Else Due Today", -2)
}

fun nothingToDoItem(): LazyComposableItem {
    return listPlaceholderItem("Nothing left to do!", -4)
}

fun todoToLazyComposableItem(
    todo: ITodo,
    onItemEditClicked: (Int) -> Unit,
    onItemDeleteClicked: (Int) -> Unit,
    onItemCompletedClicked: (Int) -> Unit,
    onItemSnoozeClicked: (Int, Int?) -> Unit
): LazyComposableItem {
    return LazyComposableItem(
        todo.uniqueId,
        {
            ListItem(
                todo,
                { onItemEditClicked(todo.uniqueId) },
                { onItemDeleteClicked(todo.uniqueId) },
                { onItemCompletedClicked(todo.uniqueId) },
                { onItemSnoozeClicked(todo.uniqueId, it) },
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp)
                    .animateItem(),
                startExpanded = false
            )
        }
    )
}

class LazyComposableItem(val uniqueId: Int, val composable: @Composable LazyItemScope.() -> Unit)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TodoListSortedByDueDatePreview() {
    val sortMethod = TodoSorter.SortMethod.DUE_DATE

    val todo1 = DailyTodo("Clean Dishes", 2, LocalTime.of(20, 0))
    todo1.snooze()
    val todo2 = DailyTodo("Do Laundry", 7, LocalTime.of(12, 0))
    val todo3 = DailyTodo("Brush Teeth", 1, LocalTime.of(9, 0))
    todo3.snooze()
    todo3.snooze()
    todo3.snooze()

    val exampleData = mutableListOf<ITodo>(
        todo1,
        todo2,
        todo3
    )
    TodoSorter.sort(exampleData, sortMethod)

    EverydayToDoListTheme {
        Scaffold { innerPadding ->
            ListView(exampleData,
                sortMethod,
                {},
                {},
                {},
                {},
                { one, two -> },
                { one, two -> },
                Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview
@Composable
fun TodoListSortedByDueDateEmptyTodayPreview() {
    val sortMethod = TodoSorter.SortMethod.DUE_DATE

    val todo1 = DailyTodo("Clean Dishes", 2, LocalTime.of(20, 0))
    todo1.snooze()
    val todo3 = DailyTodo("Brush Teeth", 1, LocalTime.of(9, 0))
    todo3.snooze()
    todo3.snooze()
    todo3.snooze()

    val exampleData = mutableListOf<ITodo>(
        todo1,
        todo3
    )
    TodoSorter.sort(exampleData, sortMethod)

    EverydayToDoListTheme {
        Scaffold { innerPadding ->
            ListView(exampleData,
                sortMethod,
                {},
                {},
                {},
                {},
                { one, two -> },
                { one, two -> },
                Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview
@Composable
fun TodoListSortedByDueDateEmptyPreview() {
    val sortMethod = TodoSorter.SortMethod.DUE_DATE

    val exampleData = mutableListOf<ITodo>()

    EverydayToDoListTheme {
        Scaffold { innerPadding ->
            ListView(exampleData,
                sortMethod,
                {},
                {},
                {},
                {},
                { one, two -> },
                { one, two -> },
                Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview
@Composable
fun TodoListSortedByCreationPreview() {
    val sortMethod = TodoSorter.SortMethod.CREATED_DATE

    val todo1 = DailyTodo("Clean Dishes", 2, LocalTime.of(20, 0))
    todo1.snooze()
    val todo2 = DailyTodo("Do Laundry", 7, LocalTime.of(12, 0))
    val todo3 = DailyTodo("Brush Teeth", 1, LocalTime.of(9, 0))
    todo3.snooze()
    todo3.snooze()
    todo3.snooze()

    val exampleData = mutableListOf<ITodo>(
        todo1,
        todo2,
        todo3
    )
    TodoSorter.sort(exampleData, sortMethod)

    EverydayToDoListTheme {
        Scaffold { innerPadding ->
            ListView(exampleData,
                sortMethod,
                {},
                {},
                {},
                {},
                { one, two -> },
                { one, two -> },
                Modifier.padding(innerPadding)
            )
        }
    }
}