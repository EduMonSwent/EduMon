package com.android.sample.ui.todo

import android.app.DatePickerDialog
import android.view.ContextThemeWrapper
import android.widget.DatePicker
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.sample.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Read-only date text field + "Change" button that opens a native DatePickerDialog.
 * - Displays the selected date
 * - Lets user pick a new date via dialog
 */
@Composable
fun DueDateField(
    date: LocalDate, // currently selected date
    onDateChange: (LocalDate) -> Unit, // callback when user picks a new date
    modifier: Modifier = Modifier,
    label: String = "Due date*",
    changeButtonText: String = "Change",
    // Reusable text field color scheme, tuned for dark UI
    colors: TextFieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = TodoColors.OnCard,
            unfocusedTextColor = TodoColors.OnCard,
            disabledTextColor = TodoColors.OnCard.copy(alpha = 0.7f),
            focusedLabelColor = TodoColors.OnCard,
            unfocusedLabelColor = TodoColors.OnCard.copy(alpha = 0.8f),
            focusedBorderColor = TodoColors.OnCard.copy(alpha = 0.6f),
            unfocusedBorderColor = TodoColors.OnCard.copy(alpha = 0.35f),
            cursorColor = TodoColors.Accent,
            focusedPlaceholderColor = TodoColors.OnCard.copy(alpha = 0.6f),
            unfocusedPlaceholderColor = TodoColors.OnCard.copy(alpha = 0.6f))
) {
  val context = LocalContext.current
  val fmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

  Row(modifier = modifier) {
    // Read-only field showing the formatted date
    OutlinedTextField(
        value = date.format(fmt),
        onValueChange = {}, // read-only -> ignore input changes
        label = { Text(label) },
        readOnly = true, // keep enabled for contrast, just non-editable
        colors = colors, // apply high-contrast colors
        modifier = Modifier.weight(1f))

    Spacer(Modifier.width(8.dp))

    // Button that opens a native DatePicker dialog with a pink header theme overlay
    Button(
        onClick = {
          // Initialize the dialog to the current date value
          val cal =
              Calendar.getInstance().apply {
                set(Calendar.YEAR, date.year)
                set(Calendar.MONTH, date.monthValue - 1) // Calendar months are 0-based
                set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
              }

          DatePickerDialog(
                  ContextThemeWrapper(context, R.style.CustomDatePickerDialog), // pink header
                  { _: DatePicker, y: Int, m: Int, d: Int ->
                    // Note: dialog month is 0-based -> add 1 for LocalDate
                    onDateChange(LocalDate.of(y, m + 1, d))
                  },
                  cal.get(Calendar.YEAR),
                  cal.get(Calendar.MONTH),
                  cal.get(Calendar.DAY_OF_MONTH))
              .show()
        },
        modifier = Modifier.testTag(TestTags.ChangeDateBtn)) {
          Text(changeButtonText)
        }
  }
}
