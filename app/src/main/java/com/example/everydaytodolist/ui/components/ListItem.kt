package com.example.everydaytodolist.ui.components

import android.icu.text.DateFormat
import android.icu.util.Calendar
import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydaytodolist.data.Todo
import com.example.everydaytodolist.R
import com.example.everydaytodolist.ui.theme.EverydayToDoListTheme
import java.time.LocalTime

@Composable
fun ListItem(
    data: Todo,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier)
{
    val buttonTextStyle = MaterialTheme.typography.labelLarge
    val titleTextStyle = MaterialTheme.typography.titleLarge
    val bodyTextStyle = MaterialTheme.typography.bodyMedium

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = ButtonDefaults.buttonColors().contentColor,
        disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
        disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor
    )

    Box(
        modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large
            )
    ) {
        Row() {
            Column(
                Modifier
                    .weight(.75f)
                    .padding(8.dp)
            ) {
                val titleString = data.title
                val frequencyString = when (data.frequencyInDays)
                    {
                        1 -> stringResource(R.string.frequency_one_day)
                        else -> stringResource(
                            R.string.frequency_multiple_days,
                            data.frequencyInDays
                        )
                    }
                val nextOccurrenceString = DateFormat.getPatternInstance(DateFormat.MONTH_DAY).format(data.getNextOccurrenceTime())
                
                Text(
                    titleString,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = titleTextStyle,
                    maxLines = 2,
                    modifier = Modifier
                )
                Text(
                    text = frequencyString,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = bodyTextStyle
                )

                Text(
                    "Next occurrence on $nextOccurrenceString",
                    color = MaterialTheme.colorScheme.onPrimary,
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ListItemPreview() {
    val sampleData = Todo("Clean Dishes", 1, LocalTime.of(9, 0))

    EverydayToDoListTheme {
        Surface() {
            ListItem(sampleData, {}, {}, Modifier)
        }
    }
}