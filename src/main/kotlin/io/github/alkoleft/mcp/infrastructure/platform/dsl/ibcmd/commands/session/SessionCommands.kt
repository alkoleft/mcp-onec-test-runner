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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.session

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 1. info — Получение информации о сеансе
 *
 * Получает подробную информацию о конкретном сеансе.
 */
data class SessionInfoCommand(
    /**
     * Идентификатор сеанса (обязательно)
     * --session=<uuid>
     */
    val session: String,
    /**
     * Вывод информации о лицензиях
     * --licenses
     */
    var licenses: Boolean = false,
) : IbcmdCommand {
    override val mode: String = "session"
    override val subCommand: String = "info"
    override val commandName: String = "session info"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            args.addAll(listOf("--session", session))
            if (licenses) args.add("--licenses")

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        details.add("сеанс: $session")
        if (licenses) details.add("с лицензиями")

        return "Получение информации о сеансе (${details.joinToString(", ")})"
    }
}

/**
 * 2. list — Получение списка сеансов
 *
 * Получает список всех активных сеансов.
 */
data class SessionListCommand(
    /**
     * Вывод информации о лицензиях
     * --licenses
     */
    val licenses: Boolean = false,
) : IbcmdCommand {
    override val mode: String = "session"
    override val subCommand: String = "list"
    override val commandName: String = "session list"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            if (licenses) args.add("--licenses")

            return args
        }

    override fun getFullDescription(): String = "Получение списка сеансов" + if (licenses) " (с лицензиями)" else ""
}

/**
 * 3. terminate — Принудительное завершение сеанса
 *
 * Принудительно завершает указанный сеанс.
 */
data class SessionTerminateCommand(
    /**
     * Идентификатор сеанса (обязательно)
     * --session=<uuid>
     */
    val session: String,
    /**
     * Сообщение о причине завершения
     * --error-message=<string>
     */
    var errorMessage: String? = null,
) : IbcmdCommand {
    override val mode: String = "session"
    override val subCommand: String = "terminate"
    override val commandName: String = "session terminate"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            args.addAll(listOf("--session", session))
            errorMessage?.let { args.addAll(listOf("--error-message", it)) }

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        details.add("сеанс: $session")
        errorMessage?.let { details.add("сообщение: $it") }

        return "Принудительное завершение сеанса (${details.joinToString(", ")})"
    }
}

/**
 * 4. interrupt-current-server-call — Прерывание текущего серверного вызова
 *
 * Прерывает текущий серверный вызов в указанном сеансе.
 */
data class SessionInterruptCurrentServerCallCommand(
    /**
     * Идентификатор сеанса (обязательно)
     * --session=<uuid>
     */
    val session: String,
    /**
     * Сообщение о причине прерывания
     * --error-message=<string>
     */
    val errorMessage: String? = null,
) : IbcmdCommand {
    override val mode: String = "session"
    override val subCommand: String = "interrupt-current-server-call"
    override val commandName: String = "session interrupt-current-server-call"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            args.addAll(listOf("--session", session))
            errorMessage?.let { args.addAll(listOf("--error-message", it)) }

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        details.add("сеанс: $session")
        errorMessage?.let { details.add("сообщение: $it") }

        return "Прерывание текущего серверного вызова (${details.joinToString(", ")})"
    }
}
