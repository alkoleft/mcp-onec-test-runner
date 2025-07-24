package io.github.alkoleft.mcp.infrastructure.process

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.YaXUnitConfigWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * YAXUnit configuration writer that generates JSON configuration files for test execution.
 * Supports different test execution modes and platform-specific settings.
 */
private val logger = KotlinLogging.logger {  }

@Component
class JsonYaXUnitConfigWriter : YaXUnitConfigWriter {

    private val objectMapper =
        ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

    override suspend fun writeConfig(
        request: TestExecutionRequest,
        outputPath: Path,
    ): Path =
        withContext(Dispatchers.IO) {
            logger.debug("Writing YAXUnit configuration to: $outputPath")

            try {
                // Ensure output directory exists
                Files.createDirectories(outputPath.parent)

                // Generate configuration based on request type
                val config = generateConfiguration(request)

                // Write configuration to file
                val configJson = objectMapper.writeValueAsString(config)
                Files.writeString(
                    outputPath,
                    configJson,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                )

                logger.info("YAXUnit configuration written successfully: $outputPath")
                return@withContext outputPath
            } catch (e: Exception) {
                logger.error("Failed to write YAXUnit configuration", e)
                throw TestExecutionError.TestRunFailed("Configuration write failed: ${e.message}")
            }
        }

    override suspend fun createTempConfig(request: TestExecutionRequest): Path =
        withContext(Dispatchers.IO) {
            val tempDir = request.projectPath.resolve(".yaxunit").resolve("temp")
            Files.createDirectories(tempDir)

            val tempConfigFile = tempDir.resolve("yaxunit-config-${System.currentTimeMillis()}.json")

            return@withContext writeConfig(request, tempConfigFile)
        }

    /**
     * Generates YAXUnit configuration based on the test execution request
     */
    private suspend fun generateConfiguration(request: TestExecutionRequest): YaXUnitConfiguration =
        withContext(Dispatchers.IO) {
            when (request) {
                is RunAllTestsRequest -> generateAllTestsConfig(request)
                is RunModuleTestsRequest -> generateModuleTestsConfig(request)
                is RunListTestsRequest -> generateListTestsConfig(request)
            }
        }

    /**
     * Generates configuration for running all tests
     */
    private suspend fun generateAllTestsConfig(request: RunAllTestsRequest): YaXUnitConfiguration =
        withContext(Dispatchers.IO) {
            val testModules = discoverTestModules(request.testsPath)

            YaXUnitConfiguration(
                version = "1.0",
                testRunSettings =
                    TestRunSettings(
                        executionMode = "all",
                        timeout = 30 * 60, // 30 minutes in seconds
                        outputFormat = "json",
                        reportPath =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("test-report.json")
                                .toString(),
                        parallel = false, // For safety, start with sequential execution
                    ),
                testSources =
                    TestSources(
                        baseDir = request.testsPath.toString(),
                        includes = listOf("**/*Test*.bsl", "**/*Тест*.bsl"),
                        excludes = listOf("**/temp/**", "**/build/**"),
                    ),
                testModules =
                    testModules.map { module ->
                        TestModule(
                            name = module.name,
                            path = module.path,
                            enabled = true,
                            timeout = 10 * 60, // 10 minutes per module
                            tests = module.tests,
                        )
                    },
                reporting =
                    ReportingConfig(
                        generateJUnit = true,
                        generateJson = true,
                        junitPath =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("junit-report.xml")
                                .toString(),
                        jsonPath =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("test-report.json")
                                .toString(),
                        includeSystemOut = true,
                        includeStackTrace = true,
                    ),
                environment =
                    EnvironmentConfig(
                        workingDirectory = request.projectPath.toString(),
                        tempDirectory =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("temp")
                                .toString(),
                        locale = System.getProperty("user.language") ?: "ru",
                        codepage = "UTF-8",
                    ),
            )
        }

