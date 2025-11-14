package com.example.everydaytodolist.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydaytodolist.data.Todo
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
    modifier: Modifier = Modifier)
{
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.verticalScroll(rememberScrollState())
        ) {
            for ((i, todo) in data.withIndex()) {
                ListItem(
                    todo,
                    { onItemEditClicked(i) },
                    { onItemDeleteClicked(i) },
                    { onItemCompletedClicked(i) },
                    { onItemSnoozeClicked(i, it) },
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    startExpanded = false
                )
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
                Modifier.padding(innerPadding)
            )
        }
    }
}