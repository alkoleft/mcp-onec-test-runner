package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.common.BuildMode
import io.github.alkoleft.mcp.application.actions.common.BuildResult
import io.github.alkoleft.mcp.application.actions.common.ChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.common.ChangeAnalysisResult
import io.github.alkoleft.mcp.application.actions.common.LaunchAction
import io.github.alkoleft.mcp.application.actions.common.LaunchResult
import io.github.alkoleft.mcp.application.actions.change.SourceSetChanges
import io.github.alkoleft.mcp.application.actions.test.yaxunit.ChangeType
import io.github.alkoleft.mcp.application.core.UtilityType
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.BuildProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.ProjectFormat
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilities
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.CommandExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.alkoleft.mcp.infrastructure.storage.HashStorage
import io.github.alkoleft.mcp.infrastructure.storage.SourceSetContext
import io.github.alkoleft.mcp.infrastructure.yaxunit.ReportParser
import io.github.alkoleft.mcp.infrastructure.yaxunit.YaXUnitRunner
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

class LauncherServiceTest {
    private lateinit var buildAction: io.github.alkoleft.mcp.application.actions.common.BuildAction
    private lateinit var changeAnalysisAction: ChangeAnalysisAction
    private lateinit var launchAction: LaunchAction
    private lateinit var reportParser: ReportParser
    private lateinit var yaxUnitRunner: YaXUnitRunner
    private lateinit var properties: ApplicationProperties
    private lateinit var sourceSetsService: SourceSetsService
    private lateinit var edtExecutor: CommandExecutor
    private lateinit var capturedCommands: MutableList<List<String>>

    @BeforeEach
    fun setUp() {
        buildAction = mockk()
        changeAnalysisAction = mockk()
        launchAction = mockk()
        reportParser = mockk()
        yaxUnitRunner = mockk()
        sourceSetsService = mockk()
        edtExecutor = mockk()
        capturedCommands = mutableListOf()

        every { buildAction.runPartial(any(), any(), any(), any()) } returns
            BuildResult(
                message = "ok",
                success = true,
                sourceSet = emptyMap(),
                steps = emptyList(),
            )
        every { launchAction.run(any()) } returns LaunchResult(success = true, message = "ok", errors = emptyList())
        every { edtExecutor.execute(capture(capturedCommands)) } answers { successProcessResult("ok") }
    }

    @Test
    fun `build should convert configuration and extension in full mode`(
        @TempDir tempDir: Path,
    ) {
        properties = createProperties(tempDir)
        val service = createService()
        val edtContext = createEdtSourceSetContext(tempDir)
        val designerContext = createDesignerSourceSetContext(tempDir)

        every { sourceSetsService.getEdtSourceSet() } returns edtContext
        every { sourceSetsService.getDesignerSourceSet() } returns designerContext
        every { changeAnalysisAction.run(edtContext) } returns changeAnalysisResult(tempDir, "uhmrg", "YAXUNIT")
        every { changeAnalysisAction.run(designerContext) } returns noChanges()
        every { changeAnalysisAction.saveSourceSetState(any(), any(), any(), any()) } returns true

        val result = service.build(mode = BuildMode.FULL)

        assertTrue(result.success)
        assertEquals(2, capturedCommands.size)
        assertTrue(capturedCommands.any { it.containsAll(listOf("export", "--project-name", "uhmrg")) })
        assertTrue(capturedCommands.any { it.containsAll(listOf("export", "--project-name", "YAXUNIT")) })
        assertFalse(result.steps.any { it.message.contains("Конвертация основной конфигурации EDT: пропущена") })
        verify(exactly = 2) { changeAnalysisAction.saveSourceSetState(edtContext, any(), any(), true) }
    }

