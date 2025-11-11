package io.github.alkoleft.mcp.infrastructure.designer

import io.github.alkoleft.mcp.application.services.validation.Issue
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class DesignerValidationLogParser {
    private val moduleIssueRegex = Regex("""^\{([^}]+\s)?([^}]+)\((\d+),(\d+)\)}:\s*(.+)$""")
    private val objectIssueRegex = Regex("""^([\wа-яА-Я_]+\s)?([\wа-яА-Я_.]+)\s(.+)$""")

    fun parse(path: Path): List<Issue> =
        if (!Files.exists(path)) {
            emptyList()
        } else {
            Files.newBufferedReader(path).use { reader ->
                parse(reader.lineSequence())
            }
        }

    fun parse(content: String) = parse(content.lineSequence())

    private fun parse(sequence: Sequence<String>): List<Issue> {
        val entries: MutableList<Issue> = mutableListOf()
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
        return entries
    }

    private fun parseModuleIssue(
        line: String,
        nextLine: String?,
    ): Issue.ModuleIssue? =
        moduleIssueRegex.matchEntire(line.trim())?.let { match ->
            Issue.ModuleIssue(
                path = match.groupValues[2],
                extension = match.groupValues[1].trim().ifEmpty { null },
                line = match.groupValues[3].toIntOrNull() ?: 0,
                column = match.groupValues[4].toIntOrNull() ?: 0,
                message = match.groupValues[5].trim(),
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

    private fun parseObjectIssue(line: String): Issue? =
        objectIssueRegex.matchEntire(line.trim())?.let { match ->
            Issue.ObjectIssue(
                path = match.groupValues[2],
                message = match.groupValues[3],
                extension = match.groupValues[1].trim().ifEmpty { null },
            )
        }
}
