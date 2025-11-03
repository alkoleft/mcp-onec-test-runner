package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildAction
import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.application.actions.exceptions.BuildException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.time.TimeSource

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
    override fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult = measureExecutionTime { executeBuildDsl(properties, sourceSet) }

    /**
     * Измеряет время выполнения операции с обработкой ошибок
     */
    private fun measureExecutionTime(block: () -> BuildResult): BuildResult {
        val startTime = TimeSource.Monotonic.markNow()
        return try {
            val result = block()
            val duration = startTime.elapsedNow()
            logger.info { "Сборка проекта завершена за $duration" }
            result.copy(duration = duration)
        } catch (e: Exception) {
            val duration = startTime.elapsedNow()
            logger.error(e) { "Сборка проекта завершилась с ошибкой после $duration" }
            throw BuildException("Сборка проекта завершилась с ошибкой: ${e.message}", e)
        }
    }

    /**
     * Метод для выполнения DSL сборки
     */
    fun executeBuildDsl(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult {
        logger.debug { "Сборка проекта" }

        initDsl(properties)
        val state = CurrentBuildState()

        // Загружаем основную конфигурацию
        sourceSet.configuration?.also { configuration ->
            logger.info { "Загружаю основную конфигурацию" }
            val result = loadConfiguration(configuration.name, sourceSet.basePath.resolve(configuration.path))
            state.addResult(configuration.name, result)

            if (result.success) {
                logger.info { "Конфигурация загружена успешно" }
            } else {
                logger.error { "Не удалось загрузить конфигурацию" }
            }
        }

        if (!state.success) {
            return state.toBuildResult()
        }

        // Загружаем расширения
        val extensions = sourceSet.extensions
        logger.info { "Загружаю ${extensions.size} расширений: ${extensions.joinToString(", ") { it.name }}" }
        extensions.forEach {
            val result = loadExtension(it.name, sourceSet.basePath.resolve(it.path))
            state.addResult(it.name, result)

            if (result.success) {
                logger.info { "Расширение ${it.name} загружено успешно" }
            } else {
                logger.error { "Не удалось загрузить расширение ${it.name}" }
            }
            if (!result.success) {
                return@forEach
            }
        }

        if (!state.success) {
            return state.toBuildResult()
        }

        val updateResult = updateDb().also(state::registerUpdateResult)
        if (updateResult.success) {
            logger.info { "Обновление базы данных завершена успешно" }
        } else {
            logger.error { "Обновление базы данных не выполнено" }
        }

        return state.toBuildResult().also { if (it.success) logger.info { "Сборка завершена успешно" } }
    }

    protected abstract fun initDsl(properties: ApplicationProperties): Unit

    protected abstract fun loadConfiguration(
        name: String,
        path: Path,
    ): ShellCommandResult

    protected abstract fun loadExtension(
        name: String,
        path: Path,
    ): ShellCommandResult

    protected abstract fun updateDb(): ShellCommandResult

    private class CurrentBuildState {
        var success: Boolean = true
        val sourceSet = mutableMapOf<String, ShellCommandResult>()
        var updateResult: ShellCommandResult? = null

        fun addResult(
            name: String,
            result: ShellCommandResult,
        ) {
            sourceSet.put(name, result)
            if (!result.success) {
                success = false
            }
        }

        fun registerUpdateResult(result: ShellCommandResult) {
            updateResult = result
            if (!result.success) {
                success = false
            }
        }

        fun toBuildResult(): BuildResult {
            val errors = mutableListOf<String>()
            errors.addAll(sourceSet.values.mapNotNull { it.error })
            updateResult?.error?.let(errors::add)

            return BuildResult(
                success = success,
                errors = errors,
                sourceSet = sourceSet,
            )
        }
    }
}
