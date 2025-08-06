package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.BuilderType
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.file.Paths

@ExtendWith(MockitoExtension::class)
class BuildActionTest {

    @Mock
    private lateinit var mockDsl: PlatformUtilityDsl

    @Test
    fun `should handle ibcmd build successfully`() {
        // Given
        val action = IbcmdBuildAction(mockDsl)
        val properties = createTestProperties()

        // When
        val result = runBlocking { action.build(properties) }

        // Then
        assert(result.success)
        assert(result.configurationBuilt)
        assert(result.sourceSet.isNotEmpty())
    }

    @Test
    fun `should measure execution time correctly`() {
        // Given
        val action = DesignerBuildAction(mockDsl)
        val properties = createTestProperties()

        // When
        val result = runBlocking { action.build(properties) }

        // Then
        assert(result.duration.toMillis() >= 0)
    }

    private fun createTestProperties(): ApplicationProperties {
        val basePath = Paths.get(".") // Use current directory which should exist
        val sourceSet = SourceSet(
            listOf(
                SourceSetItem(
                    name = "Configuration",
                    path = "Configuration",
                    type = SourceSetType.CONFIGURATION,
                    purpose = setOf(SourceSetPurpose.MAIN)
                ),
                SourceSetItem(
                    name = "TestExtension",
                    path = "Extensions/TestExtension",
                    type = SourceSetType.EXTENSION,
                    purpose = setOf(SourceSetPurpose.MAIN)
                )
            )
        )

        return ApplicationProperties(
            basePath = basePath,
            sourceSet = sourceSet,
            connection = ConnectionProperties("test-connection"),
            tools = ToolsProperties(BuilderType.DESIGNER)
        )
    }
} 