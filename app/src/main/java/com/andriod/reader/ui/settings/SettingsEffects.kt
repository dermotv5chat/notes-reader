package com.andriod.reader.ui.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun SettingsLifecycleEffects(
    viewModel: SettingsViewModel,
    refreshTts: Boolean = true,
    refreshMaintenance: Boolean = false,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    LaunchedEffect(context) {
        viewModel.setHostContext(context)
        if (refreshTts) {
            viewModel.refreshTtsInfo()
        }
        if (refreshMaintenance) {
            viewModel.refreshLogStats()
            viewModel.refreshStorageStats()
        }
    }

    DisposableEffect(lifecycleOwner, context, refreshTts, refreshMaintenance) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.setHostContext(context)
                if (refreshTts) {
                    viewModel.refreshTtsInfo()
                }
                if (refreshMaintenance) {
                    viewModel.refreshLogStats()
                    viewModel.refreshStorageStats()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
fun SettingsSnackbarEffect(
    testMessage: String?,
    snackbar: SnackbarHostState,
    onClear: () -> Unit,
) {
    LaunchedEffect(testMessage) {
        testMessage?.let {
            snackbar.showSnackbar(it)
            onClear()
        }
    }
}
