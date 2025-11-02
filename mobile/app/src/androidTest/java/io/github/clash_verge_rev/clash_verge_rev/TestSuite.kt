package io.github.clash_verge_rev.clash_verge_rev

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * 测试套件 - 运行所有UI测试
 * 
 * 运行命令:
 * ./gradlew connectedAndroidTest
 * 
 * 或运行特定测试套件:
 * ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.clash_verge_rev.clash_verge_rev.TestSuite
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    MainActivityTest::class,
    ProxyScreenTest::class,
    ConfigScreenTest::class,
    LogScreenTest::class
)
class TestSuite


