package com.bennyjon.auiandroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bennyjon.auiandroid.livechat.DemoAuiTheme

/**
 * Dropdown button for selecting the active [DemoAuiTheme].
 *
 * Displays the current theme name. Tapping opens a menu with all [DemoAuiTheme] entries.
 */
@Composable
fun ThemeDropdown(
    currentTheme: DemoAuiTheme,
    onThemeSelected: (DemoAuiTheme) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(currentTheme.displayName)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DemoAuiTheme.entries.forEach { theme ->
                DropdownMenuItem(
                    text = { Text(theme.displayName) },
                    onClick = {
                        expanded = false
                        onThemeSelected(theme)
                    },
                )
            }
        }
    }
}
