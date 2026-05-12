package com.milton.forgetmenot.ui.screens

import android.content.res.Configuration
import android.icu.text.SimpleDateFormat
import java.util.TimeZone
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.milton.forgetmenot.data.DayOfWeekUtil.Factory.timeToCalendarDayOfWeek
import com.milton.forgetmenot.data.todos.DailyTodo
import com.milton.forgetmenot.data.todos.ITodo
import com.milton.forgetmenot.data.todos.WeeklyTodo
import com.milton.forgetmenot.ui.components.DayOfWeekSelector
import com.milton.forgetmenot.ui.theme.EverydayToDoListTheme
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

private enum class EndType {
    NEVER,
    NUM_OCCURRENCES,
    DATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskComposable(
    data: ITodo?,
    onSave: (ITodo) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editingNewTodo = (data == null)
    var taskName by remember { mutableStateOf(data?.title ?: ITodo.defaultName) }
    var frequencyString by remember { mutableStateOf((data?.frequency ?: ITodo.defaultFrequency).toString()) }
    var alarmTime by remember { mutableStateOf(data?.alarmTime ?: ITodo.defaultAlarmTime) }
    val timePickerState = rememberTimePickerState(
        initialHour = alarmTime.hour,
        initialMinute = alarmTime.minute
    )


    var showTimePicker by remember { mutableStateOf(false) }
    var repeating by remember { mutableStateOf((data?.maxOccurrences ?: 2) != 1) }
    
    val frequencyOptions = listOf("Days", "Weeks"/*, "Months",*/)
    var frequencyLengthSelection by remember { mutableStateOf(
        when(data) {
            null, is DailyTodo -> frequencyOptions[0]
            is WeeklyTodo -> frequencyOptions[1]
            else -> frequencyOptions[0]
        }) }
    var showFrequencyMenu by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate.timeInMillis
    )
    val startDateScrollState = remember { ScrollState(0) }

    val daysOfWeek = remember {
        when(data) {
            is WeeklyTodo -> data.daysOfWeek.toMutableStateList()
            else -> mutableStateListOf(DayOfWeek.MONDAY)
        }
    }
    val onDaySelected = { dayOfWeek: DayOfWeek ->
        if(!daysOfWeek.remove(dayOfWeek))
            daysOfWeek.add(dayOfWeek)
    }
    val daysOfWeekSort: (DayOfWeek) -> Int = { dayOfWeek ->
        timeToCalendarDayOfWeek(dayOfWeek)
    }

    var endSelector by remember{ mutableStateOf(
        if(data?.endDate != null) EndType.DATE
        else if(data?.maxOccurrences != null) EndType.NUM_OCCURRENCES
        else EndType.NEVER
    )}

    var numOccurrencesString by remember { mutableStateOf((data?.maxOccurrences ?: 1).toString()) }

    var endDate by remember { mutableStateOf(data?.endDate ?: Calendar.getInstance()) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate.timeInMillis
    )
    val endDateScrollState = remember { ScrollState(0) }

    val scrollState = remember { ScrollState(0) }

