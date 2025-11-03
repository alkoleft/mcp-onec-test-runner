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

    override fun executeTests(
        utilityLocation: UtilityLocation,
        request: TestExecutionRequest,
    ): YaXUnitExecutionResult {
        val startTime = Instant.now()
        logger.info { "Запуск выполнения тестов YaXUnit для ${request.javaClass.simpleName}" }

        // Создаем временную конфигурацию
        val (configPath, config) = createConfigFile(request)
        try {
            // Запускаем тесты через EnterpriseDsl
            logger.debug { "Выполнение тестов через EnterpriseDsl" }
            val result =
                executeTests(
                    request = request,
                    configPath = configPath,
                )

            logger.info { "Процесс завершен с кодом выхода: ${result.exitCode}" }

            val duration = Duration.between(startTime, Instant.now())
            logger.info { "Выполнение тестов завершено за ${duration.toSeconds()}с" }

            // Определяем путь к отчету
            val reportPath = Path(config.reportPath)
            if (Files.exists(reportPath)) {
                logger.info { "Отчет о тестах найден по пути: $reportPath" }
            } else {
                logger.warn { "Отчет о тестах не найден по ожидаемому пути" }
            }

            return YaXUnitExecutionResult(
                success = result.success,
                reportPath = reportPath,
                exitCode = result.exitCode,
                standardOutput = result.output,
                errorOutput = result.error ?: "",
                duration = duration,
            )
        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now())
            logger.error(e) { "Выполнение тестов YaXUnit завершилось с ошибкой после ${duration.toSeconds()}с" }
            return YaXUnitExecutionResult(
                success = false,
                reportPath = null,
                exitCode = -1,
                standardOutput = "",
                errorOutput = e.message ?: "Неизвестная ошибка",
                duration = duration,
            )
        }
    }

    private fun createConfigFile(request: TestExecutionRequest): Pair<Path, YaXUnitConfig> {
        val config = request.toConfig()
        config.validate().also {
            if (!it.isValid) {
                logger.warn { "Проверка конфигурации не пройдена: ${it.errors.joinToString(", ")}" }
            }
        }
        val configPath = configPath()
        objectMapper.writeValue(configPath.toFile(), config)

        return configPath to config
    }

    /**
     * Строит аргументы команды для запуска 1С:Предприятие
     */
    private fun executeTests(
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
