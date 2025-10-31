package com.example.everydaytodolist.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydaytodolist.data.Todo
import com.example.everydaytodolist.ui.theme.EverydayToDoListTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskComposable(
    data: Todo,
    onSave: (Todo) -> Unit,
    modifier: Modifier = Modifier
) {
    var taskName by remember { mutableStateOf(data.title) }
    var frequency by remember { mutableIntStateOf(data.frequencyInDays) }
    var alarmTime by remember { mutableStateOf(data.alarmTime) }
    // Need this dedicated state for the TimePicker
    var timePickerState = rememberTimePickerState(
        initialHour = data.alarmTime.hour,
        initialMinute = data.alarmTime.minute
    )


    var showTimePicker by remember { mutableStateOf(false) }
    // TODO May use this later to allow more than just number of days
    //val frequencyOptions = listOf("Daily", "Weekly", "Monthly", "Once")
    //var showFrequencyMenu by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Task Name
        OutlinedTextField(
            value = taskName,
            onValueChange = { taskName = it },
            label = { Text("Task Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // Frequency Selection
        OutlinedTextField(
            value = "$frequency",
            onValueChange = { frequency = it.toIntOrNull() ?: 1 },
            label = { Text("Frequency") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
        )

        // Alarm Time Selection
        OutlinedTextField(
            value = alarmTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            onValueChange = {},
            readOnly = true,
            label = { Text("Time") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTimePicker = true }
        )

        if (showTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            alarmTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showTimePicker = false }
                    ) {
                        Text("Cancel")
                    }
                }
            ) {
                // TODO Use something that's not experimental
                TimePicker(state = timePickerState)
            }
        }

        // Cancel and Save Buttons
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { onSave(data) },
                Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "Cancel"
                )
            }
            Button(
                onClick = {
                    var newTodo = Todo.copy(data)
                    newTodo.title = taskName
                    newTodo.frequencyInDays = frequency
                    newTodo.alarmTime = alarmTime
                    onSave(newTodo)
                },
                Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "Save"
                )
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}

@Preview(showBackground = true)
@Composable
fun EditTaskComposablePreview() {
    var taskName by remember { mutableStateOf("Do Laundry") }
    var frequency by remember { mutableIntStateOf(1) }
    var time by remember { mutableStateOf(LocalTime.of(18, 0)) }
    val data: Todo = Todo(taskName, frequency, time)

    EverydayToDoListTheme {
        Surface {
            EditTaskComposable(
                data = data,
                onSave = { }
            )
        }
    }
}