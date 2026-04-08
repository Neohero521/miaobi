package com.miaobi.app.ui.screens.writing

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miaobi.app.domain.model.LengthOption
import com.miaobi.app.ui.theme.*

/**
 * 彩云小梦风格编辑面板组件
 * 集成到现有 WritingScreen 中使用
 */
@Composable
fun CaiyunEditorPanel(
    // 编辑状态
    editorText: String,
    userPrompt: String,
    lengthOption: LengthOption,
    isGenerating: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    
    // 回调
    onContentChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onLengthChange: (LengthOption) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPolish: () -> Unit,
    onRewrite: () -> Unit,
    onInspiration: () -> Unit,
    onCharacter: () -> Unit,
    onWorld: () -> Unit,
    onHistory: () -> Unit,
    onBranch: () -> Unit,
    onSave: () -> Unit,
    onContinue: () -> Unit,
    
    // 额外回调
    onCharacterClick: () -> Unit = {},
    onWorldClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onBranchClick: () -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CaiyunColors.BottomBar)
    ) {
        // ── 第一行：撤销/重做 + 功能Tab栏 ───────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CaiyunDimens.spacing_lg)
                .padding(top = CaiyunDimens.spacing_md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 撤销/重做按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Undo,
                    contentDescription = "撤销",
                    tint = if (canUndo) CaiyunColors.TextSecondary else CaiyunColors.TextHint,
                    modifier = Modifier
                        .size(CaiyunDimens.icon_size_lg)
                        .clickable(enabled = canUndo) { onUndo() }
                )
                Spacer(modifier = Modifier.width(CaiyunDimens.spacing_xl))
                Icon(
                    imageVector = Icons.Outlined.Redo,
                    contentDescription = "重做",
                    tint = if (canRedo) CaiyunColors.TextSecondary else CaiyunColors.TextHint,
                    modifier = Modifier
                        .size(CaiyunDimens.icon_size_lg)
                        .clickable(enabled = canRedo) { onRedo() }
                )
            }

            // 功能Tab栏：润色、改写、灵感、人物、世界
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorTabItem(
                    icon = Icons.Outlined.AutoFixHigh,
                    text = "润色",
                    onClick = onPolish
                )
                EditorTabItem(
                    icon = Icons.Outlined.Edit,
                    text = "改写",
                    onClick = onRewrite
                )
                EditorTabItem(
                    icon = Icons.Default.Lightbulb,
                    text = "灵感",
                    onClick = onInspiration
                )
                EditorTabItem(
                    icon = Icons.Default.Person,
                    text = "人物",
                    onClick = onCharacterClick
                )
                EditorTabItem(
                    icon = Icons.Default.Book,
                    text = "世界",
                    onClick = onWorldClick
                )
            }
        }

        // ── 分割线 ─────────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(CaiyunDimens.spacing_sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(CaiyunColors.Divider)
        )
        Spacer(modifier = Modifier.height(CaiyunDimens.spacing_sm))

        // ── AI续写方向输入框 ─────────────────────────────────────────────
        OutlinedTextField(
            value = userPrompt,
            onValueChange = onPromptChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CaiyunDimens.spacing_lg)
                .clip(RoundedCornerShape(CaiyunDimens.radius_md)),
            placeholder = {
                Text(
                    text = "AI续写方向（可选）",
                    color = CaiyunColors.TextHint
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CaiyunColors.Primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = CaiyunColors.InputBoxBackground,
                unfocusedContainerColor = CaiyunColors.InputBoxBackground,
                focusedTextColor = CaiyunColors.TextPrimary,
                unfocusedTextColor = CaiyunColors.TextPrimary
            ),
            maxLines = 2,
            singleLine = false
        )

        Spacer(modifier = Modifier.height(CaiyunDimens.spacing_md))

        // ── 续写长度选择：短、中、长 ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CaiyunDimens.spacing_lg),
            horizontalArrangement = Arrangement.spacedBy(CaiyunDimens.spacing_md)
        ) {
            LengthBtnItem(
                text = "短",
                isSelected = lengthOption == LengthOption.SHORT,
                onClick = { onLengthChange(LengthOption.SHORT) },
                modifier = Modifier.weight(1f)
            )
            LengthBtnItem(
                text = "中",
                isSelected = lengthOption == LengthOption.MEDIUM,
                onClick = { onLengthChange(LengthOption.MEDIUM) },
                modifier = Modifier.weight(1f)
            )
            LengthBtnItem(
                text = "长",
                isSelected = lengthOption == LengthOption.LONG,
                onClick = { onLengthChange(LengthOption.LONG) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(CaiyunDimens.spacing_md))

        // ── 底部操作栏：历史、多分支、保存、续写 ─────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CaiyunDimens.spacing_lg)
                .padding(bottom = CaiyunDimens.spacing_lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CaiyunDimens.spacing_sm)
        ) {
            BottomOptBtnItem(
                icon = Icons.Default.History,
                text = "历史",
                onClick = onHistoryClick
            )
            BottomOptBtnItem(
                icon = Icons.Default.AccountTree,
                text = "多分支",
                onClick = onBranchClick
            )
            BottomOptBtnItem(
                icon = Icons.Default.Save,
                text = "保存",
                onClick = onSaveClick
            )

            // 主续写按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(CaiyunDimens.button_height_lg)
                    .clip(RoundedCornerShape(CaiyunDimens.radius_lg))
                    .background(CaiyunColors.Primary)
                    .clickable {
                        if (!isGenerating) {
                            onContinue()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoFixHigh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "续写",
                            color = Color.White,
                            fontSize = CaiyunDimens.text_size_lg,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ─── 子组件 ───────────────────────────────────────────────────────────────────

@Composable
private fun EditorTabItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = CaiyunColors.TextSecondary,
            modifier = Modifier.size(CaiyunDimens.icon_size_md)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = CaiyunColors.TextSecondary,
            fontSize = CaiyunDimens.text_size_md
        )
    }
}

@Composable
private fun LengthBtnItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(CaiyunDimens.button_height_md)
            .clip(RoundedCornerShape(CaiyunDimens.radius_md))
            .background(if (isSelected) CaiyunColors.ButtonSelected else CaiyunColors.ButtonUnselected)
            .border(
                width = 0.5.dp,
                color = if (isSelected) Color.Transparent else CaiyunColors.Divider,
                shape = RoundedCornerShape(CaiyunDimens.radius_md)
            )
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(CaiyunDimens.radius_md)
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = CaiyunColors.TextPrimary,
            fontSize = CaiyunDimens.text_size_lg,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun BottomOptBtnItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(CaiyunDimens.radius_md))
            .clickable { onClick() }
            .padding(horizontal = CaiyunDimens.spacing_md, vertical = CaiyunDimens.spacing_sm)
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = CaiyunColors.TextSecondary,
            modifier = Modifier.size(CaiyunDimens.icon_size_sm)
        )
        Spacer(modifier = Modifier.width(CaiyunDimens.spacing_xs))
        Text(
            text = text,
            color = CaiyunColors.TextSecondary,
            fontSize = CaiyunDimens.text_size_sm
        )
    }
}
