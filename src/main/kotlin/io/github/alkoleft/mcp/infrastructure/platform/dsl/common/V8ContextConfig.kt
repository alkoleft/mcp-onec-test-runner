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

import java.nio.file.Path

/**
 * Интерфейс для конфигурации контекста V8 (платформа 1С)
 *
 * Определяет методы для настройки подключения и параметров выполнения
 * утилит платформы 1С (конфигуратор, предприятие).
 */
interface V8ContextConfig {
    /**
     * Устанавливает строку подключения
     *
     * @param connectionString строка подключения к ИБ
     */
    fun connect(connectionString: String)

    /**
     * Подключается к серверу приложений
     *
     * @param serverName имя сервера
     * @param dbName имя базы данных
     */
    fun connectToServer(
        serverName: String,
        dbName: String,
    )

    /**
     * Подключается к файловой БД
     *
     * @param path путь к файлу БД
     */
    fun connectToFile(path: String)

    /**
     * Устанавливает имя пользователя
     *
     * @param user имя пользователя
     */
    fun user(user: String)

    /**
     * Устанавливает пароль
     *
     * @param password пароль
     */
    fun password(password: String)

    /**
     * Устанавливает путь к файлу вывода
     *
     * @param path путь к файлу
     */
    fun output(path: Path)

    /**
     * Устанавливает код языка интерфейса
     *
     * @param code код языка
     */
    fun language(code: String)

    /**
     * Устанавливает код локализации
     *
     * @param code код локализации
     */
    fun localization(code: String)

    /**
     * Отключает стартовые диалоги
     */
    fun disableStartupDialogs()

    /**
     * Отключает стартовые сообщения
     */
    fun disableStartupMessages()

    /**
     * Устанавливает флаг не очищать файл вывода при записи
     */
    fun noTruncate()
}
