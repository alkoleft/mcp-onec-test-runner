package io.github.alkoleft.mcp

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import kotlin.io.path.Path

const val SOURCE_PATH = "/home/akoryakin@dellin.local/Загрузки/sources"
const val IB_PATH = "/home/common/develop/file-data-base/YAxUnit"
const val VERSION = "8.3.22.1709"

/**
 * Создает тестовые свойства приложения для тестирования
 */
fun testApplicationProperties(): ApplicationProperties =
    ApplicationProperties(
        basePath = Path(SOURCE_PATH),
        sourceSet =
            SourceSet(
                listOf(
                    SourceSetItem(
                        path = "configuration",
                        name = "configuration",
                        type = SourceSetType.CONFIGURATION,
                        purpose = setOf(SourceSetPurpose.MAIN),
                    ),
                    SourceSetItem(
                        path = "yaxunit",
                        name = "yaxunit",
                        type = SourceSetType.EXTENSION,
                        purpose = setOf(SourceSetPurpose.YAXUNIT),
                    ),
                    SourceSetItem(
                        path = "tests",
                        name = "tests",
                        type = SourceSetType.EXTENSION,
                        purpose = setOf(SourceSetPurpose.TESTS),
                    ),
                ),
            ),
        connection =
            ConnectionProperties(
                connectionString = "File=\"$IB_PATH\";",
            ),
        platformVersion = VERSION,
        tools = ToolsProperties(),
    )
