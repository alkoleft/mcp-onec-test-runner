package io.github.alkoleft.mcp.infrastructure.platform.dsl.edt

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
