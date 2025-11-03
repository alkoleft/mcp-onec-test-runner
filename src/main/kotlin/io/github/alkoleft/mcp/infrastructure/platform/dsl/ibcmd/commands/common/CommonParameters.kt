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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common

data class CommonParameters(
    val root: RootParameters = RootParameters(),
    val connection: DatabaseConnectionParameters = DatabaseConnectionParameters(),
    val serverDirectory: ServerDirectoryParameters = ServerDirectoryParameters(),
    val user: InfobaseUserParameters = InfobaseUserParameters(),
)

/**
 * Общие параметры для всех режимов ibcmd
 * Эти параметры доступны во всех командах согласно спецификации
 */
data class RootParameters(
    /**
     * Идентификатор процесса сервера
     * --pid=<pid>, -p <pid>
     */
    val pid: String? = null,
    /**
     * Сетевой адрес сервера
     * --remote=<url>, -r <url>
     */
    val remote: String? = null,
) : Parameters {
    override fun toArguments(): List<String> {
        val args = mutableListOf<String>()

        pid?.let { args.addAll(listOf("--pid", it)) }
        remote?.let { args.addAll(listOf("--remote", it)) }

        return args
    }
}

/**
 * Общие параметры подключения к базе данных
 * Используются в большинстве режимов работы
 */
data class DatabaseConnectionParameters(
    /**
     * Путь к конфигурационному файлу
     * --config=<path>, -c <path>
     */
    val config: String? = null,
    /**
     * Путь к системному конфигурационному файлу
     * --system=<path>
     */
    val system: String? = null,
    /**
     * Тип СУБД
     * --dbms=<kind>
     * Допустимые значения:
     * - MSSQLServer - Microsoft SQL Server
     * - PostgreSQL - PostgreSQL
     * - IBMDB2 - IBM DB2
     * - OracleDatabase - Oracle Database
     * Если параметр не указан, используется файловая база данных
     */
    val dbms: String? = null,
    /**
     * Имя сервера СУБД
     * --database-server=<server>, --db-server=<server>
     *
     * Подробности для разных СУБД:
     * - MSSQLServer: имя компьютера или экземпляра (например: "Server/instance")
     *   Для протокола SHARED MEMORY используйте префикс lpc: (например: "lpc:Server/instance")
     *   Внимание: не поддерживается на Linux и MacOS
     * - PostgreSQL: с указанием порта используйте "localhost port=6432;"
     * - IBMDB2: имя экземпляра (например: "computer/db2name")
     * - OracleDatabase: TNS-name (например: "//имя_сервера_БД/имя_сервиса")
     */
    val databaseServer: String? = null,
    /**
     * Имя базы данных
     * --database-name=<name>, --db-name=<name>
     */
    val databaseName: String? = null,
    /**
     * Имя пользователя СУБД
     * --database-user=<name>, --db-user=<name>
     */
    val databaseUser: String? = null,
    /**
     * Пароль пользователя СУБД
     * --database-password=<password>, --db-pwd=<password>
     */
    val databasePassword: String? = null,
    /**
     * Запрос пароля пользователя сервера СУБД через стандартный поток ввода (STDIN)
     * --request-database-password, --request-db-pwd, -W
     */
    val requestDatabasePassword: Boolean = false,
    /**
     * Путь к каталогу файловой базы данных 1С:Предприятия 8
     * --database-path=<path>, --db-path=<path>
     * В случае использования относительного пути, полный путь будет получен относительно каталога данных сервера.
     * По умолчанию используется подкаталог 'db-data' в каталоге данных сервера
     */
    var databasePath: String? = null,
) : Parameters {
    override fun toArguments(): List<String> {
        val args = mutableListOf<String>()

        config?.let { args.addAll(listOf("--config", it)) }
        system?.let { args.addAll(listOf("--system", it)) }
        dbms?.let { args.addAll(listOf("--dbms", it)) }
        databaseServer?.let { args.addAll(listOf("--db-server", it)) }
        databaseName?.let { args.addAll(listOf("--db-name", it)) }
        databaseUser?.let { args.addAll(listOf("--db-user", it)) }
        databasePassword?.let { args.addAll(listOf("--db-pwd", it)) }
        if (requestDatabasePassword) args.add("--request-db-pwd")
        databasePath?.let { args.addAll(listOf("--db-path", it)) }

        return args
    }
}

/**
 * Параметры каталогов данных сервера
 * Используются в режимах infobase, config, mobile-app, mobile-client, extension
 */
