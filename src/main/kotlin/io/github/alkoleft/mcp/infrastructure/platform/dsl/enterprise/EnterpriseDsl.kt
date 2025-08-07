package io.github.alkoleft.mcp.infrastructure.platform.dsl.enterprise

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.BasePlatformDsl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * DSL для работы с 1С:Предприятие
 */
@OptIn(ExperimentalTime::class)
class EnterpriseDsl(
    utilityContext: PlatformUtilityContext
) : BasePlatformDsl<EnterpriseContext>(EnterpriseContext(utilityContext)) {
    fun runArguments(value: String) {
        context.runArguments = value
    }
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
} 