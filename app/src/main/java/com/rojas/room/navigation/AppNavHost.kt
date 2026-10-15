package com.rojas.room.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rojas.room.ui.TaskViewModel
import com.rojas.room.ui.create.CreateView
import com.rojas.room.ui.detail.DetailView
import com.rojas.room.ui.list.ListView
import com.rojas.room.ui.update.UpdateView

@Composable
fun AppNavHost(nav: NavHostController, vm: TaskViewModel) {
    NavHost(navController = nav, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            ListView(
                vm = vm,
                onOpen = { nav.navigate(Routes.detail(it)) },
                onCreate = { nav.navigate(Routes.CREATE) }
            )
        }
        composable(Routes.CREATE) {
            CreateView(vm = vm, onDone = { nav.popBackStack() })
        }
        composable(Routes.DETAIL) { backStack ->
            val id = backStack.arguments?.getString("id")?.toLongOrNull()
                ?: return@composable
            DetailView(
                id = id,
                vm = vm,
                onEdit = { nav.navigate(Routes.update(id)) },
                onDeleted = { nav.popBackStack() }
            )
        }
        composable(Routes.UPDATE) { backStack ->
            val id = backStack.arguments?.getString("id")?.toLongOrNull()
                ?: return@composable
            UpdateView(id = id, vm = vm, onDone = { nav.popBackStack() })
        }
    }
}
