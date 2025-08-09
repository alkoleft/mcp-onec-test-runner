package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildAction
import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.application.actions.exceptions.BuildException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

/**
 * Абстрактный базовый класс для BuildAction, предоставляющий общую функциональность
 * для измерения времени выполнения, обработки ошибок и логирования
 */
abstract class AbstractBuildAction(
    protected val dsl: PlatformDsl,
) : BuildAction {
    /**
     * Выполняет полную сборку проекта с измерением времени
     */
    override suspend fun build(properties: ApplicationProperties): BuildResult = build(properties, properties.sourceSet)

    override suspend fun build(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult = measureExecutionTime("build") { executeBuildDsl(properties, sourceSet) }

    /**
     * Выполняет сборку только конфигурации с измерением времени
     */
    override suspend fun buildConfiguration(properties: ApplicationProperties): BuildResult =
        measureExecutionTime("buildConfiguration") { executeConfigurationBuildDsl(properties) }

    /**
     * Выполняет сборку расширения с измерением времени
     */
    override suspend fun buildExtension(
        name: String,
        properties: ApplicationProperties,
    ): BuildResult = measureExecutionTime("buildExtension") { executeExtensionBuildDsl(name, properties) }

    /**
     * Измеряет время выполнения операции с обработкой ошибок
     */
    private suspend fun <T> measureExecutionTime(
        operation: String,
        block: suspend () -> T,
    ): T {
        val startTime = Instant.now()
        logger.info { "Начинаю операцию: $operation" }

        return try {
            withContext(Dispatchers.IO) {
                block().also {
                    val duration = Duration.between(startTime, Instant.now())
                    logger.info { "Операция $operation завершена за $duration" }
                }
            }
        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now())
            logger.error(e) { "Операция $operation завершилась с ошибкой после $duration" }
            throw BuildException("Операция $operation завершилась с ошибкой: ${e.message}", e)
        }
    }

    /**
     * Абстрактный метод для выполнения DSL полной сборки
     */
    protected abstract suspend fun executeBuildDsl(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult

    /**
     * Абстрактный метод для выполнения DSL сборки конфигурации
     */
    protected abstract suspend fun executeConfigurationBuildDsl(properties: ApplicationProperties): BuildResult

    /**
     * Абстрактный метод для выполнения DSL сборки расширения
     */
    protected abstract suspend fun executeExtensionBuildDsl(
        extensionName: String,
        properties: ApplicationProperties,
    ): BuildResult
}
