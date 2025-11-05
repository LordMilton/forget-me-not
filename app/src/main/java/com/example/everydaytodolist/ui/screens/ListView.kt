package com.example.everydaytodolist.ui.screens

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
    modifier: Modifier = Modifier)
{
    Column(
        modifier
            .scrollable(rememberScrollState(), orientation = Orientation.Vertical)
    ) {
        for((i,todo) in data.withIndex()) {
            ListItem(
                todo,
                { onItemEditClicked(i) },
                { onItemDeleteClicked(i) },
                Modifier.padding(bottom = 8.dp))
        }
    }
    FloatingActionButton(
        onFabClicked
    ) {

    }
}

@Preview
@Composable
fun TodoListPreview() {
    val exampleData = listOf(
        Todo("Clean Dishes", 2, LocalTime.of(20, 0)),
        Todo("Do Laundry", 7, LocalTime.of(12, 0)),
        Todo("Brush Teeth", 1, LocalTime.of(9, 0))
    )

    EverydayToDoListTheme {
        Scaffold { innerPadding ->
            TodoList(exampleData, {}, {}, {}, Modifier.padding(innerPadding))
        }
    }
}