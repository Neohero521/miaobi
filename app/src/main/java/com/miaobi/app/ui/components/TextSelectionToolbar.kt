package com.miaobi.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 文字选中后出现的浮动工具栏
 * 提供：改写 / 润色 一键操作
 * 选中文本后自动弹出，点击外部自动消失
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
        enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { it / 2 },
        exit = fadeOut(tween(100)) + slideOutVertically(tween(100)) { it / 2 },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 选中字数提示
                Text(
                    text = "${selectedText.length}字",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.2f))
                )

                // 改写按钮
                ToolbarActionButton(
                    icon = Icons.Default.Refresh,
                    label = "改写",
                    onClick = onRewrite
                )

                // 润色按钮
                ToolbarActionButton(
                    icon = Icons.Default.Edit,
                    label = "润色",
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
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
