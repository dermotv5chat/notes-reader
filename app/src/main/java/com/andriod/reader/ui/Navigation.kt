package com.andriod.reader.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.andriod.reader.service.TtsPlaybackManager
import com.andriod.reader.ui.editor.EditorScreen
import com.andriod.reader.ui.list.NoteListScreen
import com.andriod.reader.ui.player.TtsMiniPlayerBar
import com.andriod.reader.ui.reader.ReaderScreen
import com.andriod.reader.ui.settings.SettingsScreen

object Routes {
    const val NOTES = "notes"
    const val SETTINGS = "settings"
    const val EDITOR = "editor?fileName={fileName}"
    const val EDITOR_IN_FOLDER = "editor?parentFolder={parentFolder}"
    const val READER = "reader?fileName={fileName}"

    fun editor(fileName: String? = null, parentFolder: String = ""): String =
        when {
            fileName != null -> "editor?fileName=${Uri.encode(fileName)}"
            parentFolder.isNotEmpty() -> "editor?parentFolder=${Uri.encode(parentFolder)}"
            else -> "editor"
        }

    fun reader(fileName: String): String = "reader?fileName=${Uri.encode(fileName)}"
}

@Composable
fun ReaderApp(
    openReaderFileName: String? = null,
    onOpenReaderConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val session by TtsPlaybackManager.session.collectAsState()
    val showBottomBar = currentRoute == Routes.NOTES || currentRoute == Routes.SETTINGS
    val isReader = currentRoute?.startsWith("reader") == true
    val isEditor = currentRoute?.startsWith("editor") == true
    val showMiniBar = session.hasActiveSession && !isReader && (
        currentRoute == Routes.NOTES ||
            currentRoute == Routes.SETTINGS ||
            isEditor
        )

    LaunchedEffect(openReaderFileName) {
        val fileName = openReaderFileName ?: return@LaunchedEffect
        navController.navigate(Routes.reader(fileName)) {
            launchSingleTop = true
        }
        onOpenReaderConsumed()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (showMiniBar || showBottomBar) {
                Column {
                    if (showMiniBar) {
                        TtsMiniPlayerBar(
                            session = session,
                            onOpenReader = {
                                session.fileName?.let { fileName ->
                                    navController.navigate(Routes.reader(fileName)) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onTogglePlayPause = TtsPlaybackManager::togglePlayPause,
                        )
                    }
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
                }
            }
        },
    ) { padding ->
        val navPadding = if (isEditor && !showMiniBar) {
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
                    onCreateNote = { parentFolder ->
                        navController.navigate(Routes.editor(parentFolder = parentFolder))
                    },
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
            composable("editor?parentFolder={parentFolder}") {
                EditorScreen(
                    onDone = { navController.popBackStack() },
                )
            }
            composable("editor") {
                EditorScreen(
                    onDone = { navController.popBackStack() },
                )
            }
            composable("reader?fileName={fileName}") {
                ReaderScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { fileName -> navController.navigate(Routes.editor(fileName)) },
                )
            }
        }
    }
}
