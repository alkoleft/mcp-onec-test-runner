package io.github.alkoleft.mcp

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import kotlin.io.path.Path

const val sourcesPath = "/home/akoryakin@dellin.local/Загрузки/sources"
const val ibPath = "/home/common/develop/file-data-base/YAxUnit"
const val version = "8.3.22.1709"


/**
 * Создает тестовые свойства приложения для тестирования
 */
fun testApplicationProperties(): ApplicationProperties =
    ApplicationProperties(
        basePath = Path(sourcesPath),
        sourceSet = SourceSet(
            listOf(
                SourceSetItem(
                    path = "configuration",
                    name = "configuration",
                    type = SourceSetType.CONFIGURATION,
                    purpose = setOf(SourceSetPurpose.MAIN)
                ),
                SourceSetItem(
                    path = "yaxunit",
                    name = "yaxunit",
                    type = SourceSetType.EXTENSION,
                    purpose = setOf(SourceSetPurpose.YAXUNIT)
                ),
                SourceSetItem(
                    path = "tests",
                    name = "tests",
                    type = SourceSetType.EXTENSION,
                    purpose = setOf(SourceSetPurpose.TESTS)
                )
            )
        ),
        connection = ConnectionProperties(
            connectionString = "File=\"$ibPath\";"
        ),
        platformVersion = version,
        tools = ToolsProperties()
    )

