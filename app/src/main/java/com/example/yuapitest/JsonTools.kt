package com.example.yuapitest

sealed class JsonValue {
    data class Obj(val entries: List<JsonMember>) : JsonValue()
    data class Arr(val values: List<JsonValue>) : JsonValue()
    data class Str(val value: String) : JsonValue()
    data class Num(val raw: String) : JsonValue()
    data class Bool(val value: Boolean) : JsonValue()
    data object Null : JsonValue()
}

data class JsonMember(
    val key: String,
    val value: JsonValue
)

sealed class JsonParseResult {
    data class Success(val value: JsonValue) : JsonParseResult()
    data class Failure(val message: String) : JsonParseResult()
}

object JsonTools {
    fun parse(text: String): JsonParseResult {
        return try {
            val parser = Parser(text)
            JsonParseResult.Success(parser.parse())
        } catch (error: JsonParseException) {
            JsonParseResult.Failure(error.message ?: "JSON 解析失败")
        }
    }

    fun prettyPrint(value: JsonValue): String {
        val builder = StringBuilder()
        appendPretty(value, builder, 0)
        return builder.toString()
    }

    fun compactPrint(value: JsonValue): String {
        val builder = StringBuilder()
        appendCompact(value, builder)
        return builder.toString()
    }

    fun escapeString(value: String): String {
        val builder = StringBuilder(value.length + 8)
        builder.append('"')
        value.forEach { char ->
            when (char) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> {
                    if (char.code < 0x20) {
                        builder.append("\\u")
                        builder.append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        builder.append(char)
                    }
                }
            }
        }
        builder.append('"')
        return builder.toString()
    }

    private fun appendPretty(value: JsonValue, builder: StringBuilder, depth: Int) {
        when (value) {
            is JsonValue.Obj -> appendPrettyObject(value, builder, depth)
            is JsonValue.Arr -> appendPrettyArray(value, builder, depth)
            is JsonValue.Str -> builder.append(escapeString(value.value))
            is JsonValue.Num -> builder.append(value.raw)
            is JsonValue.Bool -> builder.append(value.value)
            JsonValue.Null -> builder.append("null")
        }
    }

    private fun appendPrettyObject(value: JsonValue.Obj, builder: StringBuilder, depth: Int) {
        if (value.entries.isEmpty()) {
            builder.append("{}")
            return
        }

        builder.append("{\n")
        value.entries.forEachIndexed { index, member ->
            appendIndent(builder, depth + 1)
            builder.append(escapeString(member.key))
            builder.append(": ")
            appendPretty(member.value, builder, depth + 1)
            if (index < value.entries.lastIndex) {
                builder.append(',')
            }
            builder.append('\n')
        }
        appendIndent(builder, depth)
        builder.append('}')
    }

    private fun appendPrettyArray(value: JsonValue.Arr, builder: StringBuilder, depth: Int) {
        if (value.values.isEmpty()) {
            builder.append("[]")
            return
        }

        builder.append("[\n")
        value.values.forEachIndexed { index, item ->
            appendIndent(builder, depth + 1)
            appendPretty(item, builder, depth + 1)
            if (index < value.values.lastIndex) {
                builder.append(',')
            }
            builder.append('\n')
        }
        appendIndent(builder, depth)
        builder.append(']')
    }

    private fun appendCompact(value: JsonValue, builder: StringBuilder) {
        when (value) {
            is JsonValue.Obj -> {
                builder.append('{')
                value.entries.forEachIndexed { index, member ->
                    builder.append(escapeString(member.key))
                    builder.append(':')
                    appendCompact(member.value, builder)
                    if (index < value.entries.lastIndex) {
                        builder.append(',')
                    }
                }
                builder.append('}')
            }
            is JsonValue.Arr -> {
                builder.append('[')
                value.values.forEachIndexed { index, item ->
                    appendCompact(item, builder)
                    if (index < value.values.lastIndex) {
                        builder.append(',')
                    }
                }
                builder.append(']')
            }
            is JsonValue.Str -> builder.append(escapeString(value.value))
            is JsonValue.Num -> builder.append(value.raw)
            is JsonValue.Bool -> builder.append(value.value)
            JsonValue.Null -> builder.append("null")
        }
    }

    private fun appendIndent(builder: StringBuilder, depth: Int) {
        repeat(depth) {
            builder.append("  ")
        }
    }
}

private class JsonParseException(message: String) : IllegalArgumentException(message)

private class Parser(private val text: String) {
    private var position = 0

