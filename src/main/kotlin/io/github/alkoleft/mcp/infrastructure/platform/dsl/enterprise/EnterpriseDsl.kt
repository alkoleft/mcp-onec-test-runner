package io.github.alkoleft.mcp.infrastructure.platform.dsl.enterprise

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * DSL для работы с 1С:Предприятие
 */
@OptIn(ExperimentalTime::class)
class EnterpriseDsl(
    private val context: EnterpriseContext
) {
    
    /**
     * Запускает 1С:Предприятие с указанными параметрами
     */
    suspend fun run(): PlatformUtilityResult = withContext(Dispatchers.IO) {
        val startTime = Clock.System.now()
        
        try {
            val args = context.buildBaseArgs()
            val processBuilder = ProcessBuilder(args)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            
            val exitCode = process.waitFor()
            val duration = Clock.System.now() - startTime
            
            val success = exitCode == 0
            
            context.setResult(success, output, null, exitCode, duration)
            context.buildResult()
            
        } catch (e: Exception) {
            val duration = Clock.System.now() - startTime
            context.setResult(false, "", e.message, -1, duration)
            context.buildResult()
        }
    }
    
    /**
     * Запускает тесты через 1С:Предприятие
     */
    suspend fun runTests(configPath: Path): PlatformUtilityResult = withContext(Dispatchers.IO) {
        val startTime = Clock.System.now()
        
        try {
            val args = context.buildTestArgs(configPath)
            val processBuilder = ProcessBuilder(args)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            
            val exitCode = process.waitFor()
            val duration = Clock.System.now() - startTime
            
            val success = exitCode == 0
            
            context.setResult(success, output, null, exitCode, duration)
            context.buildResult()
            
        } catch (e: Exception) {
            val duration = Clock.System.now() - startTime
            context.setResult(false, "", e.message, -1, duration)
            context.buildResult()
        }
    }
    
    /**
     * Проверяет доступность 1С:Предприятие
     */
    suspend fun checkAvailability(): Boolean = withContext(Dispatchers.IO) {
        try {
            val args = context.buildBaseArgs().toMutableList()
            args.add("/?") // Запрос справки
            
            val processBuilder = ProcessBuilder(args)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            val completed = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            
            if (!completed) {
                process.destroyForcibly()
                return@withContext false
            }
            
            val exitCode = process.exitValue()
            exitCode in 0..2 // Для 1С утилит коды 0, 1, 2 обычно допустимы для команды помощи
            
        } catch (e: Exception) {
            false
        }
    }
} 