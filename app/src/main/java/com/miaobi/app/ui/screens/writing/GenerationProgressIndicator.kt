package com.miaobi.app.ui.screens.writing

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AI生成进度指示器
 * 显示正在生成的动画和进度
 */
@Composable
fun GenerationProgressIndicator(
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isGenerating,
        enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 动画圆点
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "dots")
                    listOf(0, 1, 2).forEach { index ->
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, delayMillis = index * 200),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(0xFFFF6B6B).copy(alpha = alpha))
                        )
                    }
                }
                
                // 文字
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI 正在创作中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "请稍候，内容将自动添加",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                // 取消按钮
                TextButton(
                    onClick = { /* Cancel generation */ },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text("取消", fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * 简洁的生成状态指示器
 */
@Composable
fun GenerationStatusBadge(
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isGenerating,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF0F0)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "badge_dots")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "badge_alpha"
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color(0xFFFF6B6B).copy(alpha = alpha))
                )
                Text(
                    text = "创作中",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
