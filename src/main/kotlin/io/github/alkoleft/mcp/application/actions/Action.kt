package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.application.actions.change.ChangesSet
import io.github.alkoleft.mcp.application.actions.change.SourceSetChanges
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.ConfiguratorResult
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/**
 * Интерфейс для сборки конфигурации и расширений
 */
interface BuildAction {
    suspend fun build(properties: ApplicationProperties): BuildResult

    suspend fun build(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult

    suspend fun buildConfiguration(properties: ApplicationProperties): BuildResult

    suspend fun buildExtension(
        name: String,
        properties: ApplicationProperties,
    ): BuildResult
}

/**
 * Интерфейс для анализа изменений в проекте
 */
interface ChangeAnalysisAction {
    suspend fun analyze(properties: ApplicationProperties): ChangeAnalysisResult

    /**
     * Анализирует изменения с группировкой по source set для оптимизированной сборки
     */
    suspend fun analyzeBySourceSet(properties: ApplicationProperties): FileSystemChangeAnalysisResult {
        val basic = analyze(properties)
        return FileSystemChangeAnalysisResult(
            hasChangesFlag = basic.hasChanges,
            changedFilesSet = basic.changedFiles,
            sourceSetChanges = emptyMap(),
            changeTypesMap = basic.changeTypes,
            analysisTimestamp = Instant.now(),
        )
    }

    /**
     * Сохраняет состояние source set для инкрементальной сборки
     */
    suspend fun saveSourceSetState(
        properties: ApplicationProperties,
        sourceSetChanges: SourceSetChanges,
    ): Boolean
}

/**
 * Интерфейс для запуска тестов
 */
interface RunTestAction {
    suspend fun run(request: TestExecutionRequest): TestExecutionResult
}

/**
 * Результат сборки
 */
data class BuildResult(
    val success: Boolean,
    val configurationBuilt: Boolean = false,
    val errors: List<String> = emptyList(),
    val duration: Duration = Duration.ZERO,
    val sourceSet: Map<String, ConfiguratorResult> = emptyMap(),
)

/**
 * Результат анализа изменений
 */
open class ChangeAnalysisResult(
    val hasChanges: Boolean,
    val changedFiles: Set<Path> = emptySet(),
    val affectedModules: Set<String> = emptySet(),
    val changeTypes: ChangesSet = emptyMap(),
)

/**
 * Расширенный результат анализа изменений с группировкой по source set
 */
data class FileSystemChangeAnalysisResult(
    val hasChangesFlag: Boolean,
    val changedFilesSet: Set<Path>,
    val sourceSetChanges: Map<String, SourceSetChanges>,
    val changeTypesMap: ChangesSet,
    val analysisTimestamp: Instant,
) : ChangeAnalysisResult(
        hasChanges = hasChangesFlag,
        changedFiles = changedFilesSet,
        affectedModules = sourceSetChanges.keys,
        changeTypes = changeTypesMap,
    ) {
    val affectedSourceSets: Set<String> get() = sourceSetChanges.keys
    val totalChangedFiles: Int get() = changedFiles.size
    val sourceSetCount: Int get() = sourceSetChanges.size
}

/**
 * Результат выполнения тестов
 */
data class TestExecutionResult(
    val success: Boolean,
    val report: GenericTestReport,
    val reportPath: Path? = null,
    val errors: List<String> = emptyList(),
    val duration: Duration = Duration.ZERO,
)
