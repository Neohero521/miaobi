package com.miaobi.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 打字机效果工具集
 */

// ─── 常量 ────────────────────────────────────────────────────────────────────
private const val DEFAULT_CHAR_DELAY_MS = 28L   // 默认每字延迟
private const val PUNCTUATION_DELAY_MS = 180L  // 标点符号停顿
private const val NEWLINE_DELAY_MS = 120L      // 换行符停顿

private val PUNCTUATION_CHARS = setOf('。', '，', '！', '？', '、', '；', '：', '"', '"', '」', '』', '…', '.', ',', '!', '?', ';', ':')

/**
 * 计算每个字符的延迟（标点更长）
 */
private fun charDelay(c: Char): Long = when {
    c == '\n' -> NEWLINE_DELAY_MS
    c in PUNCTUATION_CHARS -> PUNCTUATION_DELAY_MS
    else -> DEFAULT_CHAR_DELAY_MS
}

/**
 * 打字机逐字动画 Text
 * - text: 要显示的完整文本
 * - charDelayMs: 每字符延迟（默认 28ms）
 * - cursorColor: 光标颜色（默认 primary）
 * - style: 文字样式
 */
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    charDelayMs: Long = DEFAULT_CHAR_DELAY_MS,
    cursorColor: Color = MaterialTheme.colorScheme.primary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onComplete: () -> Unit = {}
) {
    var displayedCharCount by remember(text) { mutableIntStateOf(0) }
    val totalChars = text.length

    // Track completion
    LaunchedEffect(text) {
        displayedCharCount = 0
        for (i in text.indices) {
            delay(charDelay(text[i]))
            displayedCharCount = i + 1
        }
        onComplete()
    }

    val annotatedText = buildAnnotatedString {
        if (displayedCharCount > 0) {
            append(text.substring(0, displayedCharCount))
        }
    }

    Box(modifier = modifier) {
        Text(
            text = annotatedText,
            maxLines = maxLines,
            overflow = overflow,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 17.sp,
                lineHeight = 28.sp
            )
        )

        // Cursor at end of current text
        if (displayedCharCount < totalChars) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = cursorPixelOffset(text.substring(0, displayedCharCount)))
                    .width(2.dp)
                    .height((MaterialTheme.typography.bodyLarge.fontSize.value * 1.4).dp)
                    .background(cursorColor)
            )
        }
    }
}

/**
 * 打字机光标 + 闪烁动画
 */
@Composable
fun TypewriterCursor(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 2.dp,
    height: Dp = 20.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .background(color.copy(alpha = alpha))
    )
}

/**
 * 带打字机效果的 AI 生成文本展示
 * 完成后显示"完成"标记
 */
@Composable
fun TypewriterGeneratedText(
    text: String,
    modifier: Modifier = Modifier,
    charDelayMs: Long = DEFAULT_CHAR_DELAY_MS,
    showCursor: Boolean = true,
    onComplete: () -> Unit = {}
) {
    var displayedCharCount by remember(text) { mutableIntStateOf(0) }
    val totalChars = text.length
    var isComplete by remember { mutableStateOf(false) }

    LaunchedEffect(text) {
        displayedCharCount = 0
        isComplete = false
        for (i in text.indices) {
            delay(charDelay(text[i]))
            displayedCharCount = i + 1
        }
        isComplete = true
        onComplete()
    }

    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = text.substring(0, displayedCharCount.coerceAtMost(totalChars)),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 17.sp,
                    lineHeight = 28.sp
                )
            )

            if (showCursor && !isComplete && displayedCharCount < totalChars) {
                TypewriterCursor(
                    modifier = Modifier.offset(x = cursorXPx(text.substring(0, displayedCharCount))),
                    color = MaterialTheme.colorScheme.primary,
                    height = (17.sp.value * 1.4f).dp
                )
            }
        }

        if (isComplete) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "✓ 完成",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 计算当前光标水平偏移（像素估算，用于等宽字体）
 * 假设每个字符宽度 ≈ fontSize * 0.6
 */
private fun cursorPixelOffset(text: String): Dp {
    // Count visible chars (accounting for newlines wrapping)
    val chars = text.count { it != '\n' }
    // Simple monospace: each char ≈ 60% of fontSize
    val pxPerChar = 17f * 0.6f
    return (chars * pxPerChar).dp
}

/** 同上，纯 px Int 版本（内部用） */
private fun cursorXPx(text: String): Dp = cursorPixelOffset(text)

/**
 * "打字中" 状态指示器
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val delay1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )
    val delay2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )
    val delay3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val dot = @Composable { alpha: Float ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color.copy(alpha = alpha), shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
        dot(delay1)
        dot(delay2)
        dot(delay3)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "AI 写作中...",
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
