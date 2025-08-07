package io.github.alkoleft.mcp.core.modules

import java.io.InputStream
import java.nio.file.Path

/**
 * Core service interfaces defining contracts for the application layer
 */

// Test execution facade
interface TestLauncher {
    suspend fun runAll(request: RunAllTestsRequest): TestExecutionResult

    suspend fun runModule(request: RunModuleTestsRequest): TestExecutionResult

    suspend fun runList(request: RunListTestsRequest): TestExecutionResult
}

// Build orchestration service
interface BuildService {
    suspend fun ensureBuild(projectPath: Path): BuildResult

    suspend fun determineBuildStrategy(projectPath: Path): BuildDecision

    suspend fun performFullBuild(projectPath: Path): BuildResult

    suspend fun performIncrementalBuild(
        projectPath: Path,
        changedModules: Set<String>,
    ): BuildResult
}

// Build state management
interface BuildStateManager {
    suspend fun checkChanges(projectPath: Path): Map<Path, ChangeType>

    suspend fun updateHashes(
        projectPath: Path,
        files: Map<Path, String>,
    )

    suspend fun getLastBuildTime(projectPath: Path): Long?

    suspend fun setLastBuildTime(
        projectPath: Path,
        timestamp: Long,
    )
}

// Hash storage for change detection
interface HashStorage {
    suspend fun getHash(file: Path): String?

    suspend fun storeHash(
        file: Path,
        hash: String,
    )

    suspend fun batchUpdate(updates: Map<Path, String>)

    suspend fun removeHash(file: Path)

    suspend fun getAllHashes(): Map<String, String>

    suspend fun close()
}

// Platform utility location
interface UtilLocator {
    suspend fun locateUtility(
        utility: UtilityType,
        version: String? = null,
    ): UtilityLocation

    suspend fun validateUtility(location: UtilityLocation): Boolean

    fun clearCache()
}

// YAXUnit test runner
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

// Report parsing
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

// Configuration writing for YAXUnit
interface YaXUnitConfigWriter {
    suspend fun writeConfig(
        request: TestExecutionRequest,
        outputPath: Path,
    ): Path

    suspend fun createTempConfig(request: TestExecutionRequest): Path
}

// File monitoring service
interface FileWatcher {
    suspend fun watchDirectory(
        path: Path,
        callback: (FileChangeEvent) -> Unit,
    )

    suspend fun getModifiedFiles(projectPath: Path): Set<Path>

    suspend fun stopWatching(path: Path)
}

class FileWatcherImpl : FileWatcher {
    override suspend fun watchDirectory(
        path: Path,
        callback: (FileChangeEvent) -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getModifiedFiles(projectPath: Path): Set<Path> {
        TODO("Not yet implemented")
    }

    override suspend fun stopWatching(path: Path) {
        TODO("Not yet implemented")
    }

}

// Configuration management
interface ConfigurationManager {
    fun getProjectConfiguration(projectPath: Path): ProjectConfiguration

    fun updateConfiguration(
        projectPath: Path,
        config: ProjectConfiguration,
    )
}

data class ProjectConfiguration(
    val projectPath: Path,
    val testsPath: Path,
    val ibConnection: String,
    val ibUser: String? = null,
    val ibPassword: String? = null,
    val platformVersion: String? = null,
    val logFile: Path? = null,
    val customSettings: Map<String, String> = emptyMap(),
)
