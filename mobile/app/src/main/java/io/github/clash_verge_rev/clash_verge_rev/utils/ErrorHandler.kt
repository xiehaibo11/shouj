package io.github.clash_verge_rev.clash_verge_rev.utils

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 统一错误处理器
 * 提供全局的错误处理、日志记录和用户提示功能
 */
object ErrorHandler {
    
    private const val TAG = "ErrorHandler"
    private const val MAX_LOG_FILES = 7 // 保留最近7天的日志
    private const val MAX_LOG_SIZE = 10 * 1024 * 1024 // 10MB
    
    private var logDir: File? = null
    private var currentLogFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * 初始化错误处理器
     */
    fun initialize(context: Context) {
        logDir = File(context.filesDir, "logs").apply {
            if (!exists()) mkdirs()
        }
        
        // 清理旧日志
        cleanOldLogs()
        
        // 设置全局未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)
        }
        
        Log.i(TAG, "ErrorHandler initialized")
    }
    
    /**
     * 处理错误
     */
    fun handleError(
        error: Throwable,
        context: String = "",
        showToUser: Boolean = true,
        snackbarHost: SnackbarHostState? = null,
        scope: CoroutineScope? = null
    ) {
        val errorMessage = formatErrorMessage(error, context)
        
        // 记录到日志
        logError(errorMessage, error)
        
        // 显示给用户
        if (showToUser && snackbarHost != null && scope != null) {
            scope.launch {
                try {
                    snackbarHost.showSnackbar(getUserFriendlyMessage(error, context))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to show snackbar", e)
                }
            }
        }
    }
    
    /**
     * 处理未捕获异常
     */
    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        val errorMessage = """
            |========================================
            |Uncaught Exception
            |Thread: ${thread.name}
            |Time: ${timeFormat.format(Date())}
            |========================================
            |${getStackTraceString(throwable)}
            |========================================
        """.trimMargin()
        
        logError(errorMessage, throwable)
        
        // 重新抛出异常，让系统处理
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        defaultHandler?.uncaughtException(thread, throwable)
    }
    
    /**
     * 记录错误到日志文件
     */
    private fun logError(message: String, error: Throwable? = null) {
        try {
            // 控制台输出
            Log.e(TAG, message, error)
            
            // 写入文件
            val logFile = getCurrentLogFile()
            logFile?.appendText("$message\n")
            
            // 检查文件大小
            if (logFile != null && logFile.length() > MAX_LOG_SIZE) {
                rotateLogFile()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }
    
    /**
     * 记录信息日志
     */
    fun logInfo(tag: String, message: String) {
        val logMessage = formatLogMessage("INFO", tag, message)
        Log.i(tag, message)
        writeToFile(logMessage)
    }
    
    /**
     * 记录警告日志
     */
    fun logWarning(tag: String, message: String) {
        val logMessage = formatLogMessage("WARN", tag, message)
        Log.w(tag, message)
        writeToFile(logMessage)
    }
    
    /**
     * 记录调试日志
     */
    fun logDebug(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            val logMessage = formatLogMessage("DEBUG", tag, message)
            Log.d(tag, message)
            writeToFile(logMessage)
        }
    }
    
    /**
     * 格式化日志消息
     */
    private fun formatLogMessage(level: String, tag: String, message: String): String {
        return "${timeFormat.format(Date())} [$level] [$tag] $message"
    }
    
    /**
     * 格式化错误消息
     */
    private fun formatErrorMessage(error: Throwable, context: String): String {
        val builder = StringBuilder()
        builder.append("========================================\n")
        builder.append("Error occurred\n")
        if (context.isNotEmpty()) {
            builder.append("Context: $context\n")
        }
        builder.append("Time: ${timeFormat.format(Date())}\n")
        builder.append("========================================\n")
        builder.append("${error.javaClass.simpleName}: ${error.message}\n")
        builder.append(getStackTraceString(error))
        builder.append("========================================\n")
        return builder.toString()
    }
    
    /**
     * 获取堆栈跟踪字符串
     */
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
    
    /**
     * 获取用户友好的错误消息
     */
    private fun getUserFriendlyMessage(error: Throwable, context: String): String {
        return when (error) {
            is java.net.UnknownHostException -> "网络连接失败，请检查网络设置"
            is java.net.SocketTimeoutException -> "连接超时，请检查网络或稍后重试"
            is java.io.FileNotFoundException -> "文件不存在或无法访问"
            is java.io.IOException -> "文件读写错误: ${error.message}"
            is IllegalArgumentException -> "参数错误: ${error.message}"
            is IllegalStateException -> "状态错误: ${error.message}"
            else -> {
                val msg = error.message ?: "未知错误"
                if (context.isNotEmpty()) {
                    "$context: $msg"
                } else {
                    "操作失败: $msg"
                }
            }
        }
    }
    
    /**
     * 获取当前日志文件
     */
    private fun getCurrentLogFile(): File? {
        if (logDir == null) return null
        
        val today = dateFormat.format(Date())
        if (currentLogFile == null || !currentLogFile!!.name.contains(today)) {
            currentLogFile = File(logDir, "clash-$today.log")
            if (!currentLogFile!!.exists()) {
                currentLogFile!!.createNewFile()
            }
        }
        
        return currentLogFile
    }
    
    /**
     * 写入日志文件
     */
    private fun writeToFile(message: String) {
        try {
            val logFile = getCurrentLogFile()
            logFile?.appendText("$message\n")
            
            // 检查文件大小
            if (logFile != null && logFile.length() > MAX_LOG_SIZE) {
                rotateLogFile()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    /**
     * 轮转日志文件
     */
    private fun rotateLogFile() {
        try {
            val today = dateFormat.format(Date())
            val timestamp = System.currentTimeMillis()
            val oldFile = currentLogFile
            val newFile = File(logDir, "clash-$today-$timestamp.log")
            
            oldFile?.renameTo(newFile)
            currentLogFile = File(logDir, "clash-$today.log").apply {
                createNewFile()
            }
            
            Log.i(TAG, "Log file rotated")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log file", e)
        }
    }
    
    /**
     * 清理旧日志文件
     */
    private fun cleanOldLogs() {
        try {
            val logFiles = logDir?.listFiles { file ->
                file.isFile && file.name.endsWith(".log")
            } ?: return
            
            // 按修改时间排序
            val sortedFiles = logFiles.sortedByDescending { it.lastModified() }
            
            // 删除超过限制的文件
            if (sortedFiles.size > MAX_LOG_FILES) {
                sortedFiles.drop(MAX_LOG_FILES).forEach { file ->
                    try {
                        file.delete()
                        Log.i(TAG, "Deleted old log: ${file.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete log file: ${file.name}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean old logs", e)
        }
    }
    
    /**
     * 获取所有日志文件
     */
    fun getLogFiles(): List<File> {
        return logDir?.listFiles { file ->
            file.isFile && file.name.endsWith(".log")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * 导出日志
     */
    fun exportLogs(outputFile: File): Boolean {
        return try {
            val logFiles = getLogFiles()
            outputFile.bufferedWriter().use { writer ->
                logFiles.forEach { logFile ->
                    writer.write("========== ${logFile.name} ==========\n")
                    logFile.bufferedReader().use { reader ->
                        reader.copyTo(writer)
                    }
                    writer.write("\n\n")
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export logs", e)
            false
        }
    }
    
    /**
     * 清除所有日志
     */
    fun clearAllLogs(): Boolean {
        return try {
            val logFiles = getLogFiles()
            logFiles.forEach { it.delete() }
            currentLogFile = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
            false
        }
    }
}

/**
 * BuildConfig 占位符（实际应由Gradle生成）
 */
object BuildConfig {
    const val DEBUG = true
}

