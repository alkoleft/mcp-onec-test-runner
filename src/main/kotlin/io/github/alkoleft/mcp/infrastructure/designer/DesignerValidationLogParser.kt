package io.github.alkoleft.mcp.infrastructure.designer

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class DesignerValidationLogParser {
    private val moduleIssueRegex = Regex("""^\{([^}]+\s)?([^}]+)\((\d+),(\d+)\)}:\s*(.+)$""")
    private val objectIssueRegex = Regex("""^([\wа-яА-Я_]+\s)?([\wа-яА-Я_.]+)\s(.+)$""")

    fun parse(path: Path): ConfiguratorLogAnalysis =
        if (!Files.exists(path)) {
            ConfiguratorLogAnalysis(emptyList())
        } else {
            Files.newBufferedReader(path).use { reader ->
                parse(reader.lineSequence())
            }
        }

    fun parse(content: String): ConfiguratorLogAnalysis = parse(content.lineSequence())

    private fun parse(sequence: Sequence<String>): ConfiguratorLogAnalysis {
        val entries: MutableList<ConfiguratorIssue> = mutableListOf()
        val iterator: Iterator<String> = sequence.iterator()

        while (iterator.hasNext()) {
            val line: String = iterator.next()
            if (line.isBlank()) {
                continue
            }

            val issue =
                if (line.trimStart().startsWith("{")) {
                    parseModuleIssue(line, iterator.next())
                } else {
                    parseObjectIssue(line)
                }
            if (issue != null) {
                entries.add(issue)
            }
        }
        return ConfiguratorLogAnalysis(entries)
    }

    private fun parseModuleIssue(
        line: String,
        nextLine: String?,
    ): ConfiguratorIssue.ModuleIssue? =
        moduleIssueRegex.matchEntire(line.trim())?.let { match ->
            ConfiguratorIssue.ModuleIssue(
                path = match.groupValues[2],
                extension = match.groupValues[1].trim().ifEmpty { null },
                line = match.groupValues[3].toIntOrNull() ?: 0,
                column = match.groupValues[4].toIntOrNull() ?: 0,
                description = match.groupValues[5].trim(),
                context = nextLine?.let(::extractContext),
            )
        }

    private fun extractContext(description: String): String? {
        val startIndex: Int = description.lastIndexOf('(')
        val contextStart = description.indexOf(':', startIndex)
        val endIndex: Int = description.lastIndexOf(')')
        if (startIndex == -1 || contextStart == -1 || endIndex == -1 || endIndex <= contextStart) {
            return null
        }
        return description.substring(contextStart + 1, endIndex).trim().takeIf { it.isNotBlank() }
    }

    private fun parseObjectIssue(line: String): ConfiguratorIssue? =
        objectIssueRegex.matchEntire(line.trim())?.let { match ->
            ConfiguratorIssue.ObjectIssue(
                path = match.groupValues[2],
                message = match.groupValues[3],
                extension = match.groupValues[1].trim().ifEmpty { null },
            )
        }
}

data class ConfiguratorLogAnalysis(
    val entries: List<ConfiguratorIssue>,
)

sealed interface ConfiguratorIssue {
    val path: String
    val extension: String?

    data class ModuleIssue(
        override val path: String,
        override val extension: String?,
        val line: Int,
        val column: Int,
        val description: String,
        val context: String?,
    ) : ConfiguratorIssue

    data class ObjectIssue(
        override val path: String,
        override val extension: String?,
        val message: String,
    ) : ConfiguratorIssue
}
