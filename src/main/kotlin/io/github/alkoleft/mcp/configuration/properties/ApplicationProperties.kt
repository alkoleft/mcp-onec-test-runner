package io.github.alkoleft.mcp.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

/**
 * Настройки приложения
 */
@ConfigurationProperties(prefix = "app")
data class ApplicationProperties(
    val basePath: Path,
    val sourceSet: List<SourceSetItem>,
    val connection: ConnectionProperties,
    val platformVersion: String? = null,

    val tools: ToolsProperties
) {
    val configurationPath: Path? = sourceSet
        .find { it.type == SourceSetType.CONFIGURATION }
        ?.let { basePath.resolve(it.path) }

    val testsPath: Path? = sourceSet
        .find { it.purpose.contains(SourceSetPurpose.TESTS) }
        ?.let { basePath.resolve(it.path) }

    val yaxunitEnginePath: Path? = sourceSet
        .find { it.purpose.contains(SourceSetPurpose.YAXUNIT) }
        ?.let { basePath.resolve(it.path) }

    val mainCodePath: Path? = sourceSet
        .find { it.purpose.contains(SourceSetPurpose.MAIN) }
        ?.let { basePath.resolve(it.path) }
}