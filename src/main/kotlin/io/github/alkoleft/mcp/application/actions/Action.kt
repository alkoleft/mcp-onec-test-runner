package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.ChangeType
import java.nio.file.Path
import java.time.Duration

/**
 * Интерфейс для сборки конфигурации и расширений
 */
interface BuildAction {
    suspend fun build(projectProperties: ApplicationProperties): BuildResult
    suspend fun buildConfiguration(projectProperties: ApplicationProperties): BuildResult
    suspend fun buildExtension(name: String, projectProperties: ApplicationProperties): BuildResult
}

/**
 * Интерфейс для анализа изменений в проекте
 */
interface ChangeAnalysisAction {
    suspend fun analyze(projectProperties: ApplicationProperties): ChangeAnalysisResult
}

/**
 * Интерфейс для запуска тестов
 */
interface RunTestAction {
    suspend fun run(filter: String? = null, projectProperties: ApplicationProperties): TestExecutionResult
}

/**
 * Результат сборки
 */
data class BuildResult(
    val success: Boolean,
    val configurationBuilt: Boolean = false,
    val extensionsBuilt: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val duration: Duration = Duration.ZERO
)

/**
 * Результат анализа изменений
 */
data class ChangeAnalysisResult(
    val hasChanges: Boolean,
    val changedFiles: Set<Path> = emptySet(),
    val affectedModules: Set<String> = emptySet(),
    val changeTypes: Map<Path, ChangeType> = emptyMap()
)

/**
 * Результат выполнения тестов
 */
data class TestExecutionResult(
    val success: Boolean,
    val testsRun: Int = 0,
    val testsPassed: Int = 0,
    val testsFailed: Int = 0,
    val reportPath: Path? = null,
    val errors: List<String> = emptyList(),
    val duration: Duration = Duration.ZERO
) 