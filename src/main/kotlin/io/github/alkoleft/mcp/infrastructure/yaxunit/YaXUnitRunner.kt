package io.github.alkoleft.mcp.infrastructure.yaxunit

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.YaXUnitExecutionResult
import io.github.alkoleft.mcp.core.modules.YaXUnitRunner
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.alkoleft.mcp.infrastructure.utility.ifNoBlank
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.Path

private val logger = KotlinLogging.logger { }

/**
 * Реализация YaXUnitRunner для запуска тестов через 1С:Предприятие
 * Интегрирован со стратегиями построения команд и обработки ошибок
 */
class YaXUnitRunner(
    private val platformDsl: PlatformDsl,
) : YaXUnitRunner {
    private val objectMapper = ObjectMapper()

    override suspend fun executeTests(
        utilityLocation: UtilityLocation,
        request: TestExecutionRequest,
    ): YaXUnitExecutionResult =
        withContext(Dispatchers.IO) {
            val startTime = Instant.now()
            logger.info { "Starting YaXUnit test execution for ${request.javaClass.simpleName}" }

            // Создаем временную конфигурацию
            val (configPath, config) = createConfigFile(request)
            try {
                // Запускаем тесты через EnterpriseDsl
                logger.debug { "Executing tests via EnterpriseDsl" }
                val result =
                    executeTests(
                        request = request,
                        configPath = configPath,
                    )

                logger.info { "Process completed with exit code: ${result.exitCode}" }

                val duration = Duration.between(startTime, Instant.now())
                logger.info { "Test execution completed in ${duration.toSeconds()}s" }

                // Определяем путь к отчету
                val reportPath = Path(config.reportPath)
                if (Files.exists(reportPath)) {
                    logger.info { "Test report found at: $reportPath" }
                } else {
                    logger.warn { "Test report not found at expected location" }
                }

                YaXUnitExecutionResult(
                    success = result.success,
                    reportPath = if (result.success) reportPath else null,
                    exitCode = result.exitCode,
                    standardOutput = result.output,
                    errorOutput = result.error ?: "",
                    duration = duration,
                )
            } catch (e: Exception) {
                val duration = Duration.between(startTime, Instant.now())
                logger.error(e) { "YaXUnit test execution failed after ${duration.toSeconds()}s" }
                YaXUnitExecutionResult(
                    success = false,
                    reportPath = null,
                    exitCode = -1,
                    standardOutput = "",
                    errorOutput = e.message ?: "Unknown error",
                    duration = duration,
                )
            }
        }

    private fun createConfigFile(request: TestExecutionRequest): Pair<Path, YaXUnitConfig> {
        val config = request.toConfig()
        config.validate().also {
            if (!it.isValid) {
                logger.warn { "Configuration validation failed: ${it.errors.joinToString(", ")}" }
            }
        }
        val configPath = configPath()
        objectMapper.writeValue(configPath.toFile(), config)

        return configPath to config
    }

    /**
     * Строит аргументы команды для запуска 1С:Предприятие
     */
    private suspend fun executeTests(
        request: TestExecutionRequest,
        configPath: Path,
    ): ProcessResult =
        platformDsl
            .enterprise {
                connect(request.ibConnection)
                request.user?.ifNoBlank { user(it) }
                request.password?.ifNoBlank { password(it) }
                runArguments("RunUnitTests=${configPath.toAbsolutePath()}")
            }.run()
}
