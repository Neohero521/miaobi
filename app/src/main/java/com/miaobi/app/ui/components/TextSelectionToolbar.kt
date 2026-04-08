package com.miaobi.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 文字选中后出现的浮动工具栏 - 迭代版
 * 提供：改写 / 润色 一键操作
 * 改进了视觉效果和动画
 */
@Composable
fun TextSelectionToolbar(
    visible: Boolean,
    selectedText: String,
    onRewrite: () -> Unit,
    onPolish: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it / 4 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 4 },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.98f),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 选中字数提示
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF0F0)
                ) {
                    Text(
                        text = "${selectedText.length}字",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )

                // 改写按钮
                ToolbarActionButton(
                    icon = Icons.Default.Refresh,
                    label = "改写",
                    color = Color(0xFFFF6B6B),
                    onClick = onRewrite
                )

                // 润色按钮
                ToolbarActionButton(
                    icon = Icons.Default.Edit,
                    label = "润色",
                    color = Color(0xFFFF9F43),
                    onClick = onPolish
                )

                // 关闭按钮
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = color
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
