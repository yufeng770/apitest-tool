package com.example.yuapitest

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private enum class ResponseTab(val label: String) {
    PRETTY("Pretty"),
    TREE("Tree"),
    RAW("Raw"),
    HEADERS("Headers"),
    COOKIES("Cookies")
}

@Composable
fun ResponseWorkspace(
    response: ApiResponseResult?,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(response?.body) { mutableStateOf(ResponseTab.PRETTY) }
    var searchVisible by remember(response?.body) { mutableStateOf(false) }
    val searchState = remember(response?.body) { ResponseSearchState() }
    var fullscreen by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Column(modifier = modifier.fillMaxSize()) {
        ResponseToolbar(
            response = response,
            onSearch = {
                searchVisible = !searchVisible
                if (!searchVisible) searchState.clear()
            },
            onCopy = { response?.let { clipboard.setText(AnnotatedString(it.body)) } },
            onFullscreen = {
                searchVisible = true
                fullscreen = true
            }
        )

        AnimatedContent(
            targetState = response,
            transitionSpec = {
                fadeIn(animationSpec = defaultSpringSpec()) togetherWith
                    fadeOut(animationSpec = defaultSpringSpec())
            },
            label = "response-content",
            modifier = Modifier.fillMaxSize()
        ) { current ->
            when {
                isSending && current == null -> LoadingResponse()
                current == null -> EmptyResponse()
                else -> ResponseContent(
                    response = current,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    searchVisible = searchVisible,
                    searchState = searchState,
                    onCloseSearch = {
                        searchVisible = false
                        searchState.clear()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (fullscreen && response != null) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Response", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { clipboard.setText(AnnotatedString(response.body)) }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "复制响应")
                        }
                        IconButton(onClick = { fullscreen = false }) {
                            Icon(Icons.Outlined.Close, contentDescription = "退出全屏")
                        }
                    }
                    ResponseContent(
                        response = response,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        searchVisible = searchVisible,
                        searchState = searchState,
                        onCloseSearch = {
                            searchVisible = false
                            searchState.clear()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponseToolbar(
    response: ApiResponseResult?,
    onSearch: () -> Unit,
    onCopy: () -> Unit,
    onFullscreen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (response == null) {
            Text(
                "Response",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        } else {
            ResponseMeta(response = response, modifier = Modifier.weight(1f))
            IconButton(onClick = onSearch, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Search, contentDescription = "搜索响应", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "复制响应", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onFullscreen, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Fullscreen, contentDescription = "全屏查看", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ResponseContent(
    response: ApiResponseResult,
    selectedTab: ResponseTab,
    onTabSelected: (ResponseTab) -> Unit,
    searchVisible: Boolean,
    searchState: ResponseSearchState,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedBody by produceState(
        initialValue = ParsedBody(value = null, prettyText = response.body),
        key1 = response.body
    ) {
        value = withContext(Dispatchers.Default) {
            val parsed = (JsonTools.parse(response.body) as? JsonParseResult.Success)?.value
            ParsedBody(
                value = parsed,
                prettyText = parsed?.let(JsonTools::prettyPrint) ?: response.body
            )
        }
    }
    val parsed = parsedBody.value
    val prettyText = parsedBody.prettyText
    val searchable = selectedTab == ResponseTab.PRETTY || selectedTab == ResponseTab.RAW
    val searchableText = if (selectedTab == ResponseTab.RAW) response.body else prettyText
    ResponseSearchEngine(
        text = searchableText,
        enabled = searchable && searchVisible,
        state = searchState
    )

    Column(modifier = modifier) {
        ResponseTabs(selectedTab, onTabSelected)
        if (searchVisible && searchable) {
            CompactSearchBar(
                state = searchState,
                onClose = onCloseSearch
            )
        }

        when (selectedTab) {
            ResponseTab.PRETTY -> CodeReader(
                text = prettyText,
                highlight = parsed != null,
                searchState = searchState,
                modifier = Modifier.weight(1f)
            )
            ResponseTab.TREE -> if (parsed != null) {
                JsonTreeReader(parsed, Modifier.weight(1f))
            } else {
                NonJsonMessage(Modifier.weight(1f))
            }
            ResponseTab.RAW -> CodeReader(
                text = response.body,
                highlight = false,
                searchState = searchState,
                modifier = Modifier.weight(1f)
            )
            ResponseTab.HEADERS -> CodeReader(
                text = response.headers.joinToString("\n") { "${it.key}: ${it.value}" },
                highlight = false,
                modifier = Modifier.weight(1f)
            )
            ResponseTab.COOKIES -> CodeReader(
                text = response.headers
                    .filter { it.key.equals("Set-Cookie", true) }
                    .joinToString("\n") { it.value },
                highlight = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ResponseSearchEngine(
    text: String,
    enabled: Boolean,
    state: ResponseSearchState
) {
    val query = state.query
    LaunchedEffect(text, enabled, query) {
        if (!enabled || query.isBlank()) {
            state.updateResults(IntArray(0))
            return@LaunchedEffect
        }
        state.isSearching = true
        delay(SearchDebounceMs)
        val matches = withContext(Dispatchers.Default) {
            findMatchStarts(text, query)
        }
        state.updateResults(matches, query)
    }
}

@Composable
private fun CompactSearchBar(
    state: ResponseSearchState,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            BasicTextField(
                value = state.query,
                onValueChange = { state.query = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                            if (state.query.isEmpty()) {
                                Text(
                                    "搜索响应",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
        }
        Text(
            text = when {
                state.isSearching -> "..."
                state.matches.isEmpty() -> "0/0"
                else -> "${state.currentIndex + 1}/${state.matches.size}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .widthIn(min = 38.dp)
                .padding(start = 6.dp)
        )
        IconButton(onClick = state::previous, enabled = state.matches.isNotEmpty(), modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "上一个结果", modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = state::next, enabled = state.matches.isNotEmpty(), modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "下一个结果", modifier = Modifier.size(20.dp))
        }
        IconButton(
            onClick = {
                keyboardController?.hide()
                onClose()
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "关闭搜索", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ResponseMeta(response: ApiResponseResult, modifier: Modifier = Modifier) {
    val statusColor = when (response.statusCode) {
        in 200..299 -> Color(0xFF15803D)
        in 400..499 -> Color(0xFFC2410C)
        in 500..599 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            response.statusCode?.let { code ->
                val message = response.statusMessage.ifBlank { statusReason(code) }
                if (message.isBlank()) code.toString() else "$code $message"
            } ?: "请求失败",
            color = statusColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
        response.elapsedMs?.let { MetaText("${it} ms") }
        response.bodySizeBytes?.let { MetaText(formatBytes(it)) }
        if (response.protocol.isNotBlank()) MetaText(response.protocol)
        response.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MetaText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun ResponseTabs(selected: ResponseTab, onSelected: (ResponseTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
    ) {
        ResponseTab.entries.forEach { tab ->
            Column(
                modifier = Modifier
                    .clickable { onSelected(tab) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected == tab) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected == tab) FontWeight.Bold else FontWeight.Normal
                )
                Surface(
                    color = if (selected == tab) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(width = 28.dp, height = 2.dp)
                ) {}
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun CodeReader(
    text: String,
    highlight: Boolean,
    searchState: ResponseSearchState? = null,
    modifier: Modifier = Modifier
) {
    val baseContent by produceState(
        initialValue = AnnotatedString(text),
        key1 = text,
        key2 = highlight
    ) {
        value = withContext(Dispatchers.Default) {
            if (highlight) highlightJson(text) else AnnotatedString(text)
        }
    }
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    var layoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val currentStart = searchState?.matches?.getOrNull(searchState.currentIndex)
    val currentQuery = searchState?.resultQuery.orEmpty()
    val currentBounds = remember(layoutResult, currentStart, currentQuery) {
        val layout = layoutResult
        if (layout == null || currentStart == null || currentQuery.isEmpty()) {
            null
        } else {
            val startBox = layout.getBoundingBox(currentStart)
            val endOffset = (currentStart + currentQuery.length - 1).coerceAtMost(text.lastIndex)
            val endBox = layout.getBoundingBox(endOffset)
            if (endBox.top == startBox.top) {
                androidx.compose.ui.geometry.Rect(startBox.left, startBox.top, endBox.right, startBox.bottom)
            } else {
                startBox
            }
        }
    }

    LaunchedEffect(currentStart, layoutResult) {
        val start = currentStart ?: return@LaunchedEffect
        val layout = layoutResult ?: return@LaunchedEffect
        val box = layout.getBoundingBox(start)
        val margin = with(density) { 20.dp.roundToPx() }
        horizontalScroll.scrollTo((box.left.toInt() - margin).coerceIn(0, horizontalScroll.maxValue))
        verticalScroll.scrollTo((box.top.toInt() - margin).coerceIn(0, verticalScroll.maxValue))
    }
    Surface(modifier = modifier.fillMaxSize(), color = Color(0xFF15171A)) {
        SelectionContainer {
            Text(
                text = baseContent,
                color = Color(0xFFE5E7EB),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 20.sp,
                onTextLayout = { layoutResult = it },
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScroll)
                    .verticalScroll(verticalScroll)
                    .padding(12.dp)
                    .drawWithContent {
                        drawContent()
                        currentBounds?.let { bounds ->
                            drawRect(
                                color = Color(0x66F97316),
                                topLeft = bounds.topLeft,
                                size = bounds.size
                            )
                            drawRect(
                                color = Color(0xFFF97316),
                                topLeft = bounds.topLeft,
                                size = bounds.size,
                                style = Stroke(width = 2f)
                            )
                        }
                    }
            )
        }
    }
}

private suspend fun findMatchStarts(text: String, query: String): IntArray {
    if (query.isBlank()) return IntArray(0)
    return buildList<Int> {
        var start = text.indexOf(query, ignoreCase = true)
        while (start >= 0) {
            if ((size and 255) == 0) currentCoroutineContext().ensureActive()
            add(start)
            start = text.indexOf(query, start + query.length, ignoreCase = true)
        }
    }.toIntArray()
}

@Composable
private fun JsonTreeReader(value: JsonValue, modifier: Modifier = Modifier) {
    val expanded = remember(value) { mutableStateMapOf<String, Boolean>() }
    var forceExpanded by remember(value) { mutableStateOf<Boolean?>(null) }
    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { forceExpanded = true; expanded.clear() }) {
                Icon(Icons.Outlined.UnfoldMore, contentDescription = "全部展开")
            }
            IconButton(onClick = { forceExpanded = false; expanded.clear() }) {
                Icon(Icons.Outlined.UnfoldLess, contentDescription = "全部折叠")
            }
        }
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp)
            ) {
                TreeNode(null, value, "$", 0, expanded, forceExpanded)
            }
        }
    }
}

@Composable
private fun TreeNode(
    label: String?,
    value: JsonValue,
    path: String,
    depth: Int,
    expanded: SnapshotStateMap<String, Boolean>,
    forceExpanded: Boolean?
) {
    when (value) {
        is JsonValue.Obj -> TreeContainer(label, "{", "}", value.entries.size, path, depth, expanded, forceExpanded) {
            value.entries.forEachIndexed { index, member ->
                TreeNode("${JsonTools.escapeString(member.key)}: ", member.value, "$path.$index", depth + 1, expanded, forceExpanded)
            }
        }
        is JsonValue.Arr -> TreeContainer(label, "[", "]", value.values.size, path, depth, expanded, forceExpanded) {
            value.values.forEachIndexed { index, item ->
                TreeNode("$index: ", item, "$path[$index]", depth + 1, expanded, forceExpanded)
            }
        }
        else -> TreeLine("${label.orEmpty()}${JsonTools.compactPrint(value)}", depth)
    }
}

@Composable
private fun TreeContainer(
    label: String?,
    open: String,
    close: String,
    count: Int,
    path: String,
    depth: Int,
    expanded: SnapshotStateMap<String, Boolean>,
    forceExpanded: Boolean?,
    children: @Composable () -> Unit
) {
    val isExpanded = expanded[path] ?: forceExpanded ?: (depth < 2)
    Row(
        modifier = Modifier
            .clickable { expanded[path] = !isExpanded }
            .padding(start = (depth * 16 + 8).dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (isExpanded) "−" else "+", modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
        Text(
            if (isExpanded) "${label.orEmpty()}$open" else "${label.orEmpty()}$open$count$close",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (isExpanded) {
        children()
        TreeLine(close, depth)
    }
}

@Composable
private fun TreeLine(text: String, depth: Int) {
    Text(
        text,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = (depth * 16 + 32).dp, top = 3.dp, bottom = 3.dp)
    )
}

@Composable
private fun EmptyResponse() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("输入 URL 后发送请求", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoadingResponse() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun NonJsonMessage(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("响应不是 JSON", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

private data class ParsedBody(
    val value: JsonValue?,
    val prettyText: String
)

@Stable
private class ResponseSearchState {
    var query by mutableStateOf("")
    var matches by mutableStateOf(IntArray(0))
        private set
    var resultQuery by mutableStateOf("")
        private set
    var currentIndex by mutableStateOf(-1)
        private set
    var isSearching by mutableStateOf(false)

    fun updateResults(newMatches: IntArray, matchedQuery: String = query) {
        matches = newMatches
        resultQuery = matchedQuery
        currentIndex = if (newMatches.isEmpty()) -1 else 0
        isSearching = false
    }

    fun previous() {
        if (matches.isEmpty()) return
        currentIndex = if (currentIndex <= 0) matches.lastIndex else currentIndex - 1
    }

    fun next() {
        if (matches.isEmpty()) return
        currentIndex = if (currentIndex >= matches.lastIndex) 0 else currentIndex + 1
    }

    fun clear() {
        query = ""
        updateResults(IntArray(0), "")
    }
}

private const val SearchDebounceMs = 120L

private fun statusReason(code: Int): String = when (code) {
    200 -> "OK"
    201 -> "Created"
    202 -> "Accepted"
    204 -> "No Content"
    301 -> "Moved Permanently"
    302 -> "Found"
    304 -> "Not Modified"
    400 -> "Bad Request"
    401 -> "Unauthorized"
    403 -> "Forbidden"
    404 -> "Not Found"
    405 -> "Method Not Allowed"
    409 -> "Conflict"
    422 -> "Unprocessable Content"
    429 -> "Too Many Requests"
    500 -> "Internal Server Error"
    502 -> "Bad Gateway"
    503 -> "Service Unavailable"
    504 -> "Gateway Timeout"
    else -> ""
}