    val textColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState),
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

        Row (
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = repeating,
                onCheckedChange = { repeating = it }
            )
            Text(
                "Repeating"
            )
        }
        // Frequency Selection
        if(repeating) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    "Every",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorProducer { textColor },
                    autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = MaterialTheme.typography.titleMedium.fontSize),
                    modifier = Modifier
                        .padding(top = 5.dp, end = 8.dp)
                        .weight(.2F)
                )
                OutlinedTextField(
                    value = frequencyString,
                    onValueChange = { frequencyString = it },
                    label = { Text("Frequency") },
                    textStyle = MaterialTheme.typography.titleMedium,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(.3F)
                )
                ExposedDropdownMenuBox(
                    expanded = showFrequencyMenu,
                    onExpandedChange = { showFrequencyMenu = !showFrequencyMenu },
                    modifier = Modifier.weight(.50F)
                ) {
                    OutlinedTextField(
                        value = frequencyLengthSelection,
                        onValueChange = {/*ExposedDropdownMenu is handling this*/ },
                        readOnly = true,
                        label = { Text("Frequency Length") },
                        textStyle = MaterialTheme.typography.titleMedium,
                        suffix = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop down")
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .padding(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showFrequencyMenu,
                        onDismissRequest = { showFrequencyMenu = false }
                    ) {
                        frequencyOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    frequencyLengthSelection = option
                                    showFrequencyMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (data == null) {
            DatePickerTextFieldDisplay(
                label = "Start Date",
                displayDate = startDate,
                onTextFieldClicked = { showStartDatePicker = true },
                showDatePicker = showStartDatePicker,
                datePickerState = startDatePickerState,
                datePickerHorScrollState = startDateScrollState,
                onDatePickerConfirmed = {
                    startDate = Calendar.getInstance().apply {
                        timeInMillis = fixDatePickerStateMillis(startDatePickerState.selectedDateMillis)
                    }
                    showStartDatePicker = false
                },
                onDatePickerDismissed = { showStartDatePicker = false },
                modifier = Modifier
            )
        }

        if(repeating) {
            if(frequencyLengthSelection == frequencyOptions[1]) {
                DayOfWeekSelector(
                    selectedDays = daysOfWeek,
                    onDaySelected = onDaySelected
                )
            }
            val fieldsModifier = Modifier.padding(start = 16.dp)
            Column {
                Text(
                    "Ends After"
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = endSelector == EndType.NEVER,
                            { endSelector = EndType.NEVER }
                        )
                        Text("Never")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = endSelector == EndType.NUM_OCCURRENCES,
                            { endSelector = EndType.NUM_OCCURRENCES }
                        )
                        Text("Number of Occurrences")
                    }
                    if(endSelector == EndType.NUM_OCCURRENCES) {
                        OutlinedTextField(
                            value = numOccurrencesString,
                            onValueChange = { numOccurrencesString = it },
                            label = { Text("Occurrences") },
                            textStyle = MaterialTheme.typography.titleMedium,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            modifier = fieldsModifier
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = endSelector == EndType.DATE,
                            { endSelector = EndType.DATE }
                        )
                        Text("The Specified Date")
                    }
                    if (endSelector == EndType.DATE) {
                        DatePickerTextFieldDisplay(
                            label = "End Date",
                            displayDate = endDate,
                            onTextFieldClicked = { showEndDatePicker = true },
                            showDatePicker = showEndDatePicker,
                            datePickerState = endDatePickerState,
                            datePickerHorScrollState = endDateScrollState,
                            onDatePickerConfirmed = {
                                endDate = Calendar.getInstance().apply {
                                    timeInMillis = fixDatePickerStateMillis(endDatePickerState.selectedDateMillis)
                                }
                                showEndDatePicker = false
                            },
                            onDatePickerDismissed = { showEndDatePicker = false },
                            modifier = fieldsModifier
                        )
                    }
                }
            }
        }

        Box {
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
            EnlargedPickerDialog(
                title = "Select Time",
                horizontalScrollState = startDateScrollState,
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
                TimePicker(state = timePickerState)
            }
        }

        val buttonColors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
            disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor
        )
        // Cancel and Save Buttons
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onCancel,
                colors = buttonColors,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "Cancel"
                )
            }
            Button(
                onClick = {
                    val maxOccurrences = when(endSelector) {
                        EndType.NUM_OCCURRENCES -> numOccurrencesString.toInt()
                        else -> null
                    }
                    val endDate = when(endSelector) {
                        EndType.DATE -> endDate
                        else -> null
                    }
                    var newTodo: ITodo? = null
                    when(editingNewTodo){
                        true -> {
                            when(repeating) {
                                true ->
                                    when (frequencyLengthSelection) {
                                        "Days" -> newTodo = DailyTodo(
                                            title = taskName,
                                            frequency = frequencyString.toInt(),
                                            alarmTime = alarmTime,
                                            maxOccurrences = maxOccurrences,
                                            endDate = endDate,
                                            nextOccurrence = Calendar.getInstance()
                                                .apply { time = startDate.time }
                                        )
                                        "Weeks" -> newTodo = WeeklyTodo(
                                            title = taskName,
                                            frequency = frequencyString.toInt(),
                                            daysOfWeek = daysOfWeek.sortedBy(daysOfWeekSort),
                                            alarmTime = alarmTime,
                                            maxOccurrences = maxOccurrences,
                                            endDate = endDate,
                                            nextOccurrence = Calendar.getInstance()
                                                .apply { time = startDate.time }
                                        )

                                        else -> println("User selected a frequency length option that's not available??")
                                    }
                                false ->
                                    newTodo = DailyTodo(
                                        title = taskName,
                                        frequency = frequencyString.toInt(),
                                        alarmTime = alarmTime,
                                        maxOccurrences = 1,
                                        endDate = null,
                                        nextOccurrence = Calendar.getInstance()
                                            .apply { time = startDate.time }
                                    )
                            }
                        }
                        false -> {
                            when(repeating) {
                                true ->
                                    when (frequencyLengthSelection) {
                                        "Days" -> newTodo = DailyTodo(
                                            title = taskName,
                                            frequency = frequencyString.toInt(),
                                            alarmTime = alarmTime,
                                            uniqueId = data.uniqueId,
                                            maxOccurrences = maxOccurrences,
                                            endDate = endDate,
                                            nextOccurrence = Calendar.getInstance()
                                                .apply { time = data.getNextOccurrence() },
                                            lastOccurrence = Calendar.getInstance()
                                                .apply { time = data.getLastOccurrence() },
                                            timesSnoozedSinceLastCompletion = data.getTimesSnoozedSinceLastCompletion(),
                                            numOccurrences = data.getNumOccurrences()
                                        )
                                        "Weeks" -> newTodo = WeeklyTodo(
                                            title = taskName,
                                            frequency = frequencyString.toInt(),
                                            daysOfWeek = daysOfWeek.sortedBy(daysOfWeekSort),
                                            alarmTime = alarmTime,
                                            uniqueId = data.uniqueId,
                                            maxOccurrences = maxOccurrences,
                                            endDate = endDate,
                                            nextOccurrence = Calendar.getInstance()
                                                .apply { time = data.getNextOccurrence() },
                                            lastOccurrence = Calendar.getInstance()
                                                .apply { time = data.getLastOccurrence() },
                                            timesSnoozedSinceLastCompletion = data.getTimesSnoozedSinceLastCompletion(),
                                            numOccurrences = data.getNumOccurrences()
                                        )

                                        else -> println("User selected a frequency length option that's not available??")
                                    }
                                false -> newTodo = DailyTodo(
                                        title = taskName,
                                        frequency = frequencyString.toInt(),
                                        alarmTime = alarmTime,
                                        uniqueId = data.uniqueId,
                                        maxOccurrences = 1,
                                        endDate = null,
                                        nextOccurrence = Calendar.getInstance()
                                            .apply { time = data.getNextOccurrence() },
                                        lastOccurrence = Calendar.getInstance()
                                            .apply { time = data.getLastOccurrence() },
                                        timesSnoozedSinceLastCompletion = data.getTimesSnoozedSinceLastCompletion(),
                                        numOccurrences = data.getNumOccurrences()
                                    )
                            }
                        }
                    }

                    when(newTodo) {
                        null -> onCancel() //TODO Tell the user something went wrong
                        else -> onSave(newTodo)
                    }
                },
                enabled = isFrequencyIrrelevantOrValid(repeating, frequencyString) &&
                        isNumOccurrencesIrrelevantOrValid(repeating, endSelector, numOccurrencesString) &&
                        isDaysOfWeekIrrelevantOrValid(repeating, frequencyString, daysOfWeek),
                colors = buttonColors,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "Save"
                )
            }
        }
    }
}

