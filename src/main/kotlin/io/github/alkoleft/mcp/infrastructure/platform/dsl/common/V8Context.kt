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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import io.github.alkoleft.mcp.application.core.UtilityType
import io.github.alkoleft.mcp.infrastructure.utility.ifNoBlank
import java.nio.file.Path

/**
 * Базовый контекст с общей конфигурацией для утилит платформы (Enterprise/Designer)
 *
 * @param platformContext контекст для работы с утилитами платформы
 */
abstract class V8Context(
    platformContext: PlatformUtilities,
) : DslContext(platformContext) {
    /** Строка подключения к информационной базе */
    protected var connectionString: String = ""

    /** Путь к файлу вывода */
    protected var outputPath: Path? = null

    /** Код языка интерфейса */
    protected var language: String? = null

    /** Код локализации */
    protected var localization: String? = null

    /** Флаг отключения стартовых диалогов */
    protected var disableStartupDialogs: Boolean = false

    /** Флаг отключения стартовых сообщений */
    protected var disableStartupMessages: Boolean = false

    /** Флаг не очищать файл вывода при записи */
    protected var noTruncate: Boolean = false

    /**
     * Устанавливает строку подключения
     *
     * @param connectionString строка подключения к ИБ
     */
    fun connect(connectionString: String) {
        this.connectionString = "\"${connectionString.replace("\"", "\"\"")}\""
    }

    /**
     * Подключается к серверу приложений
     *
     * @param serverName имя сервера
     * @param dbName имя базы данных
     */
    fun connectToServer(
        serverName: String,
        dbName: String,
    ) {
        this.connectionString = "Srvr=\"$serverName\";Ref=\"$dbName\";"
    }

    /**
     * Подключается к файловой БД
     *
     * @param path путь к файлу БД
     */
    fun connectToFile(path: String) {
        this.connectionString = "File=\"$path\";"
    }

    /**
     * Устанавливает имя пользователя
     *
     * @param user имя пользователя
     */
    fun user(user: String) {
        this.user = user
    }

    /**
     * Устанавливает пароль
     *
     * @param password пароль
     */
    fun password(password: String) {
        this.password = password
    }

    /**
     * Устанавливает путь к файлу вывода
     *
     * @param path путь к файлу
     */
    fun output(path: Path) {
        this.outputPath = path
    }

    /**
     * Устанавливает код языка интерфейса
     *
     * @param code код языка
     */
    fun language(code: String) {
        this.language = code
    }

    /**
     * Устанавливает код локализации
     *
     * @param code код локализации
     */
    fun localization(code: String) {
        this.localization = code
    }

    /**
     * Отключает стартовые диалоги
     */
    fun disableStartupDialogs() {
        this.disableStartupDialogs = true
    }

    /**
     * Отключает стартовые сообщения
     */
    fun disableStartupMessages() {
        this.disableStartupMessages = true
    }

    /**
     * Не очищает файл вывода при записи
     */
    fun noTruncate() {
        this.noTruncate = true
    }

    /**
     * Строит общие аргументы для команд утилит платформы
     *
     * @param utilityType тип утилиты
     * @param mode режим работы утилиты
     * @return список аргументов
     */
    protected fun buildCommonArgs(
        utilityType: UtilityType,
        mode: String,
    ): MutableList<String> {
        val args = mutableListOf<String>()
        val location = platformContext.locateUtility(utilityType).executablePath
        args.add(location.toString())
        if (mode.isNotBlank()) {
            args.add(mode)
        }

        connectionString.ifNoBlank {
            args.add("/IBConnectionString")
            args.add(connectionString)
        }

        user?.ifNoBlank { args.add("/N\"$it\"") }
        password?.ifNoBlank { args.add("/P\"$it\"") }

        outputPath?.let {
            args.add("/Out$it")
            if (noTruncate) {
                args.add("-NoTruncate")
            }
        }
        language?.ifNoBlank { args.add("/L$it") }
        localization?.ifNoBlank { args.add("/VL$it") }

        if (disableStartupDialogs) args.add("/DisableStartupDialogs")
        if (disableStartupMessages) args.add("/DisableStartupMessages")

        return args
    }
}
