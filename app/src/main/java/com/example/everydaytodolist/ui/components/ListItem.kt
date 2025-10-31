package com.example.everydaytodolist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.everydaytodolist.data.Todo
import com.example.everydaytodolist.R
import com.example.everydaytodolist.ui.theme.EverydayToDoListTheme
import java.time.LocalTime

@Composable
fun ListItem(data: Todo, modifier: Modifier = Modifier) {
    Row(modifier) {
        Column(
            Modifier.weight(.75f)
        ) {
            Text(
                data.title,
                fontSize = 20.sp,
                maxLines = 2,
                modifier = Modifier
            )
            Text(
                text = when(data.frequencyInDays) {
                    1 -> stringResource(R.string.frequency_one_day)
                    else -> stringResource(R.string.frequency_multiple_days, data.frequencyInDays)
                }
            )
            Text("Next occurrence...")
        }
        Column(
            Modifier.weight(.25f)
        ) {
            Button(
                {}
            ) {
                Text("Edit")
            }
            Button(
                {}
            ) {
                Text("Delete")
            }
        }
    }
}

@Preview
@Composable
fun ListItemPreview() {
    val sampleData = Todo("Clean Dishes", 1, LocalTime.of(9, 0))

    EverydayToDoListTheme {
        Scaffold() { innerPadding ->
            ListItem(sampleData, Modifier.padding(innerPadding).fillMaxSize())
        }
    }
}