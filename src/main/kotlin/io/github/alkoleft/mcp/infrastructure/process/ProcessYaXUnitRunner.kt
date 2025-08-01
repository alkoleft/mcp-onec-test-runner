package io.github.alkoleft.mcp.infrastructure.process

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.YaXUnitExecutionResult
import io.github.alkoleft.mcp.core.modules.YaXUnitRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * YAXUnit test runner that executes 1C:Enterprise processes for running tests.
 * Handles process execution, output capture, and result parsing.
 */
private val logger = KotlinLogging.logger {  }

@Component
class ProcessYaXUnitRunner : YaXUnitRunner {

    // Execution timeouts
    private val defaultTestTimeout = Duration.ofMinutes(30)
    private val processStartTimeout = Duration.ofSeconds(30)

    override suspend fun executeTests(
        utilityLocation: UtilityLocation,
        configPath: Path,
        request: TestExecutionRequest,
    ): YaXUnitExecutionResult =
        withContext(Dispatchers.IO) {
            val startTime = Instant.now()
            logger.info { "Starting YAXUnit test execution with utility: ${utilityLocation.executablePath}" }

            try {
                // Prepare execution parameters
                val executionParams = prepareExecutionParameters(utilityLocation, configPath, request)
                logger.debug { "Execution command: ${executionParams.command.joinToString(" ")}" }

                // Execute the 1C process
                val processResult = executeProcess(executionParams)

                val duration = Duration.between(startTime, Instant.now())
                logger.info { "YAXUnit execution completed in ${duration.toMillis()}ms with exit code: ${processResult.exitCode}" }

                // Determine report path
                val reportPath = findGeneratedReport(request.projectPath, request.testsPath)

                YaXUnitExecutionResult(
                    success = processResult.exitCode == 0,
                    reportPath = reportPath,
                    exitCode = processResult.exitCode,
                    standardOutput = processResult.standardOutput,
                    errorOutput = processResult.errorOutput,
                    duration = duration,
                )
            } catch (e: Exception) {
                logger.error(e) { "YAXUnit execution failed" }

                val duration = Duration.between(startTime, Instant.now())
                YaXUnitExecutionResult(
                    success = false,
                    reportPath = null,
                    exitCode = -1,
                    standardOutput = "",
                    errorOutput = "Execution failed: ${e.message}",
                    duration = duration,
                )
            }
        }

    /**
     * Prepares execution parameters based on platform and request type
     */
    private suspend fun prepareExecutionParameters(
        utilityLocation: UtilityLocation,
        configPath: Path,
        request: TestExecutionRequest,
    ): ExecutionParameters =
        withContext(Dispatchers.IO) {
            val command = mutableListOf<String>()
            val workingDirectory = request.projectPath.toFile()

            // Base 1C command
            command.add(utilityLocation.executablePath.toString())

            // Connection parameters
            when {
                request.ibConnection.startsWith("/F") -> {
                    // File database
                    command.add("ENTERPRISE")
                    command.add(request.ibConnection)
                }

                request.ibConnection.contains("Srvr=") -> {
                    // Server database
                    command.add("ENTERPRISE")
                    command.add("/S${request.ibConnection}")
                }

                else -> {
                    throw TestExecutionError.TestRunFailed("Invalid database connection format: ${request.ibConnection}")
                }
            }

            // Authentication parameters (if provided)
            val ibUser = System.getenv("IB_USER")
            val ibPassword = System.getenv("IB_PWD")

            if (!ibUser.isNullOrBlank()) {
                command.add("/N$ibUser")
                if (!ibPassword.isNullOrBlank()) {
                    command.add("/P$ibPassword")
                }
            }

            // YAXUnit execution parameters
            command.add("/C")
            command.add("RunUnitTests")

            // Configuration file parameter
            if (Files.exists(configPath)) {
                command.add("/TestConfig:${configPath.toAbsolutePath()}")
            }

            // Output parameters
            val reportPath = request.projectPath.resolve(".yaxunit").resolve("test-report.json")
            Files.createDirectories(reportPath.parent)
            command.add("/Out:${reportPath.toAbsolutePath()}")

            // Platform-specific adjustments
            adjustCommandForPlatform(command, utilityLocation.platformType)

            ExecutionParameters(
                command = command,
                workingDirectory = workingDirectory,
                timeout = defaultTestTimeout,
                environmentVariables = prepareEnvironmentVariables(),
            )
        }

    /**
     * Adjusts command for platform-specific requirements
     */
    private fun adjustCommandForPlatform(
        command: MutableList<String>,
        platformType: PlatformType,
    ) {
        when (platformType) {
            PlatformType.WINDOWS -> {
                // Windows-specific adjustments
                command.add("/DisableStartupDialogs")
                command.add("/DisableStartupMessages")
            }

            PlatformType.LINUX, PlatformType.MACOS -> {
                // Unix-specific adjustments
                command.add("-DisableStartupDialogs")
                command.add("-DisableStartupMessages")
            }
        }
    }

    /**
     * Prepares environment variables for process execution
     */
    private fun prepareEnvironmentVariables(): Map<String, String> {
        val env = mutableMapOf<String, String>()

        // Preserve important system variables
        System.getenv("PATH")?.let { env["PATH"] = it }
        System.getenv("TEMP")?.let { env["TEMP"] = it }
        System.getenv("TMP")?.let { env["TMP"] = it }
        System.getenv("HOME")?.let { env["HOME"] = it }
        System.getenv("USER")?.let { env["USER"] = it }

        // 1C-specific environment variables
        System.getenv("V8_LOCALE")?.let { env["V8_LOCALE"] = it }
        System.getenv("V8_CODEPAGE")?.let { env["V8_CODEPAGE"] = it }

        return env
    }

