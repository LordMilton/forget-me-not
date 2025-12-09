package com.example.everydaytodolist.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.everydaytodolist.data.Todo
import com.example.everydaytodolist.data.TodoSorter
import com.example.everydaytodolist.ui.components.ListItem
import com.example.everydaytodolist.ui.theme.EverydayToDoListTheme
import java.time.LocalTime

@Composable
fun TodoList(
    data: List<Todo>,
    onFabClicked: () -> Unit,
    onItemEditClicked: (Int) -> Unit,
    onItemDeleteClicked: (Int) -> Unit,
    onItemCompletedClicked: (Int) -> Unit,
    onItemSnoozeClicked: (Int, Int?) -> Unit,
    onSortClicked: (TodoSorter.SortMethod, Boolean) -> Unit,
    modifier: Modifier = Modifier)
{
    Box(modifier.fillMaxSize()) {
        Column {
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
                    items = data,
                    key = { it.getUniqueId() }
                ) { todo ->
                    ListItem(
                        todo,
                        { onItemEditClicked(todo.getUniqueId()) },
                        { onItemDeleteClicked(todo.getUniqueId()) },
                        { onItemCompletedClicked(todo.getUniqueId()) },
                        { onItemSnoozeClicked(todo.getUniqueId(), it) },
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 4.dp)
                            .animateItem(),
                        startExpanded = false
                    )
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TodoListPreview() {
    val todo1 = Todo("Clean Dishes", 2, LocalTime.of(20, 0))
    todo1.snooze()
    val todo2 = Todo("Do Laundry", 7, LocalTime.of(12, 0))
    val todo3 = Todo("Brush Teeth", 1, LocalTime.of(9, 0))
    todo3.snooze()
    todo3.snooze()
    todo3.snooze()

    val exampleData = listOf(
        todo1,
        todo2,
        todo3
    )

    EverydayToDoListTheme {
        Scaffold { innerPadding ->
            TodoList(exampleData,
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