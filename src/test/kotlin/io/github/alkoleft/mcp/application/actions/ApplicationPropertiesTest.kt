package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ApplicationPropertiesTest {

    @Test
    fun `should correctly resolve paths from sourceSet with composite purposes`() {
        val applicationProperties = ApplicationProperties(
            basePath = Paths.get("."), // Use current directory which exists
            sourceSet = SourceSet(
                listOf(
                SourceSetItem("src", "main", SourceSetType.CONFIGURATION, setOf(SourceSetPurpose.MAIN)),
                SourceSetItem(
                    "tests",
                    "tests",
                    SourceSetType.EXTENSION,
                    setOf(SourceSetPurpose.TESTS, SourceSetPurpose.YAXUNIT)
                ),
                SourceSetItem("yaxunit", "yaxunit", SourceSetType.EXTENSION, setOf(SourceSetPurpose.YAXUNIT))
                )
            ),
            connection = ConnectionProperties("connection"),
            platformVersion = "8.3.24.1761",
            tools = ToolsProperties()
        )

        assertEquals(Paths.get("./src"), applicationProperties.configurationPath)
        assertNotNull(applicationProperties.testsPath) // testsPath должен быть найден
        assertNotNull(applicationProperties.yaxunitEnginePath) // yaxunitEnginePath должен быть найден
        assertEquals(Paths.get("./src"), applicationProperties.mainCodePath)
    }

    @Test
    fun `should handle missing sourceSet items gracefully`() {
        // This test now expects the validation to fail with empty sourceSet
        assertThrows<IllegalArgumentException> {
            ApplicationProperties(
                basePath = Paths.get("."), // Use current directory which exists
                sourceSet = SourceSet(),
                connection = ConnectionProperties("connection"),
                platformVersion = "8.3.24.1761",
                tools = ToolsProperties()
            )
        }
    }

    @Test
    fun `should handle single purpose sourceSet items`() {
        val applicationProperties = ApplicationProperties(
            basePath = Paths.get("."), // Use current directory which exists
            sourceSet = SourceSet(
                listOf(
                SourceSetItem("config", "config", SourceSetType.CONFIGURATION, setOf(SourceSetPurpose.MAIN)),
                SourceSetItem("main", "main", SourceSetType.EXTENSION, setOf(SourceSetPurpose.MAIN))
                )
            ),
            connection = ConnectionProperties("connection"),
            platformVersion = "8.3.24.1761",
            tools = ToolsProperties()
        )

        assertEquals(Paths.get("./config"), applicationProperties.configurationPath)
        assertNull(applicationProperties.testsPath)
        assertNull(applicationProperties.yaxunitEnginePath)
        assertEquals(Paths.get("./config"), applicationProperties.mainCodePath)
    }
} 