package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.ActionFactory
import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.application.actions.ConvertResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ProjectFormat
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.infrastructure.utility.ConnectionStringUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import kotlin.time.Duration

private val logger = KotlinLogging.logger { }

/**
 * Сервис запуска действий, связанных с выполнением тестов YaXUnit.
 * Управляет процессом сборки, включая анализ изменений, конвертацию источников (для EDT) и обновление информационной базы.
 */
@Service
class LauncherService(
    private val actionFactory: ActionFactory,
    private val properties: ApplicationProperties,
) {
    /**
     * Подготавливает набор источников EDT, если формат проекта EDT.
     */
    private val edtSourceSet: SourceSet = createEdtSourceSet()
    /**
     * Подготавливает набор источников Designer, обрабатывая пути конвертации для EDT.
     */
    private val designerSourceSet: SourceSet = createDesignerSourceSet()

    /**
     * Запускает запрос на выполнение теста.
     * Сначала выполняет сборку, затем действие запуска теста.
     *
     * @param request Запрос на выполнение теста.
     * @return TestExecutionResult от действия запуска.
     * @throws TestExecutionError.BuildFailed если сборка не удалась.
     */
    suspend fun run(request: TestExecutionRequest): TestExecutionResult {
        val buildResult = build()
        if (!buildResult.success) {
            val reason = if (buildResult.errors.isNotEmpty()) buildResult.errors.joinToString("; ") else "Сборка не удалась"
            throw TestExecutionError.BuildFailed(reason)
        }
        return actionFactory.createRunTestAction().run(request)
    }

    /**
     * Выполняет процесс сборки.
     * Анализирует изменения, конвертирует источники при EDT, нормализует подключение и обновляет информационную базу.
     *
     * @return BuildResult со статусом успеха, ошибками и деталями.
     * @throws TestExecutionError.BuildFailed при проблемах конфигурации.
     */
    suspend fun build(): BuildResult {
        // Анализ изменений в исходных файлах
        val changeAnalyzer = actionFactory.createChangeAnalysisAction()
        val changes = changeAnalyzer.run(properties)
        if (!changes.hasChanges) {
            logger.info { "Исходные файлы не изменены. Пропуск обновления информационной базы." }
            return BuildResult(
                success = true,
                configurationBuilt = false,
                errors = emptyList(),
                duration = Duration.ZERO,
                sourceSet = emptyMap(),
            )
        }

        // Фильтрация наборов источников с изменениями
        val changedSourceSets = properties.sourceSet.subSourceSet { it.name in changes.sourceSetChanges.keys }

        if (changedSourceSets.isEmpty()) {
            throw TestExecutionError.BuildFailed("Не удалось распределить изменения по субпроектам.")
        }
        logger.info { "Обнаружены изменения: ${changedSourceSets.joinToString { it.name }}" }

        // Обработка конвертации формата EDT, если применимо
        if (properties.format == ProjectFormat.EDT) {
            val convertResult = convertSources(changedSourceSets, designerSourceSet)
            if (!convertResult.success) {
                logger.error { "Ошибки конвертации исходников EDT: ${convertResult.errors.joinToString()}" }
                return BuildResult(
                    success = false,
                    configurationBuilt = false,
                    errors = convertResult.errors,
                    duration = Duration.ZERO,
                    sourceSet = emptyMap(),
                )
            }
        }

        // Нормализация строки подключения для CLI
        val normalizedConnectionString = ConnectionStringUtils.normalizeForCli(properties.connection.connectionString, properties.basePath)
        logger.debug { "Нормализованная строка подключения: $normalizedConnectionString" }

        // Обновление информационной базы измененными источниками
        val result = updateIB(changedSourceSets, normalizedConnectionString)

        // Обработка результатов и сохранение состояния для успешных обновлений
        var success = true
        val errors = mutableListOf<String>()
        result.sourceSet.forEach { (name, resultItem) ->
            success = success && resultItem.success
            if (resultItem.success) {
                changeAnalyzer.saveSourceSetState(properties, changes.sourceSetChanges[name]!!)
            } else {
                resultItem.error?.takeIf { it.isNotBlank() }?.let { errors.add(it) }
            }
        }

        return if (success && result.success) {
            result
        } else {
            BuildResult(
                success = false,
                configurationBuilt = result.configurationBuilt,
                errors = errors.ifEmpty { result.errors },
                duration = result.duration,
                sourceSet = result.sourceSet,
            )
        }
    }

    /**
     * Конвертирует источники EDT в формат Designer для измененных наборов источников.
     *
     * @param changedSourceSets Наборы источников с изменениями для конвертации.
     * @param destination Целевой набор источников для конвертации.
     * @return ConvertResult со статусом успеха и ошибками.
     */
    private suspend fun convertSources(
        changedSourceSets: SourceSet,
        destination: SourceSet,
    ): ConvertResult =
        actionFactory.convertAction().run(
            properties,
            edtSourceSet.subSourceSet { changedSourceSets.find { item -> item.name == it.name } != null },
            destination,
        )

    /**
     * Обновляет информационную базу с использованием действия сборки для измененных наборов источников.
     *
     * @param changedSourceSets Наборы источников для обновления.
     * @param connectionString Нормализованная строка подключения.
     * @return BuildResult от действия сборки.
     */
    private suspend fun updateIB(
        changedSourceSets: SourceSet,
        connectionString: String,
    ): BuildResult {
        val builder = actionFactory.createBuildAction(properties.tools.builder)
        return builder.run(
            properties.copy(connection = properties.connection.copy(connectionString = connectionString)),
            designerSourceSet.subSourceSet { changedSourceSets.find { item -> item.name == it.name } != null },
        )
    }

    /**
     * Создает набор источников EDT на основе формата проекта.
     * @return SourceSet для EDT или пустой, если не EDT.
     */
    private fun createEdtSourceSet() =
        if (properties.format == ProjectFormat.EDT) {
            SourceSet(
                properties.basePath,
                properties.sourceSet,
            )
        } else {
            SourceSet.EMPTY
        }

    /**
     * Создает набор источников Designer, корректируя пути для проектов EDT.
     * @return SourceSet для формата Designer.
     */
    private fun createDesignerSourceSet() =
        if (properties.format == ProjectFormat.EDT) {
            SourceSet(
                properties.workPath,
                properties.sourceSet.map { it.copy(path = it.name) },
            )
        } else {
            SourceSet(
                properties.basePath,
                properties.sourceSet,
            )
        }
}