@Composable
fun EnlargedPickerDialog(
    title: String,
    horizontalScrollState: ScrollState,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit),
    minimumContentWidth: Dp? = null,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(title) },
            text = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    content()
                }
            },
            confirmButton = confirmButton,
            dismissButton = dismissButton,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            ),
            modifier = if(maxWidth < (minimumContentWidth ?: 0.dp)) {
                Modifier.horizontalScroll(horizontalScrollState)
            } else {
                Modifier
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerTextFieldDisplay(
    label: String,
    displayDate: Calendar,
    onTextFieldClicked: () -> Unit,
    showDatePicker: Boolean,
    datePickerState: DatePickerState,
    datePickerHorScrollState: ScrollState,
    onDatePickerConfirmed: () -> Unit,
    onDatePickerDismissed: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier
    ) {
        // Alarm Time Selection
        OutlinedTextField(
            value = SimpleDateFormat.getDateInstance().format(displayDate.time),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
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
                        val upEvent =
                            waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        if (upEvent != null) {
                            onTextFieldClicked()
                        }
                    }
                }
        )
    }

    if (showDatePicker) { //TODO Make this not be cropped weirdly
        val datePickerMinWidth = 360.dp
        EnlargedPickerDialog(
            title = "Select Date",
            horizontalScrollState = datePickerHorScrollState,
            onDismissRequest = onDatePickerDismissed,
            confirmButton = {
                TextButton(
                    onClick = onDatePickerConfirmed
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDatePickerDismissed
                ) {
                    Text("Cancel")
                }
            },
            minimumContentWidth = datePickerMinWidth
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null
            )
        }
    }
}

