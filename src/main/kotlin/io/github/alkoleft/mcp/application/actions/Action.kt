package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.ChangeType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.ConfiguratorResult
import java.nio.file.Path
import java.time.Duration

/**
 * Интерфейс для сборки конфигурации и расширений
 */
interface BuildAction {
    suspend fun build(properties: ApplicationProperties): BuildResult
    suspend fun build(properties: ApplicationProperties, sourceSet: SourceSet): BuildResult
    suspend fun buildConfiguration(properties: ApplicationProperties): BuildResult
    suspend fun buildExtension(name: String, properties: ApplicationProperties): BuildResult
}

/**
 * Интерфейс для анализа изменений в проекте
 */
interface ChangeAnalysisAction {
    suspend fun analyze(properties: ApplicationProperties): ChangeAnalysisResult
}

/**
 * Интерфейс для запуска тестов
 */
interface RunTestAction {
    suspend fun run(filter: String? = null, properties: ApplicationProperties): TestExecutionResult
}

/**
 * Результат сборки
 */
data class BuildResult(
    val success: Boolean,
    val configurationBuilt: Boolean = false,
    val errors: List<String> = emptyList(),
    val duration: Duration = Duration.ZERO,
    val sourceSet: Map<String, ConfiguratorResult> = emptyMap()
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