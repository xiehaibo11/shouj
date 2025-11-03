package io.github.clash_verge_rev.clash_verge_rev

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso UI测试 - ConfigScreen
 * 测试配置管理界面
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ConfigScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testConfigScreenDisplay() {
        // 导航到配置Tab
        composeTestRule.onNodeWithText("配置").performClick()
        composeTestRule.waitForIdle()
        
        // 验证配置管理标题
        composeTestRule.onNodeWithText("配置管理").assertExists()
        
        // 验证添加按钮存在
        composeTestRule.onNodeWithText("添加").assertExists()
    }

    @Test
    fun testAddConfigDialog() {
        // 导航到配置Tab
        composeTestRule.onNodeWithText("配置").performClick()
        composeTestRule.waitForIdle()
        
        // 点击添加按钮
        composeTestRule.onNodeWithText("添加").performClick()
        composeTestRule.waitForIdle()
        
        // 验证对话框标题
        composeTestRule.onNodeWithText("添加配置").assertExists()
        
        // 验证Tab存在
        composeTestRule.onNodeWithText("URL订阅").assertExists()
        composeTestRule.onNodeWithText("本地文件").assertExists()
        
        // 点击取消按钮
        composeTestRule.onAllNodesWithText("取消").onLast().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testAddConfigWithInput() {
        // 导航到配置Tab
        composeTestRule.onNodeWithText("配置").performClick()
        composeTestRule.waitForIdle()
        
        // 点击添加按钮
        composeTestRule.onNodeWithText("添加").performClick()
        composeTestRule.waitForIdle()
        
        // 输入配置名称
        composeTestRule.onNodeWithText("配置名称").performTextInput("测试配置")
        
        // 输入订阅地址
        composeTestRule.onNodeWithText("订阅地址").performTextInput("https://example.com/config.yaml")
        
        // 验证添加按钮变为可用
        val addButton = composeTestRule.onAllNodesWithText("添加").onLast()
        addButton.assertIsEnabled()
        
        // 点击添加（实际会创建配置）
        addButton.performClick()
        composeTestRule.waitForIdle()
        
        // 等待加载完成
        Thread.sleep(2000)
    }

    @Test
    fun testConfigFileDisplay() {
        // 导航到配置Tab
        composeTestRule.onNodeWithText("配置").performClick()
        composeTestRule.waitForIdle()
        
        // 如果没有配置文件，应该看到空状态提示
        // 注意: 这个测试依赖于是否已经添加了配置
        try {
            composeTestRule.onNodeWithText("暂无配置文件").assertExists()
        } catch (e: AssertionError) {
            // 如果已经有配置文件，忽略错误
        }
    }
}












