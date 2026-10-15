package com.rojas.room.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
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
import com.rojas.room.ui.TaskViewModel

@Composable
fun UpdateView(id: Long, vm: TaskViewModel, onDone: () -> Unit) {
    LaunchedEffect(id) { vm.loadById(id) }
    val task by vm.selected.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }
    var initializedFor by remember { mutableStateOf<Long?>(null) }

    if (task != null && initializedFor != task?.id) {
        title = task?.title.orEmpty()
        done = task?.done == true
        initializedFor = task?.id
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Editar tarea")
        if (task == null) {
            Text("Cargando...")
        } else {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = done, onCheckedChange = { done = it })
                Text("Hecha")
            }
            AppButton(
                text = "Actualizar",
                variant = ButtonVariant.PRIMARY,
                enabled = title.isNotBlank(),
                onClick = { vm.updateById(id, title, done, onDone) }
            )
        }
    }
}
