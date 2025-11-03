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

/**
 * 13. sign — Цифровая подпись конфигурации/расширения
 *
 * Подписывает конфигурацию или расширение.
 */
data class ConfigSignCommand(
    /**
     * Путь к приватному ключу
     * --key=<path>, -k <path>
     */
    var key: String? = null,
    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    var extension: String? = null,
    /**
     * Операция над конфигурацией базы данных
     * --db
     */
    var db: Boolean = false,
    /**
     * Путь для подписанной копии
     * --out=<path>, -o <path>
     */
    var out: String? = null,
    /**
     * Путь к файлу для подписи
     */
    var path: String? = null,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "sign"
    override val commandName: String = "config sign"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            key?.let { args.addAll(listOf("--key", it)) }
            extension?.let { args.addAll(listOf("--extension", it)) }
            if (db) args.add("--db")
            out?.let { args.addAll(listOf("--out", it)) }
            path?.let { args.add(it) }

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()

        key?.let { details.add("ключ: $it") }
        extension?.let { details.add("расширение: $it") }
        if (db) details.add("конфигурация БД")
        out?.let { details.add("вывод: $it") }
        path?.let { details.add("файл: $it") }

        return "Цифровая подпись конфигурации/расширения" +
            if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}
