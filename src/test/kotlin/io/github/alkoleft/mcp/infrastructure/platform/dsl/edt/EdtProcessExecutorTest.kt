package io.github.alkoleft.mcp.infrastructure.platform.dsl.edt

import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

class EdtProcessExecutorTest {
    @Test
    fun `buildCommandArgs should prepend executable and workspace for non interactive edt mode`() {
        val executor =
            EdtProcessExecutor(
                executablePath = "C:\\1C\\1cedtcli.exe",
                workingDirectory = "F:\\EDT\\workspace",
                commandTimeoutMs = 300_000,
            )

        val args = executor.buildCommandArgs(listOf("export", "--project-name", "uhmrg"))

        assertEquals(
            listOf(
                "C:\\1C\\1cedtcli.exe",
                "-data",
                "F:\\EDT\\workspace",
                "-timeout",
                "300",
                "-command",
                "export",
                "--project-name",
                "uhmrg",
            ),
            args,
        )
    }

    @Test
    fun `buildCommandArgs should omit workspace when it is not configured`() {
        val executor =
            EdtProcessExecutor(
                executablePath = "C:\\1C\\1cedtcli.exe",
                workingDirectory = null,
            )

        val args = executor.buildCommandArgs(listOf("validate", "--file", "out.xml"))

        assertEquals(
            listOf(
                "C:\\1C\\1cedtcli.exe",
                "-command",
                "validate",
                "--file",
                "out.xml",
            ),
            args,
        )
    }

    @Test
    fun `execute should delegate built arguments to process executor`() {
        val processExecutor = mockk<ProcessExecutor>()
        every { processExecutor.execute(any()) } returns
            ProcessResult(
                success = true,
                output = "ok",
                error = null,
                exitCode = 0,
                duration = Duration.ZERO,
            )
        val executor =
            EdtProcessExecutor(
                executablePath = "C:\\1C\\1cedtcli.exe",
                workingDirectory = "F:\\EDT\\workspace",
                commandTimeoutMs = 1_500,
                processExecutor = processExecutor,
            )

        executor.execute(listOf("export", "--project-name", "uhmrg"))

        verify(exactly = 1) {
            processExecutor.execute(
                listOf(
                    "C:\\1C\\1cedtcli.exe",
                    "-data",
                    "F:\\EDT\\workspace",
                    "-timeout",
                    "2",
                    "-command",
                    "export",
                    "--project-name",
                    "uhmrg",
                ),
            )
        }
    }

    @Test
    fun `execute should round up timeout less than one second to one`() {
        val processExecutor = mockk<ProcessExecutor>()
        every { processExecutor.execute(any()) } returns
            ProcessResult(
                success = true,
                output = "ok",
                error = null,
                exitCode = 0,
                duration = Duration.ZERO,
            )
        val executor =
            EdtProcessExecutor(
                executablePath = "C:\\1C\\1cedtcli.exe",
                commandTimeoutMs = 1,
                processExecutor = processExecutor,
            )

        executor.execute(listOf("validate"))

        verify(exactly = 1) {
            processExecutor.execute(
                listOf(
                    "C:\\1C\\1cedtcli.exe",
                    "-timeout",
                    "1",
                    "-command",
                    "validate",
                ),
            )
        }
    }
}
