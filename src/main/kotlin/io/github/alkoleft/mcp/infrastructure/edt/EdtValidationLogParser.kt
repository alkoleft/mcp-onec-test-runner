package io.github.alkoleft.mcp.infrastructure.edt

import io.github.alkoleft.mcp.application.services.validation.EdtIssueLevel
import io.github.alkoleft.mcp.application.services.validation.Issue
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class EdtValidationLogParser {
    fun parse(path: Path) =
        if (!Files.exists(path)) {
            emptyList()
        } else {
            Files.newBufferedReader(path).use { reader ->
                parse(reader.lineSequence())
            }
        }

    fun parse(content: String) = parse(content.lineSequence())

    private fun parse(sequence: Sequence<String>): MutableList<Issue.EdtValidationIssue> {
        val issues: MutableList<Issue.EdtValidationIssue> = mutableListOf()
        for (line in sequence) {
            if (line.isBlank()) {
                continue
            }
            val issue: Issue.EdtValidationIssue = parseLine(line) ?: continue
            issues.add(issue)
        }
        return issues
    }

    private fun parseLine(line: String): Issue.EdtValidationIssue? {
        val columns: List<String> = line.split('\t')
        if (columns.size < MIN_COLUMN_COUNT) {
            return null
        }

        val levelText: String = columns[1].trim()
        val level = EdtIssueLevel.fromValue(levelText)
        val project: String? = columns[3].trim().ifBlank { null }
        val issueId: String? =
            columns[4]
                .trim()
                .let {
                    val pos = it.lastIndexOf(':')
                    if (pos > 0) {
                        it.substring(pos + 1)
                    } else {
                        it
                    }
                }.ifBlank { null }
        val pathValue = columns[5].trim()
        val place = columns[6].trim().ifBlank { null }
        val message =
            columns
                .drop(MESSAGE_START_INDEX)
                .joinToString("\t")
                .trim()
        return Issue.EdtValidationIssue(
            level = level,
            project = project,
            issueId = issueId,
            path = pathValue,
            place = place,
            message = message,
        )
    }

    companion object {
        private const val MIN_COLUMN_COUNT: Int = 8
        private const val MESSAGE_START_INDEX: Int = 7
    }
}