    /**
     * Executes the 1C process with timeout and output capture
     */
    private suspend fun executeProcess(params: ExecutionParameters): ProcessExecutionResult =
        withContext(Dispatchers.IO) {
            logger.debug { "Executing process in directory: ${params.workingDirectory}" }
            logger.debug { "Process timeout: ${params.timeout.toMinutes()} minutes" }

            val processBuilder =
                ProcessBuilder(params.command)
                    .directory(params.workingDirectory)
                    .redirectErrorStream(false)

            // Set environment variables
            processBuilder.environment().putAll(params.environmentVariables)

            val process =
                try {
                    processBuilder.start()
                } catch (e: Exception) {
                    logger.error(e) { "Failed to start 1C process" }
                    throw TestExecutionError.TestRunFailed("Process start failed: ${e.message}")
                }

            // Monitor process execution with timeout
            val processCompleted =
                withTimeoutOrNull(params.timeout.toMillis()) {
                    process.waitFor(params.timeout.toMillis(), TimeUnit.MILLISECONDS)
                } ?: false

            if (!processCompleted) {
                logger.warn { "Process execution timed out after ${params.timeout.toMinutes()} minutes" }
                process.destroyForcibly()

                return@withContext ProcessExecutionResult(
                    exitCode = -2,
                    standardOutput = "Process execution timed out",
                    errorOutput = "Execution exceeded timeout of ${params.timeout.toMinutes()} minutes",
                )
            }

            // Capture output
            val standardOutput =
                try {
                    process.inputStream
                        .bufferedReader()
                        .readText()
                        .trim()
                } catch (e: Exception) {
                    logger.debug(e) { "Failed to read standard output" }
                    ""
                }

            val errorOutput =
                try {
                    process.errorStream
                        .bufferedReader()
                        .readText()
                        .trim()
                } catch (e: Exception) {
                    logger.debug(e) { "Failed to read error output" }
                    ""
                }

            val exitCode = process.exitValue()

            logger.debug { "Process completed with exit code: $exitCode" }
            if (standardOutput.isNotBlank()) {
                logger.debug { "Standard output: $standardOutput" }
            }
            if (errorOutput.isNotBlank()) {
                logger.debug { "Error output: $errorOutput" }
            }

            ProcessExecutionResult(
                exitCode = exitCode,
                standardOutput = standardOutput,
                errorOutput = errorOutput,
            )
        }

    /**
     * Finds the generated test report file
     */
    private suspend fun findGeneratedReport(
        projectPath: Path,
        testsPath: Path,
    ): Path? =
        withContext(Dispatchers.IO) {
            val possibleReportPaths =
                listOf(
                    projectPath.resolve(".yaxunit").resolve("test-report.json"),
                    projectPath.resolve(".yaxunit").resolve("test-results.json"),
                    testsPath.resolve("test-report.json"),
                    testsPath.resolve("test-results.json"),
                    projectPath.resolve("test-report.json"),
                    projectPath.resolve("test-results.json"),
                )

            for (reportPath in possibleReportPaths) {
                if (Files.exists(reportPath) && Files.size(reportPath) > 0) {
                    logger.debug { "Found test report at: $reportPath" }
                    return@withContext reportPath
                }
            }

            logger.warn { "No test report found in expected locations" }
            return@withContext null
        }

    /**
     * Validates that the utility can be executed
     */
    suspend fun validateUtilityAccess(utilityLocation: UtilityLocation): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val testCommand = listOf(utilityLocation.executablePath.toString(), "/?")

                val process =
                    ProcessBuilder(testCommand)
                        .redirectErrorStream(true)
                        .start()

                val completed = process.waitFor(5, TimeUnit.SECONDS)

                if (!completed) {
                    process.destroyForcibly()
                    logger.debug { "Utility validation timed out" }
                    return@withContext false
                }

                val exitCode = process.exitValue()
                logger.debug { "Utility validation completed with exit code: $exitCode" }

                // For 1C utilities, exit codes 0, 1, or 2 are typically acceptable for help command
                exitCode in 0..2
            } catch (e: Exception) {
                logger.debug(e) { "Utility validation failed" }
                false
            }
        }

    /**
     * Gets detailed information about the 1C platform version
     */
    suspend fun getPlatformInfo(utilityLocation: UtilityLocation): PlatformInfo? =
        withContext(Dispatchers.IO) {
            try {
                val versionCommand = listOf(utilityLocation.executablePath.toString(), "/?")

                val process =
                    ProcessBuilder(versionCommand)
                        .redirectErrorStream(false)
                        .start()

                val completed = process.waitFor(10, TimeUnit.SECONDS)

                if (!completed) {
                    process.destroyForcibly()
                    return@withContext null
                }

                val output = process.inputStream.bufferedReader().readText()

                // Parse version information from output
                val versionPattern = Regex("""(\d+\.\d+\.\d+\.\d+)""")
                val version = versionPattern.find(output)?.value

                PlatformInfo(
                    version = version,
                    platform = utilityLocation.platformType,
                    executablePath = utilityLocation.executablePath,
                    rawOutput = output,
                )
            } catch (e: Exception) {
                logger.debug(e) { "Failed to get platform info" }
                null
            }
        }
}

/**
 * Parameters for process execution
 */
private data class ExecutionParameters(
    val command: List<String>,
    val workingDirectory: File,
    val timeout: Duration,
    val environmentVariables: Map<String, String>,
)

/**
 * Result of process execution
 */
private data class ProcessExecutionResult(
    val exitCode: Int,
    val standardOutput: String,
    val errorOutput: String,
)

/**
 * Information about the 1C platform
 */
data class PlatformInfo(
    val version: String?,
    val platform: PlatformType,
    val executablePath: Path,
    val rawOutput: String,
)
