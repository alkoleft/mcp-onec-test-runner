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

/**
 * Типы расширений конфигурации согласно спецификации.
 *
 * Определяет назначение расширения и влияет на правила его применения.
 *
 * @param value строковое представление типа для использования в командах ibcmd
 */
enum class ExtensionPurpose(
    val value: String,
) {
    /** Доработка конфигурации под нужды конкретного предприятия */
    CUSTOMIZATION("customization"),

    /** Добавочный модуль для расширения функциональности */
    ADD_ON("add-on"),

    /** Патч для исправления ошибок или добавления мелкого функционала */
    PATCH("patch"),
}

/**
 * Булевы значения для параметров ibcmd.
 *
 * Используется для явного указания yes/no в параметрах команд.
 *
 * @param value строковое представление значения для командной строки
 */
enum class IbcmdBoolean(
    val value: String,
) {
    /** Значение "да" для включения параметра */
    YES("yes"),

    /** Значение "нет" для выключения параметра */
    NO("no"),
}

/**
 * Режимы динамического обновления конфигурации.
 *
 * Определяет поведение системы при обновлении конфигурации информационной базы.
 *
 * @param value строковое представление режима для командной строки
 */
enum class DynamicUpdateMode(
    val value: String,
) {
    /** Автоматическое обновление без подтверждений */
    AUTO("auto"),

    /** Отключение обновления */
    DISABLE("disable"),

    /** Запрос подтверждения перед обновлением */
    PROMPT("prompt"),

    /** Принудительное обновление без подтверждений */
    FORCE("force"),
}

/**
 * Режимы завершения сеансов пользователей.
 *
 * Определяет поведение при завершении активных сеансов на сервере.
 *
 * @param value строковое представление режима для командной строки
 */
enum class SessionTerminateMode(
    val value: String,
) {
    /** Запретить завершение сеансов */
    DISABLE("disable"),

    /** Запрашивать подтверждение перед завершением */
    PROMPT("prompt"),

    /** Принудительно завершать без запроса */
    FORCE("force"),
}

/**
 * Области действия расширений конфигурации.
 *
 * Определяет, к каким данным применяется расширение.
 *
 * @param value строковое представление области для командной строки
 */
enum class ExtensionScope(
    val value: String,
) {
    /** Расширение действует на всю информационную базу */
    INFOBASE("infobase"),

    /** Расширение действует на разделители данных */
    DATA_SEPARATION("data-separation"),
}

/**
 * Типы систем управления базами данных, поддерживаемые ibcmd.
 *
 * Используется для указания типа СУБД при работе с серверными базами данных.
 *
 * @param value строковое представление типа СУБД для командной строки
 */
enum class DbmsType(
    val value: String,
) {
    /** Microsoft SQL Server */
    MSSQL_SERVER("MSSQLServer"),

    /** PostgreSQL */
    POSTGRESQL("PostgreSQL"),

    /** IBM DB2 */
    IBM_DB2("IBMDB2"),

    /** Oracle Database */
    ORACLE_DATABASE("OracleDatabase"),
}

/**
 * Флаги для управления автономным сервером.
 *
 * Используются для контроля выдачи лицензий и планирования заданий.
 *
 * @param value строковое представление флага для командной строки
 */
enum class ServerFlag(
    val value: String,
) {
    /** Разрешить операцию */
    ALLOW("allow"),

    /** Запретить операцию */
    DENY("deny"),
}
