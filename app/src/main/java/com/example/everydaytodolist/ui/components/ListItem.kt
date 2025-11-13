package com.example.everydaytodolist.ui.components

import android.icu.text.DateFormat
import android.icu.util.Calendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydaytodolist.data.Todo
import com.example.everydaytodolist.R
import com.example.everydaytodolist.ui.theme.EverydayToDoListTheme
import com.example.everydaytodolist.ui.theme.ExtendedTheme
import java.time.LocalTime

@Composable
fun ListItem(
    data: Todo,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onCompletedClicked: () -> Unit,
    onSnoozeClicked: (Int?) -> Unit,
    modifier: Modifier = Modifier)
{
    var expanded by remember { mutableStateOf(false) }

    val buttonTextStyle = MaterialTheme.typography.labelLarge
    val titleTextStyle = MaterialTheme.typography.titleLarge
    val bodyTextStyle = MaterialTheme.typography.bodyMedium

    var backgroundColor = MaterialTheme.colorScheme.primary
    var onBackgroundColor = MaterialTheme.colorScheme.onPrimary
    var buttonColor = MaterialTheme.colorScheme.primaryContainer
    var onButtonColor = MaterialTheme.colorScheme.onPrimaryContainer
    when(data.timesSnoozedSinceLastCompletion) {
        in Int.MIN_VALUE..0 -> {}
        in 1..2 -> {
            backgroundColor = ExtendedTheme.colorScheme.snoozed.color
            onBackgroundColor = ExtendedTheme.colorScheme.snoozed.onColor
            buttonColor = ExtendedTheme.colorScheme.snoozed.colorContainer
            onButtonColor = ExtendedTheme.colorScheme.snoozed.onColorContainer
        }
        else -> {
            backgroundColor = ExtendedTheme.colorScheme.oversnoozed.color
            onBackgroundColor = ExtendedTheme.colorScheme.oversnoozed.onColor
            buttonColor = ExtendedTheme.colorScheme.oversnoozed.colorContainer
            onButtonColor = ExtendedTheme.colorScheme.oversnoozed.onColorContainer
        }
    }


    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = buttonColor,
        contentColor = ButtonDefaults.buttonColors().contentColor,
        disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
        disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor
    )

    Box(
        modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.large
            )
    ) {
        Column() {
            Row() {
                Column(
                    Modifier
                        .weight(.75f)
                        .padding(8.dp)
                        .clickable() {
                            expanded = !expanded
                        }
                ) {
                    val titleString = data.title
                    val frequencyString = when (data.frequencyInDays) {
                        1 -> stringResource(R.string.frequency_one_day)
                        else -> stringResource(
                            R.string.frequency_multiple_days,
                            data.frequencyInDays
                        )
                    }
                    val nextOccurrenceString = DateFormat.getPatternInstance(DateFormat.MONTH_DAY)
                        .format(data.getNextOccurrenceTime())

                    Text(
                        titleString,
                        color = onBackgroundColor,
                        style = titleTextStyle,
                        maxLines = 2,
                        modifier = Modifier
                    )
                    Text(
                        text = frequencyString,
                        color = onBackgroundColor,
                        style = bodyTextStyle
                    )

                    Text(
                        "Next occurrence on $nextOccurrenceString",
                        color = onBackgroundColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Column(
                    Modifier.weight(.25f)
                ) {
                    Button(
                        onEditClicked,
                        colors = buttonColors,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            "Edit",
                            style = buttonTextStyle,
                            color = onButtonColor,
                            maxLines = 1
                        )
                    }
                    Button(
                        onDeleteClicked,
                        colors = buttonColors,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            "Delete", //TODO Fix issue with this text trying to be on two lines
                            style = buttonTextStyle,
                            color = onButtonColor,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            AnimatedVisibility(expanded) {
                Row() {
                    Button(
                        onClick = { onSnoozeClicked(1) }, //TODO make this thing a dialog and determine length of snooze from user
                        colors = buttonColors
                    ) {
                        Text(
                            "Snooze",
                            style = buttonTextStyle,
                            color = onButtonColor,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = onCompletedClicked,
                        colors = buttonColors
                    ) {
                        Text(
                            "Mark Completed",
                            style = buttonTextStyle,
                            color = onButtonColor,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Preview()
@Composable
fun ListItemPreview() {
    val sampleData = Todo("Clean Dishes", 1, LocalTime.of(9, 0))

    EverydayToDoListTheme {
        Surface() {
            ListItem(sampleData, {}, {}, {}, {}, Modifier)
        }
    }
}

@Preview()
@Composable
fun ListItemSnoozedPreview() {
    val sampleData = Todo("Clean Dishes", 1, LocalTime.of(9, 0))
    sampleData.snooze()

    EverydayToDoListTheme {
        Surface() {
            ListItem(sampleData, {}, {}, {}, {}, Modifier)
        }
    }
}

@Preview()
@Composable
fun ListItemOversnoozedPreview() {
    val sampleData = Todo("Clean Dishes", 1, LocalTime.of(9, 0))
    sampleData.snooze()
    sampleData.snooze()
    sampleData.snooze()
    
    EverydayToDoListTheme {
        Surface() {
            ListItem(sampleData, {}, {}, {}, {}, Modifier)
        }
    }
}