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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand
import java.nio.file.Path

/**
 * 8. import — Импорт конфигурации из XML
 *
 * Импортирует конфигурацию из XML формата.
 * Подкоманды: files, all-extensions
 */
class ConfigImportCommand(
    /**
     * Подкоманда импорта (files, all-extensions)
     */
    var importSubCommand: String? = null,
    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    var extension: String? = null,
    /**
     * Базовый каталог XML файлов
     * --base-dir=<directory>
     */
    var baseDir: String? = null,
    /**
     * Путь к архиву XML файлов
     * --archive=<path>
     */
    var archivePath: String? = null,
    /**
     * Отключить проверку метаданных
     * --no-check
     */
    var noCheck: Boolean = false,
    /**
     * Разрешить частичный набор файлов
     * --partial
     */
    var partial: Boolean = false,
    /**
     * Путь к каталогу или архиву XML
     */
    val path: Path,
) : IbcmdCommand {
    override val mode: String = "infobase config"
    override val subCommand: String = "import" + (importSubCommand?.let { " $it" } ?: "")
    override val commandName: String = "$mode $subCommand"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            extension?.let { args.addAll(listOf("--extension", it)) }
            baseDir?.let { args.addAll(listOf("--base-dir", it)) }
            archivePath?.let { args.addAll(listOf("--archive", it)) }
            if (noCheck) args.add("--no-check")
            if (partial) args.add("--partial")
            args.add(path.toString())
            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()

        importSubCommand?.let { details.add("подкоманда: $it") }
        extension?.let { details.add("расширение: $it") }
        archivePath?.let { details.add("архив: $it") }
        if (noCheck) details.add("без проверки")
        if (partial) details.add("частичный импорт")

        if (extension.isNullOrBlank()) "конфигурации" else "расширения"
        return "Импорт ${if (extension.isNullOrBlank()) "конфигурации" else "расширения"} из XML: $path" +
            if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}