    @Test
    fun `build should convert only extension in skip main configuration mode`(
        @TempDir tempDir: Path,
    ) {
        properties = createProperties(tempDir)
        val service = createService()
        val edtContext = createEdtSourceSetContext(tempDir)
        val designerContext = createDesignerSourceSetContext(tempDir)

        every { sourceSetsService.getEdtSourceSet() } returns edtContext
        every { sourceSetsService.getDesignerSourceSet() } returns designerContext
        every { changeAnalysisAction.run(edtContext) } returns changeAnalysisResult(tempDir, "uhmrg", "YAXUNIT")
        every { changeAnalysisAction.run(designerContext) } returns noChanges()
        every { changeAnalysisAction.saveSourceSetState(any(), any(), any(), any()) } returns true

        val result = service.build(mode = BuildMode.SKIP_MAIN_CONFIGURATION)

        assertTrue(result.success)
        assertEquals(1, capturedCommands.size)
        assertTrue(capturedCommands.single().containsAll(listOf("export", "--project-name", "YAXUNIT")))
        assertFalse(capturedCommands.single().contains("uhmrg"))
        assertTrue(result.steps.any { it.message.contains("Конвертация основной конфигурации EDT: пропущена") })
        verify(exactly = 1) {
            changeAnalysisAction.saveSourceSetState(
                edtContext,
                match { it.sourceSetName == "YAXUNIT" },
                any(),
                true,
            )
        }
    }

    @Test
    fun `build should skip conversion completely when only configuration changed in skip mode`(
        @TempDir tempDir: Path,
    ) {
        properties = createProperties(tempDir)
        val service = createService()
        val edtContext = createEdtSourceSetContext(tempDir)
        val designerContext = createDesignerSourceSetContext(tempDir)

        every { sourceSetsService.getEdtSourceSet() } returns edtContext
        every { sourceSetsService.getDesignerSourceSet() } returns designerContext
        every { changeAnalysisAction.run(edtContext) } returns changeAnalysisResult(tempDir, "uhmrg")
        every { changeAnalysisAction.run(designerContext) } returns noChanges()

        val result = service.build(mode = BuildMode.SKIP_MAIN_CONFIGURATION)

        assertTrue(result.success)
        assertTrue(capturedCommands.isEmpty())
        assertTrue(result.steps.any { it.message.contains("Конвертация основной конфигурации EDT: пропущена") })
        verify(exactly = 0) { changeAnalysisAction.saveSourceSetState(edtContext, any(), any(), any()) }
    }

    @Test
    fun `build should convert changed yaxunit extension in skip mode`(
        @TempDir tempDir: Path,
    ) {
        properties = createProperties(tempDir)
        val service = createService()
        val edtContext = createEdtSourceSetContext(tempDir)
        val designerContext = createDesignerSourceSetContext(tempDir)

        every { sourceSetsService.getEdtSourceSet() } returns edtContext
        every { sourceSetsService.getDesignerSourceSet() } returns designerContext
        every { changeAnalysisAction.run(edtContext) } returns changeAnalysisResult(tempDir, "YAXUNIT")
        every { changeAnalysisAction.run(designerContext) } returns noChanges()
        every { changeAnalysisAction.saveSourceSetState(any(), any(), any(), any()) } returns true

        val result = service.build(mode = BuildMode.SKIP_MAIN_CONFIGURATION)

        assertTrue(result.success)
        assertEquals(1, capturedCommands.size)
        assertTrue(capturedCommands.single().containsAll(listOf("export", "--project-name", "YAXUNIT")))
        assertFalse(result.steps.any { it.message.contains("Конвертация основной конфигурации EDT: пропущена") })
    }

    private fun createService(): LauncherService {
        val platformUtilities = mockk<PlatformUtilities>()
        every { platformUtilities.executor(UtilityType.EDT_CLI) } returns edtExecutor
        val platformDsl = PlatformDsl(platformUtilities, mockk(relaxed = true))

        return LauncherService(
            buildAction = buildAction,
            changeAnalysisAction = changeAnalysisAction,
            launchAction = launchAction,
            platformDsl = platformDsl,
            reportParser = reportParser,
            yaxUnitRunner = yaxUnitRunner,
            properties = properties,
            sourceSetsService = sourceSetsService,
        )
    }

