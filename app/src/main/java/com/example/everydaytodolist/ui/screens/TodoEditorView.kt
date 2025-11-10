package com.example.everydaytodolist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    onCancel: () -> Unit,
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
            textStyle = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        // Frequency Selection
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Every",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 8.dp)
            )
            OutlinedTextField(
                value = "$frequency",
                onValueChange = { frequency = it.toIntOrNull() ?: 1 },
                suffix = { Text(
                    "Days",
                    style = MaterialTheme.typography.bodyLarge
                ) },
                label = { Text("Frequency") },
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier
            )
        }

        Box() {
            // Alarm Time Selection
            OutlinedTextField(
                value = alarmTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                onValueChange = {},
                readOnly = true,
                label = { Text("Time") },
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        // https://stackoverflow.com/questions/67902919/jetpack-compose-textfield-clickable-does-not-work/79721039#79721039
                        awaitEachGesture {
                            // Modifier.clickable doesn't work for text fields, so we use Modifier.pointerInput
                            // to look for the down->up event in the Initial pass before the text field consumes them
                            // in the Main pass.
                            awaitFirstDown(pass = PointerEventPass.Initial)
                            val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                            if (upEvent != null) {
                                showTimePicker = true
                            }
                        }
                    }
            )
        }

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
                onClick = onCancel,
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
    val data = Todo(taskName, frequency, time)

    EverydayToDoListTheme {
        Surface(
            Modifier.background(color = MaterialTheme.colorScheme.background)
        ) {
            EditTaskComposable(
                data = data,
                {},
                {},
                Modifier
            )
        }
    }
}