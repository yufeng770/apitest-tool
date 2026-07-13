package com.example.yuapitest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private enum class MainDestination { REQUEST, HISTORY }

enum class RequestEditorTab(val label: String) {
    AUTH("Auth"),
    HEADERS("Headers"),
    PARAMS("Params"),
    BODY("Body")
}

@Composable
fun MobileApiTesterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val historyRepository = remember { RequestHistoryRepository(context) }
    val apiClient = remember { ApiHttpClient() }
    val scope = rememberCoroutineScope()

    var destination by remember { mutableStateOf(MainDestination.REQUEST) }
    var draft by remember { mutableStateOf(ApiRequestDraft()) }
    var response by remember { mutableStateOf<ApiResponseResult?>(null) }
    var history by remember { mutableStateOf(historyRepository.load()) }
    var isSending by remember { mutableStateOf(false) }
    var selectedEditor by remember { mutableStateOf<RequestEditorTab?>(null) }

    fun sendRequest() {
        if (isSending) return
        selectedEditor = null
        scope.launch {
            isSending = true
            try {
                val result = apiClient.execute(draft)
                response = result
                if (result.requestWasSent) {
                    history = historyRepository.addRequest(draft)
                }
            } catch (error: Throwable) {
                response = ApiResponseResult(
                    errorMessage = "内部错误：${error.message ?: error.javaClass.simpleName}"
                )
            } finally {
                isSending = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            CompactBottomBar {
                CompactNavigationItem(
                    selected = destination == MainDestination.REQUEST,
                    onClick = { destination = MainDestination.REQUEST },
                    icon = Icons.Outlined.Terminal,
                    contentDescription = "Request"
                )
                CompactNavigationItem(
                    selected = destination == MainDestination.HISTORY,
                    onClick = { destination = MainDestination.HISTORY },
                    icon = Icons.Outlined.History,
                    contentDescription = "History"
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (destination) {
                MainDestination.REQUEST -> RequestWorkspace(
                    draft = draft,
                    response = response,
                    isSending = isSending,
                    selectedEditor = selectedEditor,
                    onDraftChange = { draft = it },
                    onEditorSelected = { tab ->
                        selectedEditor = if (selectedEditor == tab) null else tab
                    },
                    onClear = {
                        draft = ApiRequestDraft()
                        response = null
                        selectedEditor = null
                    },
                    onSend = ::sendRequest
                )

                MainDestination.HISTORY -> HistoryScreen(
                    history = history,
                    onRestore = { entry ->
                        draft = entry.toDraft()
                        selectedEditor = null
                        destination = MainDestination.REQUEST
                    },
                    onClear = {
                        historyRepository.clear()
                        history = emptyList()
                    }
                )
            }
        }
    }
}

@Composable
private fun RequestWorkspace(
    draft: ApiRequestDraft,
    response: ApiResponseResult?,
    isSending: Boolean,
    selectedEditor: RequestEditorTab?,
    onDraftChange: (ApiRequestDraft) -> Unit,
    onEditorSelected: (RequestEditorTab) -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        RequestBar(
            method = draft.method,
            url = draft.url,
            isSending = isSending,
            onMethodChange = { onDraftChange(draft.copy(method = it)) },
            onUrlChange = { onDraftChange(draft.copy(url = it)) },
            onClear = onClear,
            onSend = onSend
        )

        RequestTabStrip(
            draft = draft,
            selected = selectedEditor,
            onSelected = onEditorSelected
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(modifier = Modifier.weight(1f)) {
            ResponseWorkspace(
                response = response,
                isSending = isSending,
                modifier = Modifier.fillMaxSize()
            )
            selectedEditor?.let { editor ->
                Box(
                    modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(1f)
                ) {
                    RequestEditorPanel(
                        tab = editor,
                        draft = draft,
                        onDraftChange = onDraftChange
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestBar(
    method: String,
    url: String,
    isSending: Boolean,
    onMethodChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(76.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable { expanded = true },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        method,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                SupportedHttpMethods.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            onMethodChange(item)
                            expanded = false
                        }
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            BasicTextField(
                value = url,
                onValueChange = onUrlChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(start = 12.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (url.isEmpty()) {
                                Text(
                                    "https://api.example.com",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onClear()
                            },
                            enabled = !isSending,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "清空请求",
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            )
        }

        FilledIconButton(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onSend()
            },
            enabled = !isSending,
            modifier = Modifier.size(42.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "发送请求")
            }
        }
    }
}

@Composable
private fun CompactBottomBar(content: @Composable RowScope.() -> Unit) {
    Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun RowScope.CompactNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun RequestTabStrip(
    draft: ApiRequestDraft,
    selected: RequestEditorTab?,
    onSelected: (RequestEditorTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RequestEditorTab.entries.forEach { tab ->
            val enabled = tab != RequestEditorTab.BODY || draft.method.supportsRequestBody()
            val count = when (tab) {
                RequestEditorTab.HEADERS -> draft.headers.count { it.key.isNotBlank() }
                RequestEditorTab.PARAMS -> draft.params.count { it.key.isNotBlank() }
                else -> 0
            }
            val label = if (count > 0) "${tab.label} ($count)" else tab.label
            val isSelected = selected == tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (enabled) 1f else 0.38f)
                    .clickable(enabled = enabled) { onSelected(tab) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(width = 28.dp, height = 2.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    history: List<RequestHistoryEntry>,
    onRestore: (RequestHistoryEntry) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (history.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("清空") }
            }
        }
        HorizontalDivider()

        if (history.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    "暂无请求记录",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(history, key = { it.id }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRestore(entry) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            entry.method,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = methodColor(entry.method),
                            modifier = Modifier.width(52.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.url,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                formatHistoryTimestamp(entry.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
                }
            }
        }
    }
}

@Composable
private fun methodColor(method: String) = when (method) {
    "GET" -> MaterialTheme.colorScheme.primary
    "POST" -> MaterialTheme.colorScheme.tertiary
    "DELETE" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.secondary
}

private fun formatHistoryTimestamp(timestamp: Long): String {
    return SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
