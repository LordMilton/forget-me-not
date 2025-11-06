package com.example.everydaytodolist.ui.screens

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.verticalScroll(rememberScrollState())
        ) {
            for ((i, todo) in data.withIndex()) {
                ListItem(
                    todo,
                    { onItemEditClicked(i) },
                    { onItemDeleteClicked(i) },
                    Modifier.padding(bottom = 8.dp)
                )
            }
        }
        FloatingActionButton(
            onFabClicked,
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