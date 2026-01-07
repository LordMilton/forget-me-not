package com.example.everydaytodolist.ui.components

import android.icu.text.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydaytodolist.R
import com.example.everydaytodolist.data.DailyTodo
import com.example.everydaytodolist.data.ITodo
import com.example.everydaytodolist.ui.theme.EverydayToDoListTheme
import com.example.everydaytodolist.ui.theme.ExtendedTheme
import java.time.LocalTime

@Composable
fun ListItem(
    data: ITodo,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onCompletedClicked: () -> Unit,
    onSnoozeClicked: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    startExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(startExpanded) }

    val buttonTextStyle = MaterialTheme.typography.labelLarge
    val bodyTextStyle = MaterialTheme.typography.bodyMedium
    var titleTextStyle = MaterialTheme.typography.titleLarge
    if(data.completedToday()) {
        titleTextStyle = titleTextStyle.copy(textDecoration = TextDecoration.LineThrough)
    }

    var backgroundColor = MaterialTheme.colorScheme.primary
    var onBackgroundColor = MaterialTheme.colorScheme.onPrimary
    var buttonColor = MaterialTheme.colorScheme.primaryContainer
    var onButtonColor = MaterialTheme.colorScheme.onPrimaryContainer
    when(data.getTimesSnoozedSinceLastCompletion()) {
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
            .clickable() {
                expanded = !expanded
            }
    ) {
        Column {
            Row {
                Column(
                    Modifier
                        .weight(.72f)
                        .padding(8.dp)
                ) {
                    val titleString = data.title
                    val frequencyString = when (data.frequency) {
                        1 -> stringResource(R.string.frequency_one_day)
                        else -> stringResource(
                            R.string.frequency_multiple_days,
                            data.frequency
                        )
                    }
                    val nextOccurrenceString = DateFormat.getPatternInstance(DateFormat.MONTH_DAY)
                        .format(data.getNextOccurrence())

                    Text(
                        titleString,
                        color = onBackgroundColor,
                        style = titleTextStyle,
                        maxLines = when(expanded) {
                            true -> 10
                            false -> 2
                        },
                        overflow = TextOverflow.Ellipsis,
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
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(.28f)
                ) {
                    val buttonModifier = Modifier
                        .padding(end = 4.dp)
                        .defaultMinSize(minWidth = 105.dp) // Force Edit and Delete buttons to be the same size
                    Button(
                        onEditClicked,
                        colors = buttonColors,
                        modifier = buttonModifier
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
                        modifier = buttonModifier
                    ) {
                        Text(
                            "Delete",
                            style = buttonTextStyle,
                            color = onButtonColor,
                            maxLines = 1
                        )
                    }
                }
            }

            AnimatedVisibility(!expanded) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.drop_down_indicator),
                        contentDescription = stringResource(R.string.expand_hint),
                        contentScale = ContentScale.FillHeight,
                        colorFilter = ColorFilter.tint(onBackgroundColor)
                    )
                }
            }
            AnimatedVisibility(expanded) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onSnoozeClicked(1) }, //TODO make this thing a dialog and determine length of snooze from user
                        colors = buttonColors,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            "Snooze",
                            style = buttonTextStyle,
                            color = onButtonColor,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = {
                            expanded = false
                            onCompletedClicked()
                        },
                        colors = buttonColors,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            "Mark Completed",
                            style = buttonTextStyle,
                            color = onButtonColor,
                            maxLines = 1
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
    val sampleData = DailyTodo("Clean Dishes", 1, LocalTime.of(9, 0))

    EverydayToDoListTheme {
        Surface() {
            ListItem(sampleData, {}, {}, {}, {}, startExpanded = false, modifier = Modifier)
        }
    }
}

@Preview()
@Composable
fun ListItemCompletedPreview() {
    val sampleData = DailyTodo("Clean Dishes Very Thoroughly", 1, LocalTime.of(9, 0))
    sampleData.markCompleted()

    EverydayToDoListTheme {
        Surface() {
            ListItem(sampleData, {}, {}, {}, {}, startExpanded = false, modifier = Modifier)
        }
    }
}

@Preview()
@Composable
fun ListItemSnoozedPreview() {
    val sampleData = DailyTodo("Clean Dishes So Thoroughly That Dirt Will Never Even Consider Touching Them Again", 1, LocalTime.of(9, 0))
    sampleData.snooze()

    EverydayToDoListTheme {
        Surface() {
            ListItem(sampleData, {}, {}, {}, {}, startExpanded = true, modifier = Modifier)
        }
    }
}

@Preview()
@Composable
fun ListItemOversnoozedPreview() {
    val sampleData = DailyTodo("Clean Dishes", 1, LocalTime.of(9, 0))
    sampleData.snooze()
    sampleData.snooze()
    sampleData.snooze()
    
    EverydayToDoListTheme {
        Surface() {
            ListItem(sampleData, {}, {}, {}, {}, startExpanded = false, modifier = Modifier)
        }
    }
}