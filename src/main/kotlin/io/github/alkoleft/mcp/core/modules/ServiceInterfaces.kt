package io.github.alkoleft.mcp.core.modules

import io.github.alkoleft.mcp.application.actions.change.ChangesSet
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import java.nio.file.Path

/**
 * Build state management
 */
interface BuildStateManager {
    fun checkChanges(properties: ApplicationProperties): ChangesSet

    fun updateHashes(files: Map<Path, String>)
}

/**
 * YAXUnit test runner
 */
interface YaXUnitRunner {
    fun executeTests(
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

enum class ReportFormat {
    JUNIT_XML,
}
