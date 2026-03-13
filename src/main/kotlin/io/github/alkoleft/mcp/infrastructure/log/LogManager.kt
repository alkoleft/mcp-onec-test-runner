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

package io.github.alkoleft.mcp.infrastructure.log

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.FileAppender
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

private val logger = KotlinLogging.logger { }

/**
 * Сервис управления логированием
 */
@Component
class LogManager {
    private val logFilePaths: List<String> by lazy {
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        lc.loggerList
            .filter { it.level != null }
            .flatMap { l -> l.iteratorForAppenders().asSequence().toList() }
            .filterIsInstance<FileAppender<*>>()
            .map { it.file }
            .distinct()
    }

    /**
     * Очищает файл лога, если настройка включена
     * @param cleanLogBeforeExecution true если нужно очистить лог перед выполнением
     */
    fun cleanLogIfEnabled(cleanLogBeforeExecution: Boolean) {
        if (!cleanLogBeforeExecution) return

        if (logFilePaths.isEmpty()) {
            logger.warn { "Не удалось определить путь к файлу лога" }
            return
        }

        logFilePaths.forEach { filePath ->
            try {
                File(filePath).writeText("")
                logger.info { "Лог очищен перед выполнением: $filePath" }
            } catch (e: Exception) {
                logger.error(e) { "Ошибка при очистке лога: $filePath" }
            }
        }
    }
}
