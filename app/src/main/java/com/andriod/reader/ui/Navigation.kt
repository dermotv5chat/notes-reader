package com.andriod.reader.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.andriod.reader.ui.editor.EditorScreen
import com.andriod.reader.ui.list.NoteListScreen
import com.andriod.reader.ui.reader.ReaderScreen
import com.andriod.reader.ui.settings.SettingsScreen

object Routes {
    const val NOTES = "notes"
    const val SETTINGS = "settings"
    const val EDITOR = "editor?fileName={fileName}"
    const val READER = "reader/{fileName}"

    fun editor(fileName: String? = null): String =
        if (fileName == null) "editor" else "editor?fileName=$fileName"

    fun reader(fileName: String): String = "reader/$fileName"
}

@Composable
fun ReaderApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute == Routes.NOTES || currentRoute == Routes.SETTINGS

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.NOTES,
                        onClick = {
                            navController.navigate(Routes.NOTES) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("笔记") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = {
                            navController.navigate(Routes.SETTINGS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("设置") },
                    )
                }
            }
        },
    ) { padding ->
        val isEditor = currentRoute?.startsWith("editor") == true
        val navPadding = if (isEditor) {
            PaddingValues()
        } else {
            padding
        }

        NavHost(
            navController = navController,
            startDestination = Routes.NOTES,
            modifier = Modifier.padding(navPadding),
        ) {
            composable(Routes.NOTES) {
                NoteListScreen(
                    onOpenNote = { navController.navigate(Routes.reader(it)) },
                    onEditNote = { navController.navigate(Routes.editor(it)) },
                    onCreateNote = { navController.navigate(Routes.editor()) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable("editor?fileName={fileName}") {
                EditorScreen(
                    onDone = { navController.popBackStack() },
                )
            }
            composable("editor") {
                EditorScreen(
                    onDone = { navController.popBackStack() },
                )
            }
            composable("reader/{fileName}") { entry ->
                val fileName = entry.arguments?.getString("fileName") ?: return@composable
                ReaderScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
