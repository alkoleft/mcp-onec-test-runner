package io.github.alkoleft.mcp.core.modules

import io.github.alkoleft.mcp.application.actions.change.ChangesSet
import java.io.InputStream
import java.nio.file.Path

/**
 * Test execution facade
 */
interface TestLauncher {
    suspend fun run(request: TestExecutionRequest): TestExecutionResult
}

/**
 * Build state management
 */
interface BuildStateManager {
    suspend fun checkChanges(projectPath: Path): ChangesSet

    suspend fun updateHashes(files: Map<Path, String>)

    suspend fun getLastBuildTime(projectPath: Path): Long?

    suspend fun setLastBuildTime(
        projectPath: Path,
        timestamp: Long,
    )
}

/**
 * YAXUnit test runner
 */
interface YaXUnitRunner {
    suspend fun executeTests(
        utilityLocation: UtilityLocation,
        request: TestExecutionRequest,
    ): YaXUnitExecutionResult
}

data class YaXUnitExecutionResult(
    val success: Boolean,
    val reportPath: Path?,
    val exitCode: Int,
    val standardOutput: String,
    val errorOutput: String,
    val duration: java.time.Duration,
)

/**
 * Report parsing
 */
interface ReportParser {
    suspend fun parseReport(
        input: InputStream,
        format: ReportFormat,
    ): GenericTestReport

    suspend fun detectFormat(input: InputStream): ReportFormat

    fun getSupportedFormats(): Set<ReportFormat>
}

enum class ReportFormat {
    JUNIT_XML,
    JSON,
    YAXUNIT_JSON,
    PLAIN_TEXT,
}
