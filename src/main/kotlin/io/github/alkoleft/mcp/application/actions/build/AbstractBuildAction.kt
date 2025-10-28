package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildAction
import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.application.actions.exceptions.BuildException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
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
    override suspend fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult = measureExecutionTime("build") { executeBuildDsl(properties, sourceSet) }

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
     * Метод для выполнения DSL сборки
     */
    fun executeBuildDsl(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult {
        logger.debug { "Формирую единый DSL для сборки проекта" }

        initDsl(properties)
        val results = mutableMapOf<String, ShellCommandResult>()

        // Загружаем основную конфигурацию
        sourceSet.configuration?.also { configuration ->
            logger.info { "Загружаю основную конфигурацию" }
            val result = loadConfiguration(configuration.name, sourceSet.basePath.resolve(configuration.path))
            if (!result.success) {
                return result
            }

            results.putAll(result.sourceSet)
        }

        // Загружаем расширения
        val extensions = sourceSet.extensions
        logger.info { "Загружаю ${extensions.size} расширений: ${extensions.joinToString(", ") { it.name }}" }
        extensions.forEach {
            val result = loadExtension(it.name, sourceSet.basePath.resolve(it.path))
            if (!result.success) {
                return result
            }

            results.putAll(result.sourceSet)
        }

        return BuildResult(
            success = true,
            sourceSet = results.toMap(),
        )
    }


    protected abstract fun initDsl(properties: ApplicationProperties): Unit

    protected abstract fun loadConfiguration(name: String, path: Path): BuildResult

    protected abstract fun loadExtension(name: String, path: Path): BuildResult
}
