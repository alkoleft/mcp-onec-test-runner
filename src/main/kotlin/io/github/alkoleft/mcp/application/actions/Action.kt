package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.application.actions.change.ChangesSet
import io.github.alkoleft.mcp.application.actions.change.SourceSetChanges
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import java.nio.file.Path
import java.time.Instant
import kotlin.time.Duration

/**
 * Интерфейс для сборки конфигурации и расширений
 */
interface BuildAction {
    suspend fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult
}

interface ConvertAction {
    suspend fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        destination: SourceSet,
    ): ConvertResult
}

/**
 * Интерфейс для анализа изменений в проекте
 */
interface ChangeAnalysisAction {
    suspend fun run(properties: ApplicationProperties): ChangeAnalysisResult

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
    val sourceSet: Map<String, ShellCommandResult> = emptyMap(),
)

/**
 * Результат сборки
 */
data class ConvertResult(
    val success: Boolean,
    val errors: List<String> = emptyList(),
    val duration: Duration = Duration.ZERO,
    val sourceSet: Map<String, ShellCommandResult> = emptyMap(),
)

/**
 * Результат анализа изменений
 */
data class ChangeAnalysisResult(
    val hasChanges: Boolean,
    val changedFiles: Set<Path> = emptySet(),
    val changeTypes: ChangesSet = emptyMap(),
    val sourceSetChanges: Map<String, SourceSetChanges> = emptyMap(),
    val analysisTimestamp: Instant = Instant.now(),
)
