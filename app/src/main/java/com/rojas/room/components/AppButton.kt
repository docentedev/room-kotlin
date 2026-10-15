package com.rojas.room.components

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class ButtonVariant { PRIMARY, SECONDARY, DANGER }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true
) {
    when (variant) {
        ButtonVariant.PRIMARY -> Button(
            onClick = onClick, modifier = modifier, enabled = enabled
        ) { Text(text) }
        ButtonVariant.SECONDARY -> OutlinedButton(
            onClick = onClick, modifier = modifier, enabled = enabled
        ) { Text(text) }
        ButtonVariant.DANGER -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) { Text(text) }
    }
}
