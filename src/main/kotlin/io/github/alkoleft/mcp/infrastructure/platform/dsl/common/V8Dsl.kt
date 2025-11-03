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
 * Базовый DSL класс для работы с платформой 1С
 *
 * @param T тип контекста платформы
 * @param C тип команды
 * @param context контекст платформы
 */
abstract class V8Dsl<T : V8Context, C : Command>(
    context: T,
) : Dsl<T, C>(context) {
    /**
     * Устанавливает строку подключения
     *
     * @param connectionString строка подключения к ИБ
     */
    fun connect(connectionString: String) {
        context.connect(connectionString)
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
        context.connectToServer(serverName, dbName)
    }

    /**
     * Подключается к файловой БД
     *
     * @param path путь к файлу БД
     */
    fun connectToFile(path: String) {
        context.connectToFile(path)
    }

    /**
     * Устанавливает имя пользователя
     *
     * @param user имя пользователя
     */
    fun user(user: String) {
        context.user(user)
    }

    /**
     * Устанавливает пароль
     *
     * @param password пароль
     */
    fun password(password: String) {
        context.password(password)
    }

    /**
     * Устанавливает путь к файлу вывода
     *
     * @param path путь к файлу
     */
    fun output(path: Path) {
        context.output(path)
    }

    /**
     * Устанавливает код языка интерфейса
     *
     * @param code код языка
     */
    fun language(code: String) {
        context.language(code)
    }

    /**
     * Устанавливает код локализации
     *
     * @param code код локализации
     */
    fun localization(code: String) {
        context.localization(code)
    }

    /**
     * Отключает стартовые диалоги
     */
    fun disableStartupDialogs() {
        context.disableStartupDialogs()
    }

    /**
     * Отключает стартовые сообщения
     */
    fun disableStartupMessages() {
        context.disableStartupMessages()
    }

    /**
     * Устанавливает флаг не очищать файл вывода при записи
     */
    fun noTruncate() {
        context.noTruncate()
    }

    override fun buildCommandArgs(
        command: C,
        logPath: Path?,
    ): List<String> =
        if (logPath != null) {
            super.buildCommandArgs(command, null) + listOf("/Out", logPath.toString())
        } else {
            super.buildCommandArgs(command, null)
        }
}
