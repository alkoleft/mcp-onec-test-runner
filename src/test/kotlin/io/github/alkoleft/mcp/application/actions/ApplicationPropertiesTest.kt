package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ApplicationPropertiesTest {

    @Test
    fun `should correctly resolve paths from sourceSet with composite purposes`() {
        val applicationProperties = ApplicationProperties(
            basePath = Paths.get("/project"),
            sourceSet = listOf(
                SourceSetItem("src", "main", SourceSetType.CONFIGURATION, setOf(SourceSetPurpose.MAIN)),
                SourceSetItem(
                    "tests",
                    "tests",
                    SourceSetType.EXTENSION,
                    setOf(SourceSetPurpose.TESTS, SourceSetPurpose.YAXUNIT)
                ),
                SourceSetItem("yaxunit", "yaxunit", SourceSetType.EXTENSION, setOf(SourceSetPurpose.YAXUNIT))
            ),
            connection = ConnectionProperties("connection"),
            platformVersion = "8.3.24.1761",
            tools = ToolsProperties()
        )

        assertEquals(Paths.get("/project/src"), applicationProperties.configurationPath)
        assertNotNull(applicationProperties.testsPath) // testsPath должен быть найден
        assertNotNull(applicationProperties.yaxunitEnginePath) // yaxunitEnginePath должен быть найден
        assertEquals(Paths.get("/project/src"), applicationProperties.mainCodePath)
    }

    @Test
    fun `should handle missing sourceSet items gracefully`() {
        val applicationProperties = ApplicationProperties(
            basePath = Paths.get("/project"),
            sourceSet = emptyList(),
            connection = ConnectionProperties("connection"),
            platformVersion = "8.3.24.1761",
            tools = ToolsProperties()
        )

        assertNull(applicationProperties.configurationPath)
        assertNull(applicationProperties.testsPath)
        assertNull(applicationProperties.yaxunitEnginePath)
        assertNull(applicationProperties.mainCodePath)
    }

    @Test
    fun `should handle single purpose sourceSet items`() {
        val applicationProperties = ApplicationProperties(
            basePath = Paths.get("/project"),
            sourceSet = listOf(
                SourceSetItem("config", "config", SourceSetType.CONFIGURATION, setOf(SourceSetPurpose.MAIN)),
                SourceSetItem("main", "main", SourceSetType.EXTENSION, setOf(SourceSetPurpose.MAIN))
            ),
            connection = ConnectionProperties("connection"),
            platformVersion = "8.3.24.1761",
            tools = ToolsProperties()
        )

        assertEquals(Paths.get("/project/config"), applicationProperties.configurationPath)
        assertNull(applicationProperties.testsPath)
        assertNull(applicationProperties.yaxunitEnginePath)
        assertEquals(Paths.get("/project/config"), applicationProperties.mainCodePath)
    }
} 