package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.change.SourceSetChanges
import io.github.alkoleft.mcp.application.actions.common.BuildMode
import io.github.alkoleft.mcp.application.actions.test.yaxunit.ChangeType
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.BuildProperties
import io.github.alkoleft.mcp.configuration.properties.BuilderType
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.ProjectFormat
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.alkoleft.mcp.infrastructure.utility.PartialLoadListGenerator
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

class AbstractBuildActionTest {
    @Test
    fun `runPartial should skip main configuration and still load extension`(
        @TempDir tempDir: Path,
    ) {
        val properties = createProperties(tempDir)
        val sourceSet = properties.sourceSet
        val action = FakeBuildAction()

        val extensionPath = tempDir.resolve("tests")
        val changedFile = extensionPath.resolve("CommonModules/Test.xml")
        Files.createDirectories(changedFile.parent)
        Files.writeString(changedFile, "<test/>")

        val result =
            action.runPartial(
                properties = properties,
                sourceSet = sourceSet,
                sourceSetChanges =
                    mapOf(
                        "tests" to
                            SourceSetChanges(
                                sourceSetName = "tests",
                                sourceSetPath = extensionPath.toString(),
                                changedFiles = setOf(changedFile),
                                changeTypes = mapOf(changedFile to (ChangeType.MODIFIED to "hash")),
                            ),
                    ),
                mode = BuildMode.SKIP_MAIN_CONFIGURATION,
            )

        assertTrue(result.success)
        assertTrue(action.loadedConfigurations.isEmpty())
        assertEquals(listOf("tests"), action.loadedExtensions)
        assertEquals(1, action.updateDbCalls)
        assertTrue(result.steps.any { it.message.contains("пропущена по параметру skipMainConfigurationUpdate") })
    }

    @Test
    fun `runPartial should keep default full mode behavior`(
        @TempDir tempDir: Path,
    ) {
        val properties = createProperties(tempDir)
        val sourceSet = properties.sourceSet
        val action = FakeBuildAction()

        val configPath = tempDir.resolve("configuration")
        val changedFile = configPath.resolve("Configuration.xml")
        Files.writeString(changedFile, "<configuration/>")

        val result =
            action.runPartial(
                properties = properties,
                sourceSet = sourceSet,
                sourceSetChanges =
                    mapOf(
                        "main" to
                            SourceSetChanges(
                                sourceSetName = "main",
                                sourceSetPath = configPath.toString(),
                                changedFiles = setOf(changedFile),
                                changeTypes = mapOf(changedFile to (ChangeType.MODIFIED to "hash")),
                            ),
                    ),
                mode = BuildMode.FULL,
            )

        assertTrue(result.success)
        assertEquals(listOf("main"), action.loadedConfigurations)
        assertFalse(result.steps.any { it.message.contains("пропущена по параметру skipMainConfigurationUpdate") })
    }

    private fun createProperties(baseDir: Path): ApplicationProperties {
        val configurationDir = baseDir.resolve("configuration")
        val testsDir = baseDir.resolve("tests")
        Files.createDirectories(configurationDir)
        Files.createDirectories(testsDir)

        return ApplicationProperties(
            id = "test-project",
            format = ProjectFormat.DESIGNER,
            basePath = baseDir,
            sourceSet =
                SourceSet(
                    basePath = baseDir,
                    items =
                        listOf(
                            SourceSetItem(
                                path = "configuration",
                                name = "main",
                                type = SourceSetType.CONFIGURATION,
                                purpose = setOf(SourceSetPurpose.MAIN),
                            ),
                            SourceSetItem(
                                path = "tests",
                                name = "tests",
                                type = SourceSetType.EXTENSION,
                                purpose = setOf(SourceSetPurpose.TESTS, SourceSetPurpose.YAXUNIT),
                            ),
                        ),
                ),
            connection = ConnectionProperties(connectionString = "File='test';"),
            tools = ToolsProperties(builder = BuilderType.IBCMD),
            build = BuildProperties(partialLoadThreshold = 20),
            platformVersion = "8.3.27.1989",
        )
    }

    private class FakeBuildAction :
        AbstractBuildAction(
            dsl = mockk<PlatformDsl>(),
            partialLoadListGenerator = PartialLoadListGenerator(),
        ) {
        val loadedConfigurations = mutableListOf<String>()
        val loadedExtensions = mutableListOf<String>()
        var updateDbCalls = 0

        override fun initDsl(properties: ApplicationProperties) = Unit

        override fun loadConfiguration(
            name: String,
            path: Path,
        ): ProcessResult {
            loadedConfigurations += name
            return successResult("config:$name")
        }

        override fun loadConfigurationPartial(
            name: String,
            path: Path,
            changedFiles: Set<Path>,
        ): ProcessResult {
            loadedConfigurations += name
            return successResult("config-partial:$name")
        }

        override fun loadExtension(
            name: String,
            path: Path,
        ): ProcessResult {
            loadedExtensions += name
            return successResult("extension:$name")
        }

        override fun loadExtensionPartial(
            name: String,
            path: Path,
            changedFiles: Set<Path>,
        ): ProcessResult {
            loadedExtensions += name
            return successResult("extension-partial:$name")
        }

        override fun updateDb(): ProcessResult {
            updateDbCalls += 1
            return successResult("update-db")
        }

        private fun successResult(output: String) =
            ProcessResult(
                success = true,
                output = output,
                error = null,
                exitCode = 0,
                duration = Duration.ZERO,
            )
    }
}
