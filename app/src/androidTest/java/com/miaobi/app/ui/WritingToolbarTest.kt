package com.miaobi.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.miaobi.app.ui.screens.writing.WritingToolbar
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WritingToolbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchToolbar(
        canUndo: Boolean = true,
        canRedo: Boolean = true,
        onUndo: () -> Unit = {},
        onRedo: () -> Unit = {},
        onPolish: () -> Unit = {},
        onRewrite: () -> Unit = {},
        onInspiration: () -> Unit = {},
        onSave: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            WritingToolbar(
                onUndo = onUndo,
                onRedo = onRedo,
                onPolish = onPolish,
                onRewrite = onRewrite,
                onInspiration = onInspiration,
                onSave = onSave,
                canUndo = canUndo,
                canRedo = canRedo
            )
        }
    }

    // ===== Button visibility tests =====

    @Test
    fun `toolbar displays undo button`() {
        launchToolbar()
        composeTestRule.onNodeWithContentDescription("撤回").assertExists()
    }

    @Test
    fun `toolbar displays redo button`() {
        launchToolbar()
        composeTestRule.onNodeWithContentDescription("重做").assertExists()
    }

    @Test
    fun `toolbar displays 润色 button`() {
        launchToolbar()
        composeTestRule.onNodeWithText("润色").assertExists()
    }

    @Test
    fun `toolbar displays 改写 button`() {
        launchToolbar()
        composeTestRule.onNodeWithText("改写").assertExists()
    }

    @Test
    fun `toolbar displays 灵感 button`() {
        launchToolbar()
        composeTestRule.onNodeWithText("灵感").assertExists()
    }

    @Test
    fun `toolbar displays 保存 button`() {
        launchToolbar()
        composeTestRule.onNodeWithText("保存").assertExists()
    }

    // ===== Undo button tests =====

    @Test
    fun `undo button is enabled when canUndo is true`() {
        launchToolbar(canUndo = true)
        composeTestRule.onNodeWithContentDescription("撤回").assertIsEnabled()
    }

    @Test
    fun `undo button is disabled when canUndo is false`() {
        launchToolbar(canUndo = false)
        composeTestRule.onNodeWithContentDescription("撤回").assertIsNotEnabled()
    }

    @Test
    fun `undo button calls onUndo when clicked`() {
        var called = false
        launchToolbar(onUndo = { called = true })
        composeTestRule.onNodeWithContentDescription("撤回").performClick()
        assertTrue(called)
    }

    // ===== Redo button tests =====

    @Test
    fun `redo button is enabled when canRedo is true`() {
        launchToolbar(canRedo = true)
        composeTestRule.onNodeWithContentDescription("重做").assertIsEnabled()
    }

    @Test
    fun `redo button is disabled when canRedo is false`() {
        launchToolbar(canRedo = false)
        composeTestRule.onNodeWithContentDescription("重做").assertIsNotEnabled()
    }

    @Test
    fun `redo button calls onRedo when clicked`() {
        var called = false
        launchToolbar(onRedo = { called = true })
        composeTestRule.onNodeWithContentDescription("重做").performClick()
        assertTrue(called)
    }

    // ===== Polish button tests =====

    @Test
    fun `polish button calls onPolish when clicked`() {
        var called = false
        launchToolbar(onPolish = { called = true })
        composeTestRule.onNodeWithText("润色").performClick()
        assertTrue(called)
    }

    @Test
    fun `polish button is always enabled`() {
        launchToolbar()
        composeTestRule.onNodeWithText("润色").assertIsEnabled()
    }

    // ===== Rewrite button tests =====

    @Test
    fun `rewrite button calls onRewrite when clicked`() {
        var called = false
        launchToolbar(onRewrite = { called = true })
        composeTestRule.onNodeWithText("改写").performClick()
        assertTrue(called)
    }

    @Test
    fun `rewrite button is always enabled`() {
        launchToolbar()
        composeTestRule.onNodeWithText("改写").assertIsEnabled()
    }

    // ===== Inspiration button tests =====

    @Test
    fun `inspiration button calls onInspiration when clicked`() {
        var called = false
        launchToolbar(onInspiration = { called = true })
        composeTestRule.onNodeWithText("灵感").performClick()
        assertTrue(called)
    }

    @Test
    fun `inspiration button is always enabled`() {
        launchToolbar()
        composeTestRule.onNodeWithText("灵感").assertIsEnabled()
    }

    // ===== Save button tests =====

    @Test
    fun `save button calls onSave when clicked`() {
        var called = false
        launchToolbar(onSave = { called = true })
        composeTestRule.onNodeWithText("保存").performClick()
        assertTrue(called)
    }

    @Test
    fun `save button is always enabled`() {
        launchToolbar()
        composeTestRule.onNodeWithText("保存").assertIsEnabled()
    }

    // ===== Combined interaction tests =====

    @Test
    fun `all buttons can be clicked in sequence`() {
        var undoCalls = 0
        var redoCalls = 0
        var polishCalls = 0
        var rewriteCalls = 0
        var inspirationCalls = 0
        var saveCalls = 0

        launchToolbar(
            canUndo = true,
            canRedo = true,
            onUndo = { undoCalls++ },
            onRedo = { redoCalls++ },
            onPolish = { polishCalls++ },
            onRewrite = { rewriteCalls++ },
            onInspiration = { inspirationCalls++ },
            onSave = { saveCalls++ }
        )

        composeTestRule.onNodeWithContentDescription("撤回").performClick()
        composeTestRule.onNodeWithContentDescription("重做").performClick()
        composeTestRule.onNodeWithText("润色").performClick()
        composeTestRule.onNodeWithText("改写").performClick()
        composeTestRule.onNodeWithText("灵感").performClick()
        composeTestRule.onNodeWithText("保存").performClick()

        assertEquals(1, undoCalls)
        assertEquals(1, redoCalls)
        assertEquals(1, polishCalls)
        assertEquals(1, rewriteCalls)
        assertEquals(1, inspirationCalls)
        assertEquals(1, saveCalls)
    }

    // ===== Disabled state visual tests =====

    @Test
    fun `undo and redo both disabled when stack is empty`() {
        launchToolbar(canUndo = false, canRedo = false)
        composeTestRule.onNodeWithContentDescription("撤回").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("重做").assertIsNotEnabled()
    }

    @Test
    fun `only undo enabled when redo stack is empty`() {
        launchToolbar(canUndo = true, canRedo = false)
        composeTestRule.onNodeWithContentDescription("撤回").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("重做").assertIsNotEnabled()
    }

    @Test
    fun `only redo enabled when undo stack is empty`() {
        launchToolbar(canUndo = false, canRedo = true)
        composeTestRule.onNodeWithContentDescription("撤回").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("重做").assertIsEnabled()
    }
}
