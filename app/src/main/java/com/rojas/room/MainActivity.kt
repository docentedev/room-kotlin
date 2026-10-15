package com.rojas.room

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.rojas.room.navigation.AppNavHost
import com.rojas.room.ui.TaskViewModel
import com.rojas.room.ui.theme.RoomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoomTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val context = LocalContext.current.applicationContext
                    val app = remember(context) { context as TaskApplication }
                    val vm: TaskViewModel = viewModel(
                        factory = TaskViewModel.Factory(app.repository)
                    )
                    val nav = rememberNavController()
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavHost(nav = nav, vm = vm)
                    }
                }
            }
        }
    }
}
