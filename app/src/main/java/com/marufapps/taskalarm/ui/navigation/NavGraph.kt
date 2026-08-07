package com.marufapps.taskalarm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.marufapps.taskalarm.ui.screens.AddEditTaskScreen
import com.marufapps.taskalarm.ui.screens.HomeScreen
import com.marufapps.taskalarm.ui.screens.SettingsScreen
import com.marufapps.taskalarm.ui.screens.TaskDetailScreen

object Routes {
    const val HOME = "home"
    const val ADD_TASK = "add_task"
    const val EDIT_TASK = "edit_task/{taskId}"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val SETTINGS = "settings"

    fun editTask(id: Long) = "edit_task/$id"
    fun taskDetail(id: Long) = "task_detail/$id"
}

@Composable
fun TaskAlarmNavGraph(startTaskId: Long? = null) {
    val navController = rememberNavController()

    LaunchedEffect(startTaskId) {
        if (startTaskId != null && startTaskId != -1L) {
            navController.navigate(Routes.taskDetail(startTaskId))
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddTask = { navController.navigate(Routes.ADD_TASK) },
                onOpenTask = { id -> navController.navigate(Routes.taskDetail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.ADD_TASK) {
            AddEditTaskScreen(taskId = null, onDone = { navController.popBackStack() })
        }
        composable(
            Routes.EDIT_TASK,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("taskId")
            AddEditTaskScreen(taskId = id, onDone = { navController.popBackStack() })
        }
        composable(
            Routes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("taskId") ?: -1L
            TaskDetailScreen(
                taskId = id,
                onBack = { navController.popBackStack() },
                onEdit = { taskId -> navController.navigate(Routes.editTask(taskId)) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
