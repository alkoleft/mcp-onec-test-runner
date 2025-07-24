package io.github.alkoleft.mcp.core.modules

import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/**
 * Core domain models for test execution
 */

// Test execution requests
sealed class TestExecutionRequest {
    abstract val projectPath: Path
    abstract val testsPath: Path
    abstract val ibConnection: String
    abstract val platformVersion: String?
}

data class RunAllTestsRequest(
    override val projectPath: Path,
    override val testsPath: Path = projectPath.resolve("tests"),
    override val ibConnection: String,
    override val platformVersion: String? = null,
) : TestExecutionRequest()

data class RunModuleTestsRequest(
    override val projectPath: Path,
    override val testsPath: Path = projectPath.resolve("tests"),
    override val ibConnection: String,
    override val platformVersion: String? = null,
    val moduleName: String,
) : TestExecutionRequest()

data class RunListTestsRequest(
    override val projectPath: Path,
    override val testsPath: Path = projectPath.resolve("tests"),
    override val ibConnection: String,
    override val platformVersion: String? = null,
    val testNames: List<String>,
) : TestExecutionRequest()

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

// Platform utilities
data class UtilityLocation(
    val executablePath: Path,
    val version: String?,
    val platformType: PlatformType,
)

enum class PlatformType {
    WINDOWS,
    LINUX,
    MACOS,
}

enum class UtilityType {
    COMPILER_1CV8C,
    INFOBASE_MANAGER_IBCMD,
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
