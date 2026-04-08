package com.miaobi.app.ui.screens.writing

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miaobi.app.domain.model.WriteStyle

/**
 * 底部输入栏 - 新布局
 * 布局：菱形(AI灵感) | 撤销 | 重做 | [脑洞大开 ▼] | Ai继续
 */
@Composable
fun BottomInputBar(
    isGenerating: Boolean,
    prompt: String,
    onPromptChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onExpandClick: () -> Unit,
    isExpanded: Boolean,
    onDismissExpand: () -> Unit,
    showRewriteStyleRow: Boolean,
    selectedStyle: com.miaobi.app.domain.model.RewriteStyle?,
    onRewrite: () -> Unit,
    onExpand: () -> Unit,
    onShrink: () -> Unit,
    selectedWriteType: WriteType,
    onWriteTypeSelected: (WriteType) -> Unit,
    // 新增：续写风格相关
    selectedWriteStyle: WriteStyle,
    onWriteStyleSelected: (WriteStyle) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // ── AI 功能菜单（菱形按钮点击弹出）────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Column {
                    // 扩写/缩写/改写/定向续写 按钮行
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiMenuButton(
                            text = "扩写",
                            emoji = "↗",
                            isSelected = selectedWriteType == WriteType.EXPAND,
                            onClick = {
                                onWriteTypeSelected(WriteType.EXPAND)
                                onExpand()
                                onDismissExpand()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AiMenuButton(
                            text = "缩写",
                            emoji = "↘",
                            isSelected = selectedWriteType == WriteType.SHRINK,
                            onClick = {
                                onWriteTypeSelected(WriteType.SHRINK)
                                onShrink()
                                onDismissExpand()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AiMenuButton(
                            text = "改写",
                            emoji = "✎",
                            isSelected = selectedWriteType == WriteType.REWRITE,
                            onClick = {
                                onWriteTypeSelected(WriteType.REWRITE)
                                onRewrite()
                                onDismissExpand()
                            },
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFFF7F50) // 橙色
                        )
                        AiMenuButton(
                            text = "定向",
                            emoji = "◎",
                            isSelected = selectedWriteType == WriteType.DIRECTED,
                            onClick = {
                                onWriteTypeSelected(WriteType.DIRECTED)
                                // 定向续写，保持展开状态让用户输入
                            },
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF9370DB) // 紫色
                        )
                    }

                    // 分割线
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f))
                    )

                    // 输入框
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        placeholder = {
                            Text(
                                "输入续写提示词...",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .height(48.dp),
                        singleLine = true,
                        enabled = !isGenerating,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B6B),
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color(0xFFFFF8F8),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // ── 主底部栏 ─────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── 菱形按钮 ──────────────────────────────────────────────
                DiamondButton(
                    isExpanded = isExpanded,
                    onClick = {
                        if (isExpanded) onDismissExpand() else onExpandClick()
                    }
                )

                Spacer(modifier = Modifier.width(4.dp))

                // ── 撤销按钮 ──────────────────────────────────────────────
                Icon(
                    imageVector = Icons.Outlined.Undo,
                    contentDescription = "撤销",
                    tint = if (canUndo) Color(0xFF666666) else Color.LightGray,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canUndo, onClick = onUndo)
                        .padding(6.dp)
                )

                // ── 重做按钮 ──────────────────────────────────────────────
                Icon(
                    imageVector = Icons.Outlined.Redo,
                    contentDescription = "重做",
                    tint = if (canRedo) Color(0xFF666666) else Color.LightGray,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canRedo, onClick = onRedo)
                        .padding(6.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // ── 续写风格下拉选择器 ─────────────────────────────────────
                WriteStyleDropdown(
                    selectedStyle = selectedWriteStyle,
                    onStyleSelected = onWriteStyleSelected,
                    enabled = !isGenerating,
                    modifier = Modifier.weight(1f)
                )

                // ── AI继续按钮 ──────────────────────────────────────────────
                AiContinueButton(
                    onClick = {
                        if (!isGenerating) {
                            onGenerate()
                        }
                    },
                    isGenerating = isGenerating,
                    modifier = Modifier.height(40.dp)
                )
            }
        }
    }
}

// ─── 续写风格下拉选择器 ───────────────────────────────────────────────────
@Composable
private fun WriteStyleDropdown(
    selectedStyle: WriteStyle,
    onStyleSelected: (WriteStyle) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true },
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFFF0F0)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedStyle.emoji,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedStyle.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFF6B6B),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            WriteStyle.entries.forEach { style ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = style.emoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = style.label,
                                    fontWeight = if (style == selectedStyle) FontWeight.Medium else FontWeight.Normal,
                                    color = if (style == selectedStyle) Color(0xFFFF6B6B) else Color.Black
                                )
                                Text(
                                    text = style.description,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    },
                    onClick = {
                        onStyleSelected(style)
                        expanded = false
                    },
                    trailingIcon = {
                        if (style == selectedStyle) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .heightIn(min = 48.dp)
                )
            }
        }
    }
}

// ─── AI菜单按钮 ───────────────────────────────────────────────────────────
@Composable
private fun AiMenuButton(
    text: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFF6B6B)
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) color.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.1f),
        animationSpec = tween(200),
        label = "menu_btn_bg"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji,
            fontSize = 16.sp,
            color = if (isSelected) color else Color.Gray
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) color else Color.Black
        )
    }
}

// ─── 菱形按钮 ─────────────────────────────────────────────────────────────
@Composable
private fun DiamondButton(
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(250),
        label = "diamond_rotation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isExpanded) 1.1f else 1f,
        animationSpec = tween(200),
        label = "diamond_scale"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .rotate(45f)
            .background(
                brush = Brush.linearGradient(
                    colors = if (isExpanded) {
                        listOf(Color(0xFFFF9F43), Color(0xFFFF6B6B))
                    } else {
                        listOf(Color(0xFFFF6B6B), Color(0xFFFF9F43))
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "AI 灵感",
            tint = Color.White,
            modifier = Modifier
                .size(20.dp)
                .rotate(-45f + rotation)
        )
    }
}

// ─── AI继续按钮 ────────────────────────────────────────────────────────────
@Composable
private fun AiContinueButton(
    onClick: () -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isGenerating) 0.95f else 1f,
        animationSpec = tween(150),
        label = "continue_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = if (isGenerating) {
                        listOf(Color.Gray, Color.LightGray)
                    } else {
                        listOf(Color(0xFFFF6B6B), Color(0xFFFF9F43))
                    }
                )
            )
            .clickable(enabled = !isGenerating, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "AI继续",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── 写作类型 ─────────────────────────────────────────────────────────────
enum class WriteType(val label: String, val emoji: String, val description: String) {
    CONTINUE("续写", "▶", "根据上文继续创作"),
    EXPAND("扩写", "↗", "将内容更加丰富"),
    SHRINK("缩写", "↘", "精简内容提炼要点"),
    REWRITE("改写", "✎", "改变风格重新表达"),
    DIRECTED("定向", "◎", "按指定方向续写")
}
