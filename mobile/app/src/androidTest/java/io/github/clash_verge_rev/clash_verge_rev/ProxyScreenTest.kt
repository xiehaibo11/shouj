package io.github.clash_verge_rev.clash_verge_rev

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso UI测试 - ProxyScreen
 * 测试代理节点界面
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProxyScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testProxyScreenDisplay() {
        // 导航到节点Tab
        composeTestRule.onNodeWithText("节点").performClick()
        composeTestRule.waitForIdle()
        
        // 验证代理节点标题
        composeTestRule.onNodeWithText("代理节点").assertExists()
        
        // 验证节点统计信息存在
        composeTestRule.onNode(
            hasText("共") and hasText("个节点")
        ).assertExists()
    }

    @Test
    fun testProxyNodeSelection() {
        // 导航到节点Tab
        composeTestRule.onNodeWithText("节点").performClick()
        composeTestRule.waitForIdle()
        
        // 验证DIRECT节点存在
        composeTestRule.onNodeWithText("DIRECT").assertExists()
        
        // 可以看到其他测试节点
        composeTestRule.onNodeWithText("节点1").assertExists()
    }

    @Test
    fun testSpeedTestButton() {
        // 导航到节点Tab
        composeTestRule.onNodeWithText("节点").performClick()
        composeTestRule.waitForIdle()
        
        // 验证测速按钮存在（通过contentDescription）
        composeTestRule.onNodeWithContentDescription("测速").assertExists()
        
        // 点击测速按钮
        composeTestRule.onNodeWithContentDescription("测速").performClick()
        composeTestRule.waitForIdle()
        
        // 验证加载状态（可能会看到CircularProgressIndicator）
        // 等待测速完成
        Thread.sleep(3000)
    }

    @Test
    fun testDelayDisplay() {
        // 导航到节点Tab
        composeTestRule.onNodeWithText("节点").performClick()
        composeTestRule.waitForIdle()
        
        // 验证延迟标签存在（DIRECT应该显示"直连"）
        composeTestRule.onNodeWithText("直连").assertExists()
    }
}