private fun fixDatePickerStateMillis(millis: Long?): Long {
    val nonadjustedMillis = millis ?: Calendar.getInstance(TimeZone.getTimeZone("GMT+0")).timeInMillis
    val adjustedMillis = nonadjustedMillis - TimeZone.getDefault().getOffset(Calendar.getInstance().timeInMillis)
    return adjustedMillis
}

private fun isFrequencyIrrelevantOrValid(isRepeating: Boolean, frequencyString: String): Boolean {
    return !isRepeating || isFrequencyValid(frequencyString)
}

private fun isFrequencyValid(frequencyString: String): Boolean {
    val frequency = frequencyString.toIntOrNull() ?: -1
    return frequency >= 1
}

private fun isDaysOfWeekIrrelevantOrValid(isRepeating: Boolean, frequencyType: String, daysOfWeek: List<DayOfWeek>): Boolean {
    return !isRepeating || !(frequencyType == "Weeks") || isDaysOfWeekValid(daysOfWeek)
}

private fun isDaysOfWeekValid(daysOfWeek: List<DayOfWeek>): Boolean {
    return !daysOfWeek.isEmpty()
}

private fun isNumOccurrencesIrrelevantOrValid(isRepeating: Boolean, endType: EndType, numOccurrencesString: String): Boolean {
    return !isRepeating || endType != EndType.NUM_OCCURRENCES || isNumOccurrencesValid(numOccurrencesString)
}

private fun isNumOccurrencesValid(numOccurrencesString: String): Boolean {
    val numOccurrences = numOccurrencesString.toIntOrNull() ?: -1
    return numOccurrences >= 1
}

@Preview(showBackground = true)
@Preview(widthDp = 350)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditTaskComposablePreview() {
    var taskName by remember { mutableStateOf("Do Laundry") }
    var frequency by remember { mutableIntStateOf(1) }
    var time by remember { mutableStateOf(LocalTime.of(18, 0)) }
    val data = DailyTodo(taskName, frequency, time)

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

@Preview(showBackground = true, widthDp = 350)
@Composable
fun EditTaskComposableWeeklyPreview() {
    val taskName by remember { mutableStateOf("Do Laundry") }
    val frequency by remember { mutableIntStateOf(1) }
    val daysOfWeek = remember { mutableStateListOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY) }
    val time by remember { mutableStateOf(LocalTime.of(18, 0)) }
    val data = WeeklyTodo(taskName, frequency, daysOfWeek, time)

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

@Preview(showBackground = true)
@Composable
fun EditTaskComposableNewPreview() {
    EverydayToDoListTheme {
        Surface(
            Modifier.background(color = MaterialTheme.colorScheme.background)
        ) {
            EditTaskComposable(
                data = null,
                {},
                {},
                Modifier
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditTaskComposableNonRepeatingPreview() {
    var taskName by remember { mutableStateOf("Do Laundry") }
    var frequency by remember { mutableIntStateOf(1) }
    var time by remember { mutableStateOf(LocalTime.of(18, 0)) }
    val data = DailyTodo(taskName, frequency, time, maxOccurrences = 1)

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

@Preview(showBackground = true)
@Composable
fun EditTaskComposableInvalidPreview() {
    var taskName by remember { mutableStateOf("Do Laundry") }
    var frequency by remember { mutableIntStateOf(0) }
    var time by remember { mutableStateOf(LocalTime.of(18, 0)) }
    val data = DailyTodo(taskName, frequency, time, endDate = Calendar.getInstance().apply { isLenient = true; set(Calendar.DAY_OF_YEAR, Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + 5) })

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