    fun parse(): JsonValue {
        skipWhitespace()
        if (position >= text.length) {
            fail("JSON 内容为空")
        }

        val value = parseValue()
        skipWhitespace()
        if (position != text.length) {
            fail("JSON 末尾存在多余内容")
        }
        return value
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        if (position >= text.length) {
            fail("JSON 值不完整")
        }

        return when (val char = text[position]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.Str(parseString())
            't' -> parseLiteral("true", JsonValue.Bool(true))
            'f' -> parseLiteral("false", JsonValue.Bool(false))
            'n' -> parseLiteral("null", JsonValue.Null)
            '-', in '0'..'9' -> parseNumber()
            else -> fail("位置 $position 处存在非法字符 '$char'")
        }
    }

    private fun parseObject(): JsonValue.Obj {
        expect('{')
        skipWhitespace()
        if (peek() == '}') {
            position++
            return JsonValue.Obj(emptyList())
        }

        val entries = mutableListOf<JsonMember>()
        while (true) {
            skipWhitespace()
            if (peek() != '"') {
                fail("对象 key 必须是字符串")
            }
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            entries.add(JsonMember(key, value))
            skipWhitespace()
            when (peek()) {
                ',' -> position++
                '}' -> {
                    position++
                    return JsonValue.Obj(entries)
                }
                else -> fail("对象成员之间必须使用逗号分隔")
            }
        }
    }

    private fun parseArray(): JsonValue.Arr {
        expect('[')
        skipWhitespace()
        if (peek() == ']') {
            position++
            return JsonValue.Arr(emptyList())
        }

        val values = mutableListOf<JsonValue>()
        while (true) {
            values.add(parseValue())
            skipWhitespace()
            when (peek()) {
                ',' -> position++
                ']' -> {
                    position++
                    return JsonValue.Arr(values)
                }
                else -> fail("数组成员之间必须使用逗号分隔")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val builder = StringBuilder()
        while (position < text.length) {
            val char = text[position++]
            when (char) {
                '"' -> return builder.toString()
                '\\' -> builder.append(parseEscape())
                else -> {
                    if (char.code < 0x20) {
                        fail("字符串中包含非法控制字符")
                    }
                    builder.append(char)
                }
            }
        }
        fail("字符串没有闭合")
    }

    private fun parseEscape(): Char {
        if (position >= text.length) {
            fail("字符串转义不完整")
        }

        return when (val escaped = text[position++]) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> parseUnicodeEscape()
            else -> fail("不支持的字符串转义 \\$escaped")
        }
    }

    private fun parseUnicodeEscape(): Char {
        if (position + 4 > text.length) {
            fail("Unicode 转义不完整")
        }
        val hex = text.substring(position, position + 4)
        if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            fail("Unicode 转义包含非法字符")
        }
        position += 4
        return hex.toInt(16).toChar()
    }

    private fun parseNumber(): JsonValue.Num {
        val start = position
        if (peek() == '-') {
            position++
        }

        when {
            peek() == '0' -> position++
            peek()?.isDigitOneToNine() == true -> {
                position++
                while (peek()?.isDigit() == true) {
                    position++
                }
            }
            else -> fail("数字格式不正确")
        }

        if (peek() == '.') {
            position++
            if (peek()?.isDigit() != true) {
                fail("小数点后必须有数字")
            }
            while (peek()?.isDigit() == true) {
                position++
            }
        }

        if (peek() == 'e' || peek() == 'E') {
            position++
            if (peek() == '+' || peek() == '-') {
                position++
            }
            if (peek()?.isDigit() != true) {
                fail("指数部分必须有数字")
            }
            while (peek()?.isDigit() == true) {
                position++
            }
        }

        return JsonValue.Num(text.substring(start, position))
    }

    private fun parseLiteral(literal: String, value: JsonValue): JsonValue {
        if (!text.startsWith(literal, position)) {
            fail("期望 $literal")
        }
        position += literal.length
        return value
    }

    private fun expect(expected: Char) {
        if (peek() != expected) {
            fail("期望 '$expected'")
        }
        position++
    }

    private fun skipWhitespace() {
        while (peek() == ' ' || peek() == '\n' || peek() == '\r' || peek() == '\t') {
            position++
        }
    }

    private fun peek(): Char? = text.getOrNull(position)

    private fun fail(message: String): Nothing {
        throw JsonParseException("$message，位置 $position")
    }

    private fun Char.isDigit(): Boolean = this in '0'..'9'

    private fun Char.isDigitOneToNine(): Boolean = this in '1'..'9'
}