    private fun createProperties(baseDir: Path): ApplicationProperties {
        baseDir.resolve("uhmrg").createDirectories()
        baseDir.resolve("src/cfe/uhmrg.YAXUNIT").createDirectories()

        return ApplicationProperties(
            id = "uhmrg-test",
            format = ProjectFormat.EDT,
            basePath = baseDir,
            sourceSet =
                SourceSet(
                    basePath = baseDir,
                    items =
                        listOf(
                            SourceSetItem(
                                path = "uhmrg",
                                name = "uhmrg",
                                type = SourceSetType.CONFIGURATION,
                                purpose = setOf(SourceSetPurpose.MAIN),
                            ),
                            SourceSetItem(
                                path = "src/cfe/uhmrg.YAXUNIT",
                                name = "YAXUNIT",
                                type = SourceSetType.EXTENSION,
                                purpose = setOf(SourceSetPurpose.TESTS, SourceSetPurpose.YAXUNIT),
                            ),
                        ),
                ),
            connection = ConnectionProperties(connectionString = "File='test';"),
            tools = ToolsProperties(),
            build = BuildProperties(),
            platformVersion = "8.3.27.1989",
        )
    }

    private fun createEdtSourceSetContext(baseDir: Path): SourceSetContext =
        SourceSetContext(
            sourceSet = properties.sourceSet,
            hashStorage = mockk<HashStorage>(),
            format = ProjectFormat.EDT,
            isDesignerSource = false,
        )

    private fun createDesignerSourceSetContext(baseDir: Path): SourceSetContext =
        SourceSetContext(
            sourceSet =
                SourceSet(
                    basePath = baseDir.resolve("designer"),
                    items =
                        listOf(
                            SourceSetItem(
                                path = "uhmrg",
                                name = "uhmrg",
                                type = SourceSetType.CONFIGURATION,
                                purpose = setOf(SourceSetPurpose.MAIN),
                            ),
                            SourceSetItem(
                                path = "YAXUNIT",
                                name = "YAXUNIT",
                                type = SourceSetType.EXTENSION,
                                purpose = setOf(SourceSetPurpose.TESTS, SourceSetPurpose.YAXUNIT),
                            ),
                        ),
                ),
            hashStorage = mockk<HashStorage>(),
            format = ProjectFormat.EDT,
            isDesignerSource = true,
        )

    private fun changeAnalysisResult(
        tempDir: Path,
        vararg sourceSetNames: String,
    ): ChangeAnalysisResult {
        val sourceSetChanges =
            sourceSetNames.associateWith { name ->
                val changedFile = tempDir.resolve("$name.xml")
                changedFile.writeText("<$name/>")
                SourceSetChanges(
                    sourceSetName = name,
                    sourceSetPath = tempDir.toString(),
                    changedFiles = setOf(changedFile),
                    changeTypes = mapOf(changedFile to (ChangeType.MODIFIED to "hash-$name")),
                )
            }

        return ChangeAnalysisResult(
            hasChanges = sourceSetChanges.isNotEmpty(),
            changedFiles = sourceSetChanges.values.flatMap { it.changedFiles }.toSet(),
            changeTypes = emptyMap(),
            sourceSetChanges = sourceSetChanges,
            steps = emptyList(),
            timestamp = 1L,
        )
    }

    private fun noChanges() =
        ChangeAnalysisResult(
            hasChanges = false,
            changedFiles = emptySet(),
            changeTypes = emptyMap(),
            sourceSetChanges = emptyMap(),
            steps = emptyList(),
            timestamp = 2L,
        )

    private fun successProcessResult(output: String) =
        ProcessResult(
            success = true,
            output = output,
            error = null,
            exitCode = 0,
            duration = Duration.ZERO,
        )
}
