package com.rojas.room.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rojas.room.components.AppButton
import com.rojas.room.components.ButtonVariant
import com.rojas.room.components.ConfirmModal
import com.rojas.room.ui.TaskViewModel

@Composable
fun DetailView(
    id: Long,
    vm: TaskViewModel,
    onEdit: () -> Unit,
    onDeleted: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(id) { vm.loadById(id) }
    val task by vm.selected.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Detalle de tarea", style = MaterialTheme.typography.titleLarge)
        val current = task
        if (current == null) {
            Text("Cargando...")
        } else {
            Text(current.title, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = current.done,
                    onCheckedChange = { vm.toggleDone(current) }
                )
                Text(if (current.done) "Hecha" else "Pendiente")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(text = "Editar", variant = ButtonVariant.SECONDARY, onClick = onEdit)
                AppButton(
                    text = "Eliminar",
                    variant = ButtonVariant.DANGER,
                    onClick = { showConfirm = true }
                )
            }
        }
    }

    if (showConfirm) {
        ConfirmModal(
            title = "Eliminar tarea",
            message = "¿Seguro que quieres eliminar esta tarea? Esta acción no se puede deshacer.",
            onConfirm = { vm.deleteById(id, onDeleted) },
            onDismiss = { showConfirm = false }
        )
    }
}
