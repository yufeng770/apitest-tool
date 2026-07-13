package com.example.yuapitest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RequestEditorPanel(
    tab: RequestEditorTab,
    draft: ApiRequestDraft,
    onDraftChange: (ApiRequestDraft) -> Unit
) {
    val maxHeight = if (tab == RequestEditorTab.BODY) 250.dp else 190.dp
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            when (tab) {
                RequestEditorTab.AUTH -> AuthEditor(
                    auth = draft.auth,
                    onChange = { onDraftChange(draft.copy(auth = it)) }
                )

                RequestEditorTab.HEADERS -> PairListEditor(
                    pairs = draft.headers,
                    keyPlaceholder = "Header",
                    keySuggestions = CommonRequestHeaderKeys,
                    onChange = { onDraftChange(draft.copy(headers = it)) }
                )

                RequestEditorTab.PARAMS -> PairListEditor(
                    pairs = draft.params,
                    keyPlaceholder = "Parameter",
                    onChange = { onDraftChange(draft.copy(params = it)) }
                )

                RequestEditorTab.BODY -> BodyEditor(
                    draft = draft,
                    onDraftChange = onDraftChange
                )
            }
        }
    }
}

@Composable
private fun AuthEditor(auth: AuthConfig, onChange: (AuthConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AuthType.entries.forEach { type ->
                CompactChoice(
                    selected = auth.type == type,
                    onClick = { onChange(auth.copy(type = type)) },
                    label = type.label
                )
            }
        }

        when (auth.type) {
            AuthType.NONE -> Unit
            AuthType.BEARER -> CompactTextField(
                value = auth.token,
                onValueChange = { onChange(auth.copy(token = it)) },
                label = "Token"
            )
            AuthType.BASIC -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactTextField(
                    value = auth.username,
                    onValueChange = { onChange(auth.copy(username = it)) },
                    label = "Username",
                    modifier = Modifier.weight(1f)
                )
                CompactTextField(
                    value = auth.password,
                    onValueChange = { onChange(auth.copy(password = it)) },
                    label = "Password",
                    password = true,
                    modifier = Modifier.weight(1f)
                )
            }
            AuthType.API_KEY -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactTextField(
                        value = auth.apiKeyName,
                        onValueChange = { onChange(auth.copy(apiKeyName = it)) },
                        label = "Key",
                        modifier = Modifier.weight(0.8f)
                    )
                    CompactTextField(
                        value = auth.apiKeyValue,
                        onValueChange = { onChange(auth.copy(apiKeyValue = it)) },
                        label = "Value",
                        modifier = Modifier.weight(1.2f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ApiKeyLocation.entries.forEach { location ->
                        CompactChoice(
                            selected = auth.apiKeyLocation == location,
                            onClick = { onChange(auth.copy(apiKeyLocation = location)) },
                            label = if (location == ApiKeyLocation.HEADER) "Header" else "Query"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactChoice(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Surface(
        modifier = Modifier
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    password: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    CompactFieldSurface(modifier = modifier, focused = focused) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                CompactFieldDecoration(
                    valueIsEmpty = value.isEmpty(),
                    placeholder = label,
                    innerTextField = innerTextField
                )
            }
        )
    }
}

@Composable
fun PairListEditor(
    pairs: List<HeaderPair>,
    keyPlaceholder: String,
    onChange: (List<HeaderPair>) -> Unit,
    keySuggestions: List<String> = emptyList()
) {
    val visiblePairs = pairs.ifEmpty { listOf(HeaderPair()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        visiblePairs.forEachIndexed { index, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (keySuggestions.isEmpty()) {
                    CompactTextField(
                        value = pair.key,
                        onValueChange = { onChange(pairs.updatePair(index, pair.copy(key = it))) },
                        label = keyPlaceholder,
                        modifier = Modifier.weight(0.72f)
                    )
                } else {
                    SuggestedKeyField(
                        value = pair.key,
                        onValueChange = { onChange(pairs.updatePair(index, pair.copy(key = it))) },
                        suggestions = keySuggestions,
                        modifier = Modifier.weight(0.72f)
                    )
                }
                StableValueField(
                    value = pair.value,
                    onValueChange = { onChange(pairs.updatePair(index, pair.copy(value = it))) },
                    modifier = Modifier.weight(1.28f)
                )
                IconButton(
                    onClick = { onChange(pairs.removePair(index)) },
                    enabled = visiblePairs.size > 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp))
                }
                if (index == 0) {
                    IconButton(
                        onClick = { onChange(visiblePairs + HeaderPair()) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "添加", modifier = Modifier.size(18.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestedKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val options = remember(query, suggestions) {
        if (query.isBlank()) suggestions else suggestions.filter { it.contains(query, true) }
    }
    val menuExpanded = expanded && options.isNotEmpty()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    ExposedDropdownMenuBox(
        expanded = menuExpanded,
        onExpandedChange = {
            expanded = it
            if (it) query = ""
        },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .height(40.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = compactFieldBorder(focused)
        ) {
            BasicTextField(
                value = value,
                onValueChange = {
                    onValueChange(it)
                    query = it
                    expanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(start = 10.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) CompactPlaceholder("Header")
                            innerTextField()
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(menuExpanded)
                    }
                }
            )
        }
        ExposedDropdownMenu(menuExpanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        query = ""
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StableValueField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (value != state.text) state = TextFieldValue(value, TextRange(value.length))
    }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    CompactFieldSurface(modifier = modifier, focused = focused) {
        BasicTextField(
            value = state,
            onValueChange = {
                state = it
                onValueChange(it.text)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                CompactFieldDecoration(
                    valueIsEmpty = state.text.isEmpty(),
                    placeholder = "Value",
                    innerTextField = innerTextField
                )
            }
        )
    }
}

@Composable
private fun CompactFieldSurface(
    modifier: Modifier,
    focused: Boolean,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.height(40.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = compactFieldBorder(focused),
        content = content
    )
}

@Composable
private fun compactFieldBorder(focused: Boolean): BorderStroke {
    return BorderStroke(
        width = if (focused) 2.dp else 1.dp,
        color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    )
}

@Composable
private fun CompactFieldDecoration(
    valueIsEmpty: Boolean,
    placeholder: String,
    innerTextField: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (valueIsEmpty) CompactPlaceholder(placeholder)
        innerTextField()
    }
}

@Composable
private fun CompactPlaceholder(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun BodyEditor(draft: ApiRequestDraft, onDraftChange: (ApiRequestDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BodyType.entries.forEach { type ->
                FilterChip(
                    selected = draft.bodyType == type,
                    onClick = { onDraftChange(draft.copy(bodyType = type)) },
                    label = { Text(type.label) }
                )
            }
        }
        when (draft.bodyType) {
            BodyType.JSON -> JsonCodeEditor(
                value = draft.body,
                onValueChange = { onDraftChange(draft.copy(body = it)) },
                highlight = true
            )
            BodyType.RAW -> JsonCodeEditor(
                value = draft.body,
                onValueChange = { onDraftChange(draft.copy(body = it)) },
                highlight = false
            )
            BodyType.FORM -> PairListEditor(
                pairs = draft.formFields,
                keyPlaceholder = "Field",
                onChange = { onDraftChange(draft.copy(formFields = it)) }
            )
            BodyType.MULTIPART -> PairListEditor(
                pairs = draft.multipartFields,
                keyPlaceholder = "Part",
                onChange = { onDraftChange(draft.copy(multipartFields = it)) }
            )
        }
    }
}

@Composable
private fun JsonCodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    highlight: Boolean
) {
    var state by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (value != state.text) state = TextFieldValue(value, TextRange(value.length))
    }
    Surface(
        color = CodeBackground,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp, max = 190.dp)
    ) {
        BasicTextField(
            value = state,
            onValueChange = { incoming ->
                val adjusted = if (highlight) adjustCodeEdit(state, incoming) else incoming
                state = adjusted
                onValueChange(adjusted.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = CodeForeground,
                lineHeight = 18.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38BDF8)),
            visualTransformation = if (highlight) JsonSyntaxTransformation else VisualTransformation.None
        )
    }
}

private fun adjustCodeEdit(old: TextFieldValue, incoming: TextFieldValue): TextFieldValue {
    if (incoming.text.length != old.text.length + 1 || !incoming.selection.collapsed) return incoming
    val insertedAt = incoming.selection.start - 1
    val inserted = incoming.text.getOrNull(insertedAt) ?: return incoming
    val closing = when (inserted) {
        '{' -> '}'
        '[' -> ']'
        '"' -> '"'
        else -> null
    }
    if (closing != null) {
        val text = incoming.text.substring(0, incoming.selection.start) + closing +
            incoming.text.substring(incoming.selection.start)
        return incoming.copy(text = text, selection = TextRange(incoming.selection.start))
    }
    if (inserted == '\n') {
        val beforeNewline = incoming.text.substring(0, insertedAt)
        val indent = beforeNewline.substringAfterLast('\n').takeWhile { it == ' ' || it == '\t' }
        val text = incoming.text.substring(0, incoming.selection.start) + indent +
            incoming.text.substring(incoming.selection.start)
        return incoming.copy(text = text, selection = TextRange(incoming.selection.start + indent.length))
    }
    return incoming
}

private object JsonSyntaxTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(highlightJson(text.text), OffsetMapping.Identity)
    }
}

fun highlightJson(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    JsonTokenRegex.findAll(text).forEach { match ->
        val token = match.value
        val color = when {
            token.startsWith('"') && text.drop(match.range.last + 1).trimStart().startsWith(':') -> CodeKey
            token.startsWith('"') -> CodeString
            token == "true" || token == "false" || token == "null" -> CodeLiteral
            else -> CodeNumber
        }
        builder.addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1)
    }
    return builder.toAnnotatedString()
}

private val JsonTokenRegex = Regex("\"(?:\\\\.|[^\"\\\\])*\"|-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b|\\b(?:true|false|null)\\b")
private val CodeBackground = Color(0xFF15171A)
private val CodeForeground = Color(0xFFE5E7EB)
private val CodeKey = Color(0xFF7DD3FC)
private val CodeString = Color(0xFF86EFAC)
private val CodeNumber = Color(0xFFFDE68A)
private val CodeLiteral = Color(0xFFC4B5FD)

private val AuthType.label: String
    get() = when (this) {
        AuthType.NONE -> "None"
        AuthType.BEARER -> "Bearer"
        AuthType.BASIC -> "Basic"
        AuthType.API_KEY -> "API Key"
    }

private val BodyType.label: String
    get() = when (this) {
        BodyType.JSON -> "JSON"
        BodyType.RAW -> "Raw"
        BodyType.FORM -> "Form"
        BodyType.MULTIPART -> "Multipart"
    }

private fun List<HeaderPair>.updatePair(index: Int, value: HeaderPair): List<HeaderPair> {
    val source = ifEmpty { listOf(HeaderPair()) }
    return source.mapIndexed { itemIndex, item -> if (itemIndex == index) value else item }
}

private fun List<HeaderPair>.removePair(index: Int): List<HeaderPair> {
    return filterIndexed { itemIndex, _ -> itemIndex != index }.ifEmpty { listOf(HeaderPair()) }
}

private val CommonRequestHeaderKeys = listOf(
    "Authorization",
    "Content-Type",
    "Accept",
    "Cookie",
    "User-Agent",
    "Cache-Control",
    "Accept-Encoding",
    "Origin",
    "Referer",
    "X-API-Key"
)
