package io.github.clash_verge_rev.clash_verge_rev

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso UI测试 - LogScreen
 * 测试日志界面
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LogScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testLogScreenDisplay() {
        // 导航到日志Tab
        composeTestRule.onNodeWithText("日志").performClick()
        composeTestRule.waitForIdle()
        
        // 验证日志标题存在
        composeTestRule.onAllNodesWithText("日志").onFirst().assertExists()
        
        // 验证自动滚动开关存在
        composeTestRule.onNodeWithText("自动滚动").assertExists()
    }

    @Test
    fun testLogFilterButton() {
        // 导航到日志Tab
        composeTestRule.onNodeWithText("日志").performClick()
        composeTestRule.waitForIdle()
        
        // 点击筛选按钮
        composeTestRule.onNodeWithContentDescription("筛选").performClick()
        composeTestRule.waitForIdle()
        
        // 验证筛选选项
        composeTestRule.onNodeWithText("ALL").assertExists()
        composeTestRule.onNodeWithText("ERROR").assertExists()
        composeTestRule.onNodeWithText("WARN").assertExists()
        composeTestRule.onNodeWithText("INFO").assertExists()
        composeTestRule.onNodeWithText("DEBUG").assertExists()
        
        // 选择ERROR级别
        composeTestRule.onNodeWithText("ERROR").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testLogClearButton() {
        // 导航到日志Tab
        composeTestRule.onNodeWithText("日志").performClick()
        composeTestRule.waitForIdle()
        
        // 等待一些日志生成
        Thread.sleep(6000)
        
        // 点击清空按钮
        composeTestRule.onNodeWithContentDescription("清空").performClick()
        composeTestRule.waitForIdle()
        
        // 验证空状态
        composeTestRule.onNodeWithText("暂无日志").assertExists()
    }

    @Test
    fun testAutoScrollSwitch() {
        // 导航到日志Tab
        composeTestRule.onNodeWithText("日志").performClick()
        composeTestRule.waitForIdle()
        
        // 找到自动滚动开关并切换
        // 注意: Switch可能需要通过父节点或其他方式定位
        composeTestRule.onNodeWithText("自动滚动").assertExists()
    }

    @Test
    fun testLogEntryDisplay() {
        // 导航到日志Tab
        composeTestRule.onNodeWithText("日志").performClick()
        composeTestRule.waitForIdle()
        
        // 验证初始日志存在
        composeTestRule.onNodeWithText("Core initialized").assertExists()
        composeTestRule.onNodeWithText("Config loaded successfully").assertExists()
        composeTestRule.onNodeWithText("TUN device started").assertExists()
    }
}


