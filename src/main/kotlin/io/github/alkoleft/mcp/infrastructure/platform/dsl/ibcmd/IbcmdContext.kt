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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.application.core.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.DslContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilities

/**
 * Контекст для выполнения команд ibcmd.
 *
 * Этот класс предоставляет контекст для работы с утилитой ibcmd и управляет общими параметрами,
 * которые используются во всех командах: путь к базе данных, пользователь и пароль.
 *
 * Контекст наследует от [DslContext] и предоставляет доступ к утилите ibcmd через [PlatformUtilities].
 *
 * @param platformContext контекст платформы, содержащий информацию о доступных утилитах
 *
 * @see [buildBaseArgs] для получения базовых аргументов команды
 */
class IbcmdContext(
    platformContext: PlatformUtilities,
) : DslContext(platformContext) {
    private var dbPath: String? = null

    /**
     * Путь к исполняемому файлу утилиты ibcmd.
     *
     * Автоматически определяется через [PlatformUtilities] по типу утилиты [UtilityType.IBCMD].
     */
    val utilityPath
        get() = platformContext.locateUtility(UtilityType.IBCMD).executablePath.toString()

    /**
     * Устанавливает путь к базе данных.
     *
     * Этот параметр используется во всех командах ibcmd для указания на целевую информационную базу.
     *
     * @param path путь к файлу базы данных или строка подключения к серверу
     *
     * @see [buildBaseArgs] где этот параметр добавляется как аргумент --db-path
     */
    fun dbPath(path: String) {
        this.dbPath = path
    }

    /**
     * Устанавливает имя пользователя для подключения к базе данных.
     *
     * @param user имя пользователя для аутентификации
     *
     * @see [buildBaseArgs] где этот параметр добавляется как аргумент --user
     */
    fun user(user: String) {
        this.user = user
    }

    /**
     * Устанавливает пароль для подключения к базе данных.
     *
     * @param password пароль для аутентификации
     *
     * @see [buildBaseArgs] где этот параметр добавляется как аргумент --password
     */
    fun password(password: String) {
        this.password = password
    }

    /**
     * Строит базовые аргументы для команды ibcmd.
     *
     * Формирует список аргументов командной строки на основе установленных параметров:
     * - путь к базе данных ([dbPath])
     * - имя пользователя ([user])
     * - пароль ([password])
     *
     * Все параметры добавляются только если они были установлены (не null).
     *
     * @return список аргументов командной строки для передачи в ibcmd
     *
     * @see [dbPath]
     * @see [user]
     * @see [password]
     */
    override fun buildBaseArgs(): List<String> {
        val args = mutableListOf<String>()

        // Путь к базе данных
        dbPath?.let {
            args.add("--db-path")
            args.add(it)
        }

        // Пользователь
        user?.let {
            args.add("--user")
            args.add(it)
        }

        // Пароль
        password?.let {
            args.add("--password")
            args.add(it)
        }

        return args
    }
}
