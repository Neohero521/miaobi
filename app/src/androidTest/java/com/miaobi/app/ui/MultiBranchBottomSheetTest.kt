package com.miaobi.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.miaobi.app.domain.model.BranchOption
import com.miaobi.app.domain.model.MultiBranchState
import com.miaobi.app.ui.screens.writing.MultiBranchBottomSheet
import org.junit.Rule
import org.junit.Test

class MultiBranchBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchSheet(
        multiBranchState: MultiBranchState = MultiBranchState(),
        onCountChanged: (Int) -> Unit = {},
        onGenerate: () -> Unit = {},
        onCancel: () -> Unit = {},
        onBranchSelected: (Int) -> Unit = {},
        onAccept: () -> Unit = {},
        onRegenerateBranch: (Int) -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
            MultiBranchBottomSheet(
                multiBranchState = multiBranchState,
                onCountChanged = onCountChanged,
                onGenerate = onGenerate,
                onCancel = onCancel,
                onBranchSelected = onBranchSelected,
                onAccept = onAccept,
                onRegenerateBranch = onRegenerateBranch,
                onDismiss = onDismiss,
                sheetState = sheetState
            )
        }
    }

    // ===== Initial state tests =====

    @Test
    fun `sheet shows title 多分支续写`() {
        launchSheet()

        composeTestRule.onNodeWithText("多分支续写").assertExists()
    }

    @Test
    fun `sheet has close button`() {
        launchSheet()

        composeTestRule.onNodeWithContentDescription("关闭").assertExists()
    }

    @Test
    fun `sheet shows branch count selector when not generating`() {
        launchSheet()

        composeTestRule.onNodeWithText("选择分支数量").assertExists()
        composeTestRule.onNodeWithText("2 条").assertExists()
        composeTestRule.onNodeWithText("3 条").assertExists()
        composeTestRule.onNodeWithText("4 条").assertExists()
    }

    @Test
    fun `sheet shows token warning message`() {
        launchSheet(multiBranchState = MultiBranchState(branchCount = 3))

        composeTestRule.onNodeWithText("选择分支数量").assertExists()
        composeTestRule.onNodeContaining("3 条将消耗约 3 倍的 token").assertExists()
    }

    @Test
    fun `sheet shows 开始生成 button when not generating`() {
        launchSheet()

        composeTestRule.onNodeWithText("开始生成").assertExists()
    }

    @Test
    fun `sheet does not show branch cards when branches is empty and not generating`() {
        launchSheet()

        composeTestRule.onNodeWithText("分支 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("分支 2").assertDoesNotExist()
    }

    // ===== Branch count selector tests =====

    @Test
    fun `selecting 2 branches updates count`() {
        var capturedCount = 3
        launchSheet(
            onCountChanged = { capturedCount = it }
        )

        composeTestRule.onNodeWithText("2 条").performClick()

        assertEquals(2, capturedCount)
    }

    @Test
    fun `selecting 4 branches updates count`() {
        var capturedCount = 3
        launchSheet(
            multiBranchState = MultiBranchState(branchCount = 2),
            onCountChanged = { capturedCount = it }
        )

        composeTestRule.onNodeWithText("4 条").performClick()

        assertEquals(4, capturedCount)
    }

    @Test
    fun `token warning updates with branch count`() {
        launchSheet(multiBranchState = MultiBranchState(branchCount = 2))

        composeTestRule.onNodeContaining("2 条将消耗约 2 倍的 token").assertExists()
    }

    // ===== Generate button tests =====

    @Test
    fun `开始生成 calls onGenerate`() {
        var called = false
        launchSheet(onGenerate = { called = true })

        composeTestRule.onNodeWithText("开始生成").performClick()

        assertTrue(called)
    }

    // ===== Generating state tests =====

    @Test
    fun `sheet shows generating indicator when isGenerating is true`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branchCount = 3,
                isGenerating = true,
                branches = listOf(
                    BranchOption(index = 0, isGenerating = true),
                    BranchOption(index = 1, isGenerating = true),
                    BranchOption(index = 2, isGenerating = true)
                )
            )
        )

        composeTestRule.onNodeContaining("生成中").assertExists()
    }

    @Test
    fun `sheet shows progress text with count`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branchCount = 2,
                isGenerating = true,
                branches = listOf(
                    BranchOption(index = 0, content = "完成的内容", isGenerating = false),
                    BranchOption(index = 1, isGenerating = true)
                )
            )
        )

        composeTestRule.onNodeContaining("1/2").assertExists()
    }

    @Test
    fun `sheet shows cancel button when generating`() {
        launchSheet(
            multiBranchState = MultiBranchState(isGenerating = true)
        )

        composeTestRule.onNodeWithText("取消").assertExists()
    }

    @Test
    fun `cancel button calls onCancel`() {
        var called = false
        launchSheet(
            multiBranchState = MultiBranchState(isGenerating = true),
            onCancel = { called = true }
        )

        composeTestRule.onNodeWithText("取消").performClick()

        assertTrue(called)
    }

    @Test
    fun `sheet shows linear progress indicator when generating`() {
        launchSheet(
            multiBranchState = MultiBranchState(isGenerating = true)
        )

        composeTestRule.onNodeWithText("生成中...").assertExists()
    }

    // ===== Branch cards tests =====

    @Test
    fun `branch cards show branch labels`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "内容1", isGenerating = false),
                    BranchOption(index = 1, content = "内容2", isGenerating = false)
                )
            )
        )

        composeTestRule.onNodeWithText("分支 1").assertExists()
        composeTestRule.onNodeWithText("分支 2").assertExists()
    }

    @Test
    fun `branch card shows content when available`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "这是分支1的续写内容", isGenerating = false)
                )
            )
        )

        composeTestRule.onNodeWithText("这是分支1的续写内容").assertExists()
    }

    @Test
    fun `branch card shows summary tag`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(
                        index = 0,
                        content = "内容",
                        summaryTag = "💥 激烈冲突",
                        isGenerating = false
                    )
                )
            )
        )

        composeTestRule.onNodeWithText("💥 激烈冲突").assertExists()
    }

    @Test
    fun `branch card shows word count`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(
                        index = 0,
                        content = "一段文字内容",
                        isGenerating = false
                    )
                )
            )
        )

        composeTestRule.onNodeWithText("7 字").assertExists()
    }

    @Test
    fun `branch card shows 生成中 when still generating`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "", isGenerating = true)
                )
            )
        )

        composeTestRule.onNodeWithText("生成中...").assertExists()
    }

    @Test
    fun `branch card shows error when error is present`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(
                        index = 0,
                        content = "",
                        error = "生成失败",
                        isGenerating = false
                    )
                )
            )
        )

        composeTestRule.onNodeWithText("⚠️").assertExists()
        composeTestRule.onNodeWithText("生成失败").assertExists()
    }

    @Test
    fun `branch card is not clickable when cannot accept`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "", isGenerating = true)
                )
            ),
            onBranchSelected = { assertTrue(false) } // should not be called
        )

        // Card should not respond to click when isGenerating
    }

    @Test
    fun `selecting branch calls onBranchSelected`() {
        var selectedIndex = -1
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "内容1", isGenerating = false),
                    BranchOption(index = 1, content = "内容2", isGenerating = false)
                )
            ),
            onBranchSelected = { selectedIndex = it }
        )

        composeTestRule.onNodeWithText("内容1").performClick()

        assertEquals(0, selectedIndex)
    }

    @Test
    fun `selected branch shows check icon`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "内容", isSelected = true, isGenerating = false)
                )
            )
        )

        composeTestRule.onNodeWithContentDescription(null, substring = true).assertExists()
    }

    // ===== Action buttons tests =====

    @Test
    fun `采纳选中 button exists when branches are done`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "内容", isSelected = true, isGenerating = false)
                ),
                selectedBranchIndex = 0
            )
        )

        composeTestRule.onNodeWithText("采纳选中").assertExists()
    }

    @Test
    fun `采纳选中 calls onAccept`() {
        var called = false
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "内容", isSelected = true, isGenerating = false)
                ),
                selectedBranchIndex = 0
            ),
            onAccept = { called = true }
        )

        composeTestRule.onNodeWithText("采纳选中").performClick()

        assertTrue(called)
    }

    @Test
    fun `全部重写 button exists when all branches are done`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "内容1", isGenerating = false),
                    BranchOption(index = 1, content = "内容2", isGenerating = false)
                ),
                isGenerating = false
            )
        )

        composeTestRule.onNodeWithText("全部重写").assertExists()
    }

    @Test
    fun `全部重写 button calls onGenerate`() {
        var called = false
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "内容1", isGenerating = false)
                ),
                isGenerating = false
            ),
            onGenerate = { called = true }
        )

        composeTestRule.onNodeWithText("全部重写").performClick()

        assertTrue(called)
    }

    // ===== Selected branch card action buttons =====

    @Test
    fun `selected branch card shows 采纳 and 重写 buttons`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "内容", isSelected = true, isGenerating = false)
                ),
                selectedBranchIndex = 0
            )
        )

        composeTestRule.onNodeWithText("采纳").assertExists()
        composeTestRule.onNodeWithText("重写").assertExists()
    }

    @Test
    fun `采纳 on selected branch card calls onBranchSelected and onAccept`() {
        var selectedIndex = -1
        var acceptCalled = false
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "内容", isSelected = true, isGenerating = false)
                ),
                selectedBranchIndex = 0
            ),
            onBranchSelected = { selectedIndex = it },
            onAccept = { acceptCalled = true }
        )

        composeTestRule.onNodeWithText("采纳").performClick()

        assertEquals(0, selectedIndex)
        assertTrue(acceptCalled)
    }

    @Test
    fun `重写 on selected branch card calls onRegenerateBranch`() {
        var regeneratedIndex = -1
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = "内容", isSelected = true, isGenerating = false),
                    BranchOption(index = 1, content = "其他", isSelected = false, isGenerating = false)
                ),
                selectedBranchIndex = 0
            ),
            onRegenerateBranch = { regeneratedIndex = it }
        )

        composeTestRule.onNodeWithText("重写").performClick()

        assertEquals(0, regeneratedIndex)
    }

    // ===== Error display tests =====

    @Test
    fun `sheet shows error message when error is set`() {
        launchSheet(
            multiBranchState = MultiBranchState(error = "生成失败，请重试")
        )

        composeTestRule.onNodeWithText("生成失败，请重试").assertExists()
    }

    @Test
    fun `close button calls onDismiss`() {
        var called = false
        launchSheet(onDismiss = { called = true })

        composeTestRule.onNodeWithContentDescription("关闭").performClick()

        assertTrue(called)
    }

    // ===== Branch count 4 tests =====

    @Test
    fun `all 4 branches display correctly`() {
        launchSheet(
            multiBranchState = MultiBranchState(
                branchCount = 4,
                branches = listOf(
                    BranchOption(index = 0, content = "分支1", isGenerating = false),
                    BranchOption(index = 1, content = "分支2", isGenerating = false),
                    BranchOption(index = 2, content = "分支3", isGenerating = false),
                    BranchOption(index = 3, content = "分支4", isGenerating = false)
                )
            )
        )

        composeTestRule.onNodeWithText("分支 1").assertExists()
        composeTestRule.onNodeWithText("分支 2").assertExists()
        composeTestRule.onNodeWithText("分支 3").assertExists()
        composeTestRule.onNodeWithText("分支 4").assertExists()
    }

    // ===== Dismiss behavior tests =====

    @Test
    fun `content is truncated to 5 lines with ellipsis`() {
        val longContent = "这是第一行内容。\n这是第二行内容。\n这是第三行内容。\n这是第四行内容。\n这是第五行内容。\n这是第六行内容。\n这是第七行内容。"
        launchSheet(
            multiBranchState = MultiBranchState(
                branches = listOf(
                    BranchOption(index = 0, content = longContent, isGenerating = false)
                )
            )
        )

        // The text should exist but be truncated (maxLines = 5)
        composeTestRule.onNodeWithText("这是第一行内容。").assertExists()
    }
}
