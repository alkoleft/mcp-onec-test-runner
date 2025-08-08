package io.github.alkoleft.mcp.core.modules

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/**
 * Core domain models for test execution
 */

// Test execution requests
sealed class TestExecutionRequest {
    val projectPath: Path
    val testsPath: Path
    val ibConnection: String
    val platformVersion: String?
    val user: String?
    val password: String?

    constructor(properties: ApplicationProperties) {
        projectPath = properties.basePath
        testsPath = properties.testsPath
        platformVersion = properties.platformVersion
        ibConnection = properties.connection.connectionString
        user = properties.connection.user
        password = properties.connection.password
    }
}

class RunAllTestsRequest(properties: ApplicationProperties) : TestExecutionRequest(properties) {
}

class RunModuleTestsRequest(
    val moduleName: String, properties: ApplicationProperties
) : TestExecutionRequest(properties) {
}

data class RunListTestsRequest(
    val testNames: List<String>,
    val properties: ApplicationProperties,
) : TestExecutionRequest(properties) {
}

// Test execution results
data class TestExecutionResult(
    val success: Boolean,
    val report: GenericTestReport,
    val duration: Duration,
    val error: TestExecutionError? = null,
)

sealed class TestExecutionError : Exception() {
    data class UtilNotFound(
        val utility: String,
    ) : TestExecutionError()

    data class BuildFailed(
        val reason: String,
    ) : TestExecutionError()

    data class TestRunFailed(
        val details: String,
    ) : TestExecutionError()

    data class ReportParsingFailed(
        override val message: String,
    ) : TestExecutionError()
}

// Generic test report structure
data class GenericTestReport(
    val metadata: TestMetadata,
    val summary: TestSummary,
    val testSuites: List<GenericTestSuite>,
    val timestamp: Instant,
    val duration: Duration,
)

data class TestMetadata(
    val environment: Map<String, String> = emptyMap(),
    val configuration: Map<String, String> = emptyMap(),
    val tags: Set<String> = emptySet(),
)

data class TestSummary(
    val totalTests: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val errors: Int,
) {
    val successRate: Double get() = if (totalTests > 0) passed.toDouble() / totalTests else 0.0
}

data class GenericTestSuite(
    val name: String,
    val tests: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val errors: Int = 0,
    val duration: Duration,
    val testCases: List<GenericTestCase>,
)

data class GenericTestCase(
    val name: String,
    val className: String?,
    val status: TestStatus,
    val duration: Duration,
    val errorMessage: String? = null,
    val stackTrace: String? = null,
    val systemOut: String? = null,
    val systemErr: String? = null,
)

enum class TestStatus {
    PASSED,
    FAILED,
    SKIPPED,
    ERROR,
}

// Build-related domain models
sealed class BuildDecision {
    object FULL_BUILD : BuildDecision()

    data class INCREMENTAL_BUILD(
        val changedModules: Set<String>,
    ) : BuildDecision()

    object INCREMENTAL_TESTS : BuildDecision()

    object NO_BUILD_NEEDED : BuildDecision()
}

data class BuildResult(
    val success: Boolean,
    val duration: Duration,
    val buildType: BuildType,
    val error: String? = null,
)

enum class BuildType {
    FULL,
    INCREMENTAL,
    SKIP,
}


// File change detection
data class FileChangeEvent(
    val path: Path,
    val changeType: ChangeType,
    val timestamp: Instant,
)

enum class ChangeType {
    NEW,
    MODIFIED,
    DELETED,
    UNCHANGED,
}
