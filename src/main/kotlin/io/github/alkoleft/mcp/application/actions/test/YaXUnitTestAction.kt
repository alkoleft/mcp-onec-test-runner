package io.github.alkoleft.mcp.application.actions.test

import io.github.alkoleft.mcp.application.actions.RunTestAction
import io.github.alkoleft.mcp.application.actions.TestExecutionResult
import io.github.alkoleft.mcp.application.actions.exceptions.TestExecuteException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

/**
 * Реализация RunTestAction для тестирования через YaXUnit (DRAFT - только "рыба")
 */
class YaXUnitTestAction(
    private val platformUtilityDsl: PlatformUtilityDsl
) : RunTestAction {

    override suspend fun run(filter: String?, projectProperties: ApplicationProperties): TestExecutionResult {
        val startTime = Instant.now()
        logger.info { "Starting YaXUnit test execution with filter: $filter" }

        return withContext(Dispatchers.IO) {
            try {
                val testsPath = projectProperties.testsPath ?: projectProperties.basePath.resolve("tests")
                val yaxunitEnginePath =
                    projectProperties.yaxunitEnginePath ?: projectProperties.basePath.resolve("yaxunit")

                logger.info { "Tests path: $testsPath" }
                logger.info { "YaXUnit engine path: $yaxunitEnginePath" }

                // TODO: Реализовать специфичную логику для YaXUnit
                // Пока возвращаем заглушку
                val duration = Duration.between(startTime, Instant.now())

                logger.info { "YaXUnit test execution completed (draft mode)" }

                TestExecutionResult(
                    success = true,
                    testsRun = 1,
                    testsPassed = 1,
                    testsFailed = 0,
                    reportPath = null,
                    errors = emptyList(),
                    duration = duration
                )

            } catch (e: Exception) {
                val duration = Duration.between(startTime, Instant.now())
                logger.error(e) { "YaXUnit test execution failed after $duration" }
                throw TestExecuteException("YaXUnit test execution failed: ${e.message}", e)
            }
        }
    }
} 