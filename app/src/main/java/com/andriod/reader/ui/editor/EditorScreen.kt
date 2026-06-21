package com.andriod.reader.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andriod.reader.ui.theme.LocalNoteEditorBackground

private val ToolbarHeight = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onDone: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val editorBackground = LocalNoteEditorBackground.current
    val scrollState = rememberScrollState()
    val lastSavedLabel = viewModel.formattedLastSavedAt()
    val showToolbar = uiState.bodyFocused
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    Scaffold(
        modifier = Modifier.background(editorBackground),
        containerColor = editorBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.onBack()
                        onDone()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = editorBackground,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = if (showToolbar) imeBottom + ToolbarHeight else imeBottom,
                    )
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                BasicTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 34.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.title.isEmpty()) {
                                Text(
                                    text = "标题",
                                    style = TextStyle(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        lineHeight = 34.sp,
                                    ),
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                if (lastSavedLabel != null) {
                    Text(
                        text = lastSavedLabel,
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Box(modifier = Modifier.padding(bottom = 12.dp))
                }

                BasicTextField(
                    value = uiState.body,
                    onValueChange = viewModel::onBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { viewModel.onBodyFocusChanged(it.isFocused) },
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 25.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.body.text.isEmpty()) {
                                Text(
                                    text = "开始书写…",
                                    style = TextStyle(
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        lineHeight = 25.sp,
                                    ),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }

            if (showToolbar) {
                EditorFormattingToolbar(
                    onFormat = viewModel::applyFormat,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = imeBottom),
                )
            }
        }
    }
}