    /**
     * Generates configuration for running specific module tests
     */
    private suspend fun generateModuleTestsConfig(request: RunModuleTestsRequest): YaXUnitConfiguration =
        withContext(Dispatchers.IO) {
            val allModules = discoverTestModules(request.testsPath)
            val targetModule =
                allModules.find { it.name.equals(request.moduleName, ignoreCase = true) }
                    ?: throw TestExecutionError.TestRunFailed("Test module not found: ${request.moduleName}")

            YaXUnitConfiguration(
                version = "1.0",
                testRunSettings =
                    TestRunSettings(
                        executionMode = "module",
                        timeout = 20 * 60, // 20 minutes
                        outputFormat = "json",
                        reportPath =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("test-report.json")
                                .toString(),
                        parallel = false,
                    ),
                testSources =
                    TestSources(
                        baseDir = request.testsPath.toString(),
                        includes = listOf("**/${request.moduleName}*.bsl", "**/${request.moduleName}*/**/*.bsl"),
                        excludes = listOf("**/temp/**", "**/build/**"),
                    ),
                testModules =
                    listOf(
                        TestModule(
                            name = targetModule.name,
                            path = targetModule.path,
                            enabled = true,
                            timeout = 15 * 60, // 15 minutes for single module
                            tests = targetModule.tests,
                        ),
                    ),
                reporting =
                    ReportingConfig(
                        generateJUnit = true,
                        generateJson = true,
                        junitPath =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("junit-report.xml")
                                .toString(),
                        jsonPath =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("test-report.json")
                                .toString(),
                        includeSystemOut = true,
                        includeStackTrace = true,
                    ),
                environment =
                    EnvironmentConfig(
                        workingDirectory = request.projectPath.toString(),
                        tempDirectory =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("temp")
                                .toString(),
                        locale = System.getProperty("user.language") ?: "ru",
                        codepage = "UTF-8",
                    ),
            )
        }

    /**
     * Generates configuration for running specific tests from a list
     */
    private suspend fun generateListTestsConfig(request: RunListTestsRequest): YaXUnitConfiguration =
        withContext(Dispatchers.IO) {
            val allModules = discoverTestModules(request.testsPath)
            val filteredModules = filterModulesForTests(allModules, request.testNames)

            YaXUnitConfiguration(
                version = "1.0",
                testRunSettings =
                    TestRunSettings(
                        executionMode = "list",
                        timeout = 15 * 60, // 15 minutes for specific tests
                        outputFormat = "json",
                        reportPath =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("test-report.json")
                                .toString(),
                        parallel = false,
                    ),
                testSources =
                    TestSources(
                        baseDir = request.testsPath.toString(),
                        includes = generateIncludesForTests(request.testNames),
                        excludes = listOf("**/temp/**", "**/build/**"),
                    ),
                testModules = filteredModules,
                reporting =
                    ReportingConfig(
                        generateJUnit = true,
                        generateJson = true,
                        junitPath =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("junit-report.xml")
                                .toString(),
                        jsonPath =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("test-report.json")
                                .toString(),
                        includeSystemOut = true,
                        includeStackTrace = true,
                    ),
                environment =
                    EnvironmentConfig(
                        workingDirectory = request.projectPath.toString(),
                        tempDirectory =
                            request.projectPath
                                .resolve(".yaxunit")
                                .resolve("temp")
                                .toString(),
                        locale = System.getProperty("user.language") ?: "ru",
                        codepage = "UTF-8",
                    ),
            )
        }

