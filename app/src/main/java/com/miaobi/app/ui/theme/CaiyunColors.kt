package com.miaobi.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 彩云小梦风格配色
 */
object CaiyunColors {
    // 主色调
    val Primary = Color(0xFFF5A623)        // 主色 - 橙色
    val PrimaryDark = Color(0xFFD68910)     // 主色深
    val PrimaryLight = Color(0xFFFFB74D)    // 主色浅

    // 背景色
    val Background = Color(0xFFFFFFFF)       // 白色背景
    val Surface = Color(0xFFF5F5F5)        // 浅灰表面
    val BottomBar = Color(0xFFF8F8F8)       // 底部栏背景

    // 文字颜色
    val TextPrimary = Color(0xFF1A1A1A)     // 主要文字 - 深黑
    val TextSecondary = Color(0xFF555555)   // 次要文字 - 灰色 (4.5:1+ contrast)
    val TextHint = Color(0xFF757575)       // 提示文字 - 浅灰 (4.5:1+ contrast)
    val TextWhite = Color(0xFFFFFFFF)       // 白色文字

    // 按钮状态
    val ButtonSelected = Color(0xFFE8E8E8)  // 选中状态 - 深灰
    val ButtonUnselected = Color(0xFFFFFFFF) // 未选中状态 - 白色
    val ButtonDisabled = Color(0xFFCCCCCC)  // 禁用状态 - 浅灰

    // 分割线
    val Divider = Color(0xFFEEEEEE)        // 分割线 - 极浅灰

    // 功能色
    val Success = Color(0xFF4CAF50)        // 成功 - 绿色
    val Error = Color(0xFFE53935)          // 错误 - 红色
    val Warning = Color(0xFFFF9800)        // 警告 - 橙色
    val Info = Color(0xFF2196F3)           // 信息 - 蓝色

    // 编辑器专属
    val EditorBackground = Color(0xFFFFFFFF) // 编辑区背景
    val EditorCursor = Color(0xFFF5A623)    // 光标颜色
    val InputBoxBackground = Color(0xFFF0F0F0) // 输入框背景
}
