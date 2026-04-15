package io.github.alkoleft.mcp.server

import io.github.alkoleft.mcp.application.actions.common.ActionStepResult
import io.github.alkoleft.mcp.application.actions.common.BuildMode
import io.github.alkoleft.mcp.application.actions.common.RunTestResult
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunAllTestsRequest
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunModuleTestsRequest
import io.github.alkoleft.mcp.application.services.DumpService
import io.github.alkoleft.mcp.application.services.LauncherService
import io.github.alkoleft.mcp.application.services.SyntaxCheckService
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.log.LogManager
import io.github.alkoleft.mcp.server.dto.McpTestResponse
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.clearMocks
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.time.Duration

class McpServerTest {
    private companion object {
        const val INPUT_MODULE_NAME: String = "TestModule"
    }

    private lateinit var launcherService: LauncherService
    private lateinit var syntaxCheckService: SyntaxCheckService
    private lateinit var dumpService: DumpService
    private lateinit var properties: ApplicationProperties
    private lateinit var logManager: LogManager
    private lateinit var server: McpServer

    @BeforeEach
    fun setUp(): Unit {
        launcherService = mockk()
        syntaxCheckService = mockk()
        dumpService = mockk()
        properties = mockk()
        logManager = mockk()

        every { properties.cleanLogBeforeExecution } returns false
        every { logManager.cleanLogIfEnabled(any()) } just runs
        every { launcherService.runTests(any()) } returns successfulTestResult()

        server = McpServer(launcherService, syntaxCheckService, dumpService, properties, logManager)
    }

    @Test
    fun `run all tests should pass skip main configuration mode`(): Unit {
        val actualResult: McpTestResponse = server.runAllTests(skipMainConfigurationUpdate = true)

        assertTrue(actualResult.success)
        verify(exactly = 1) {
            launcherService.runTests(
                match<RunAllTestsRequest> {
                    it.buildMode == BuildMode.SKIP_MAIN_CONFIGURATION
                },
            )
        }
    }

    @Test
    fun `run module tests should pass skip main configuration mode`(): Unit {
        val inputModuleName: String = INPUT_MODULE_NAME
        val actualResult: McpTestResponse =
            server.runModuleTests(moduleName = inputModuleName, skipMainConfigurationUpdate = true)

        assertTrue(actualResult.success)
        verify(exactly = 1) {
            launcherService.runTests(
                match<RunModuleTestsRequest> {
                    it.moduleName == inputModuleName && it.buildMode == BuildMode.SKIP_MAIN_CONFIGURATION
                },
            )
        }
    }

    @Test
    fun `run all tests should use full mode when skip flag is false`(): Unit = verifyRunAllTestsFullMode(skipMainConfigurationUpdate = false)

    @Test
    fun `run all tests should use full mode when skip flag is null`(): Unit = verifyRunAllTestsFullMode(skipMainConfigurationUpdate = null)

    @Test
    fun `run module tests should use full mode when skip flag is false`(): Unit =
        verifyRunModuleTestsFullMode(moduleName = INPUT_MODULE_NAME, skipMainConfigurationUpdate = false)

    @Test
    fun `run module tests should use full mode when skip flag is null`(): Unit =
        verifyRunModuleTestsFullMode(moduleName = INPUT_MODULE_NAME, skipMainConfigurationUpdate = null)

    private fun verifyRunAllTestsFullMode(skipMainConfigurationUpdate: Boolean?): Unit {
        clearMocks(launcherService, answers = false, recordedCalls = true)
        every { launcherService.runTests(any()) } returns successfulTestResult()

        val actualResult: McpTestResponse = server.runAllTests(skipMainConfigurationUpdate = skipMainConfigurationUpdate)

        assertTrue(actualResult.success)
        verify(exactly = 1) {
            launcherService.runTests(
                match<RunAllTestsRequest> {
                    it.buildMode == BuildMode.FULL
                },
            )
        }
    }

    private fun verifyRunModuleTestsFullMode(
        moduleName: String,
        skipMainConfigurationUpdate: Boolean?,
    ): Unit {
        clearMocks(launcherService, answers = false, recordedCalls = true)
        every { launcherService.runTests(any()) } returns successfulTestResult()

        val inputModuleName: String = moduleName
        val actualResult: McpTestResponse =
            server.runModuleTests(
                moduleName = inputModuleName,
                skipMainConfigurationUpdate = skipMainConfigurationUpdate,
            )

        assertTrue(actualResult.success)
        verify(exactly = 1) {
            launcherService.runTests(
                match<RunModuleTestsRequest> {
                    it.moduleName == inputModuleName && it.buildMode == BuildMode.FULL
                },
            )
        }
    }

    private fun successfulTestResult(): RunTestResult =
        RunTestResult(
            success = true,
            duration = Duration.ZERO,
            message = "ok",
            errors = emptyList(),
            steps = listOf(ActionStepResult("ok", true, duration = Duration.ZERO)),
            report = null,
            reportPath = null,
            enterpriseLogPath = null,
            logPath = null,
        )
}
