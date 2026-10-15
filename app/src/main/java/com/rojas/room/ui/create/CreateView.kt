package com.rojas.room.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rojas.room.components.AppButton
import com.rojas.room.components.ButtonVariant
import com.rojas.room.ui.TaskViewModel

@Composable
fun CreateView(vm: TaskViewModel, onDone: () -> Unit) {
    var title by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Nueva tarea")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        AppButton(
            text = "Guardar",
            variant = ButtonVariant.PRIMARY,
            enabled = title.isNotBlank(),
            onClick = { vm.save(title, onDone) }
        )
    }
}