    /**
     * Discovers available test modules in the tests directory
     */
    private suspend fun discoverTestModules(testsPath: Path): List<DiscoveredTestModule> =
        withContext(Dispatchers.IO) {
            if (!Files.exists(testsPath)) {
                logger.warn("Tests directory does not exist: $testsPath")
                return@withContext emptyList()
            }

            val modules = mutableListOf<DiscoveredTestModule>()

            try {
                val files =
                    Files
                        .walk(testsPath, 3) // Limit depth to avoid deep recursion
                        .filter { Files.isRegularFile(it) }
                        .filter { it.toString().endsWith(".bsl", ignoreCase = true) }
                        .filter { isTestFile(it) }
                        .toList()

                for (testFile in files) {
                    val moduleName = extractModuleName(testFile, testsPath)
                    val tests = extractTestMethods(testFile)

                    if (tests.isNotEmpty()) {
                        modules.add(
                            DiscoveredTestModule(
                                name = moduleName,
                                path = testFile.toString(),
                                tests = tests,
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to discover test modules in: $testsPath", e)
            }

            logger.debug("Discovered ${modules.size} test modules in: $testsPath")
            modules
        }

    /**
     * Checks if a file is a test file based on naming conventions
     */
    private fun isTestFile(file: Path): Boolean {
        val fileName = file.fileName.toString().lowercase()
        return fileName.contains("test") || fileName.contains("тест")
    }

    /**
     * Extracts module name from test file path
     */
    private fun extractModuleName(
        testFile: Path,
        testsPath: Path,
    ): String {
        val relativePath = testsPath.relativize(testFile)
        val pathParts = relativePath.toString().split("/", "\\")

        return when {
            pathParts.size > 1 -> pathParts[0] // Use directory name
            else -> testFile.fileName.toString().substringBeforeLast(".") // Use filename
        }
    }

    /**
     * Extracts test method names from a test file (simplified implementation)
     */
    private suspend fun extractTestMethods(testFile: Path): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val content = Files.readString(testFile)
                val testMethods = mutableListOf<String>()

                // Simple regex to find test methods (1C/BSL syntax)
                val testMethodPattern = Regex("""(?i)(?:процедура|procedure)\s+(\w*[Tt]ест\w*|Test\w*)\s*\(""")

                testMethodPattern.findAll(content).forEach { match ->
                    val methodName = match.groupValues[1]
                    if (methodName.isNotBlank()) {
                        testMethods.add(methodName)
                    }
                }

                testMethods
            } catch (e: Exception) {
                logger.debug("Failed to extract test methods from: $testFile", e)
                emptyList()
            }
        }

    /**
     * Filters modules to include only those containing specified tests
     */
    private fun filterModulesForTests(
        allModules: List<DiscoveredTestModule>,
        testNames: List<String>,
    ): List<TestModule> =
        allModules.mapNotNull { module ->
            val relevantTests =
                module.tests.filter { test ->
                    testNames.any { targetTest ->
                        test.contains(targetTest, ignoreCase = true) || targetTest.contains(test, ignoreCase = true)
                    }
                }

            if (relevantTests.isNotEmpty()) {
                TestModule(
                    name = module.name,
                    path = module.path,
                    enabled = true,
                    timeout = 5 * 60, // 5 minutes per module for specific tests
                    tests = relevantTests,
                )
            } else {
                null
            }
        }

    /**
     * Generates include patterns for specific tests
     */
    private fun generateIncludesForTests(testNames: List<String>): List<String> =
        testNames.flatMap { testName ->
            listOf(
                "**/*$testName*.bsl",
                "**/*$testName*/**/*.bsl",
            )
        }
}

// Configuration data structures
data class YaXUnitConfiguration(
    val version: String,
    val testRunSettings: TestRunSettings,
    val testSources: TestSources,
    val testModules: List<TestModule>,
    val reporting: ReportingConfig,
    val environment: EnvironmentConfig,
)

data class TestRunSettings(
    val executionMode: String,
    val timeout: Int, // in seconds
    val outputFormat: String,
    val reportPath: String,
    val parallel: Boolean,
)

data class TestSources(
    val baseDir: String,
    val includes: List<String>,
    val excludes: List<String>,
)

data class TestModule(
    val name: String,
    val path: String,
    val enabled: Boolean,
    val timeout: Int, // in seconds
    val tests: List<String>,
)

data class ReportingConfig(
    val generateJUnit: Boolean,
    val generateJson: Boolean,
    val junitPath: String,
    val jsonPath: String,
    val includeSystemOut: Boolean,
    val includeStackTrace: Boolean,
)

data class EnvironmentConfig(
    val workingDirectory: String,
    val tempDirectory: String,
    val locale: String,
    val codepage: String,
)

// Internal data structures
private data class DiscoveredTestModule(
    val name: String,
    val path: String,
    val tests: List<String>,
)
