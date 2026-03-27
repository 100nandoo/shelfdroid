package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipDropdownMenu(
  modifier: Modifier = Modifier,
  options: List<String> = emptyList(),
  label: String? = null,
  labelOnTop: Boolean = false,
  initialValue: String,
  isError: Boolean = false,
  onClick: (String) -> Unit = {},
) {
  var expanded by remember { mutableStateOf(false) }
  var selected by remember(initialValue) { mutableStateOf(initialValue) }

  ExposedDropdownMenuBox(
    modifier = modifier,
    expanded = expanded,
    onExpandedChange = { expanded = it },
  ) {
    val labelComposable: @Composable (() -> Unit)? = label?.let {
      {
        Text(
          text = it,
          style = MaterialTheme.typography.labelSmall,
          color = OutlinedTextFieldDefaults.colors().unfocusedLabelColor,
          modifier =
            if (labelOnTop) {
              Modifier
            } else {
              Modifier.padding(end = 8.dp)
            },
        )
      }
    }

    val chipComposable: @Composable () -> Unit = {
      val errorBorder =
        if (isError) {
          FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = MaterialTheme.colorScheme.error,
            selectedBorderColor = MaterialTheme.colorScheme.error,
          )
        } else {
          FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
        }
      FilterChip(
        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        selected = expanded,
        onClick = {},
        label = {
          Text(
            text = selected.ifEmpty { label ?: "" },
            color =
              if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
          )
        },
        border = errorBorder,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      )
    }

    if (labelOnTop) {
      Column {
        labelComposable?.invoke()
        chipComposable()
      }
    } else {
      Row(verticalAlignment = Alignment.CenterVertically) {
        labelComposable?.invoke()
        chipComposable()
      }
    }

    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        val isSelected = option == selected

        DropdownMenuItem(
          text = { Text(option) },
          onClick = {
            selected = option
            expanded = false
            onClick(option)
          },
          trailingIcon = {
            if (isSelected) {
              Icon(
                painter = painterResource(id = R.drawable.check),
                contentDescription = stringResource(R.string.selected),
              )
            }
          },
        )
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun ChipDropdownMenuPreview() {
  PreviewWrapper {
    ChipDropdownMenu(
      options = listOf("Option 1", "Option 2", "Option 3"),
      label = "Label",
      initialValue = "Option 1",
    )
  }
}

@ShelfDroidPreview
@Composable
fun ChipDropdownMenuLabelTopPreview() {
  PreviewWrapper {
    ChipDropdownMenu(
      options = listOf("Option 1", "Option 2", "Option 3"),
      label = "Label",
      labelOnTop = true,
      initialValue = "Option 1",
    )
  }
}
