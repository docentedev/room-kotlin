package com.rojas.room.components

import androidx.compose.runtime.Composable

@Composable
fun ConfirmModal(
    title: String,
    message: String,
    confirmText: String = "Eliminar",
    dismissText: String = "Cancelar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text(title) },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = {
            AppButton(text = confirmText, variant = ButtonVariant.DANGER, onClick = onConfirm)
        },
        dismissButton = {
            AppButton(text = dismissText, variant = ButtonVariant.SECONDARY, onClick = onDismiss)
        }
    )
}