data class ServerDirectoryParameters(
    /**
     * Путь к каталогу данных сервера
     * --data=<path>, -d <path>
     * По умолчанию используется значение: /home/alko/.1cv8/1C/1cv8/standalone-server/
     */
    val data: String? = null,
    /**
     * Путь к файлу блокировки каталога данных автономного сервера
     * --lock=<path>
     * По умолчанию используется файл lock.pid в каталоге данных сервера
     */
    val lock: String? = null,
    /**
     * Путь к каталогу временных файлов информационной базы
     * --temp=<path>, -t <path>
     * В случае использования относительного пути, полный путь будет получен относительно каталога данных сервера.
     * По умолчанию используется подкаталог 'temp' в каталоге данных сервера
     */
    val temp: String? = null,
    /**
     * Путь к каталогу конфигурационных данных пользователей
     * --users-data=<path>
     * По умолчанию используется значение: users-data
     */
    val usersData: String? = null,
    /**
     * Путь к каталогу сеансовых данных
     * --session-data=<path>
     * В случае использования относительного пути, полный путь будет получен относительно каталога данных сервера.
     * По умолчанию используется подкаталог 'session-data' в каталоге данных сервера
     */
    val sessionData: String? = null,
    /**
     * Путь к каталогу хранилища моделей распознавания речи
     * --stt-data=<path>
     * В случае использования относительного пути, полный путь будет получен относительно каталога данных сервера.
     * По умолчанию используется подкаталог 'stt-data' в каталоге данных сервера
     */
    val sttData: String? = null,
    /**
     * Путь к каталогу данных журнала регистрации
     * --log-data=<path>
     * В случае использования относительного пути, полный путь будет получен относительно каталога данных сервера.
     * По умолчанию используется подкаталог 'log-data' в каталоге данных сервера
     */
    val logData: String? = null,
    /**
     * Путь к каталогу данных полнотекстового поиска
     * --ftext-data=<path>
     * В случае использования относительного пути, полный путь будет получен относительно каталога данных сервера.
     * По умолчанию используется подкаталог 'ftext-data' в каталоге данных сервера
     */
    val ftextData: String? = null,
    /**
     * Путь к каталогу данных полнотекстового поиска версии 2
     * --ftext2-data=<path>
     * В случае использования относительного пути, полный путь будет получен относительно каталога данных сервера.
     * По умолчанию используется подкаталог 'ftext2-data' в каталоге данных сервера
     */
    val ftext2Data: String? = null,
    /**
     * Путь к каталогу данных OpenID-аутентификации
     * --openid-data=<path>
     * В случае использования относительного пути, полный путь будет получен относительно каталога данных сервера.
     * По умолчанию используется подкаталог 'openid-data' в каталоге данных сервера
     */
    val openidData: String? = null,
    /**
     * Путь к каталогу хранилища двоичных данных
     * --bin-data-strg=<path>
     * В случае использования относительного пути, полный путь будет получен относительно каталога данных сервера.
     * По умолчанию используется подкаталог 'bin-data-strg' в каталоге данных сервера
     */
    val binDataStrg: String? = null,
) : Parameters {
    override fun toArguments(): List<String> {
        val args = mutableListOf<String>()

        data?.let { args.addAll(listOf("--data", it)) }
        lock?.let { args.addAll(listOf("--lock", it)) }
        temp?.let { args.addAll(listOf("--temp", it)) }
        usersData?.let { args.addAll(listOf("--users-data", it)) }
        sessionData?.let { args.addAll(listOf("--session-data", it)) }
        sttData?.let { args.addAll(listOf("--stt-data", it)) }
        logData?.let { args.addAll(listOf("--log-data", it)) }
        ftextData?.let { args.addAll(listOf("--ftext-data", it)) }
        ftext2Data?.let { args.addAll(listOf("--ftext2-data", it)) }
        openidData?.let { args.addAll(listOf("--openid-data", it)) }
        binDataStrg?.let { args.addAll(listOf("--bin-data-strg", it)) }

        return args
    }
}

/**
 * Параметры пользователя информационной базы
 * Используются в большинстве команд для аутентификации
 */
data class InfobaseUserParameters(
    /**
     * Имя пользователя информационной базы
     * --user=<name>, -u <name>
     */
    var user: String? = null,
    /**
     * Пароль пользователя информационной базы
     * --password=<password>, -P <password>
     */
    var password: String? = null,
) : Parameters {
    override fun toArguments(): List<String> {
        val args = mutableListOf<String>()

        user?.let { args.addAll(listOf("--user", it)) }
        password?.let { args.addAll(listOf("--password", it)) }

        return args
    }
}
