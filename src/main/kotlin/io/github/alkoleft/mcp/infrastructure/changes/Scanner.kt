/*
 * This file is part of METR.
 *
 * Copyright (C) 2025 Aleksey Koryakin <alkoleft@gmail.com> and contributors.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * METR is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * METR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with METR.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.alkoleft.mcp.infrastructure.changes

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

private val logger = KotlinLogging.logger { }

private val IGNORED_PATTERNS =
    listOf(
        ".yaxunit/",
        "build/",
        "target/",
        ".git/",
        ".gradle/",
        "temp/",
        "tmp/",
        "ConfigDumpInfo.xml",
    )

fun isIgnoredPath(
    path: Path,
    projectRoot: Path,
): Boolean {
    val relativePath = projectRoot.relativize(path).toString().replace("\\", "/")
    return IGNORED_PATTERNS.any { pattern ->
        relativePath.startsWith(pattern) || relativePath.contains("/$pattern")
    }
}

@Service
class Scanner {
    suspend fun scanByLastModifiedTime(
        projectPath: Path,
        projectTimestamp: Long?,
    ): Sequence<Path> {
        logger.debug { "Сканирование изменений временных меток в: $projectPath" }

        val sourceFiles = getAllSourceFiles(projectPath)

        if (projectTimestamp == null) {
            return sourceFiles
        }
        return sourceFiles
            .filter { file ->
                try {
                    val currentTimestamp = Files.getLastModifiedTime(file).toMillis()

                    if (currentTimestamp > projectTimestamp) {
                        // Potentially modified file
                        logger.trace {
                            "Потенциально измененный файл: $file (текущая: $currentTimestamp, сохраненная: $projectTimestamp)"
                        }
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    logger.debug(e) { "Ошибка при проверке временной метки для файла: $file" }
                    true
                }
            }
    }

    /**
     * Gets all source files in the project that should be tracked for changes
     */
    private fun getAllSourceFiles(projectPath: Path): Sequence<Path> {
        try {
            if (!Files.exists(projectPath)) {
                logger.warn { "Путь проекта не существует: $projectPath" }
                return emptySequence()
            }

            return projectPath
                .walk()
                .filter { it.isRegularFile() }
                .filter { !isIgnoredPath(it, projectPath) }
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при сканировании исходных файлов в: $projectPath" }
            return emptySequence()
        }
    }
}
