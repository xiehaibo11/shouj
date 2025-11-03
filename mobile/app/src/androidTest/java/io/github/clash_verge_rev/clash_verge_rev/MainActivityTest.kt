package io.github.clash_verge_rev.clash_verge_rev

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso UI测试 - MainActivity
 * 测试主界面的UI元素和交互
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAppLaunches() {
        // 验证应用启动
        composeTestRule.waitForIdle()
        
        // 验证顶部AppBar显示应用名称
        composeTestRule.onNodeWithText("Clash Verge Rev").assertExists()
    }

    @Test
    fun testBottomNavigationExists() {
        // 验证底部导航栏存在
        composeTestRule.onNodeWithText("主页").assertExists()
        composeTestRule.onNodeWithText("节点").assertExists()
        composeTestRule.onNodeWithText("配置").assertExists()
        composeTestRule.onNodeWithText("日志").assertExists()
    }

    @Test
    fun testBottomNavigationSwitching() {
        // 测试切换到节点Tab
        composeTestRule.onNodeWithText("节点").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("代理节点").assertExists()
        
        // 测试切换到配置Tab
        composeTestRule.onNodeWithText("配置").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("配置管理").assertExists()
        
        // 测试切换到日志Tab
        composeTestRule.onNodeWithText("日志").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("日志").assertExists()
        
        // 切换回主页
        composeTestRule.onNodeWithText("主页").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testVpnConnectionButton() {
        // 验证VPN连接按钮存在
        // 注意: 在测试环境中，实际的VPN连接会失败，这里只测试UI元素
        composeTestRule.onNodeWithText("未连接").assertExists()
        
        // 可以验证连接按钮存在但不实际点击（因为需要VPN权限）
        composeTestRule.onNodeWithText("点击连接").assertExists()
    }

    @Test
    fun testTopMenuButton() {
        // 点击更多菜单
        composeTestRule.onNodeWithContentDescription("更多").performClick()
        composeTestRule.waitForIdle()
        
        // 验证菜单项显示
        composeTestRule.onNodeWithText("设置").assertExists()
        composeTestRule.onNodeWithText("关于").assertExists()
    }
}












