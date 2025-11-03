package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.infobase

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 1. create — Создание информационной базы
 *
 * Создаёт новую информационную базу.
 */
data class InfobaseCreateCommand(
    /**
     * Локаль ИБ
     * --locale=<name>, -l <name>
     */
    var locale: String? = null,
    /**
     * Смещение дат (для MSSQLServer)
     * --date-offset=<years>
     */
    var dateOffset: Int? = null,
    /**
     * Создать БД, если отсутствует
     * --create-database
     */
    var createDatabase: Boolean = false,
    /**
     * Путь к файлу выгрузки для загрузки
     * --restore=<file>
     */
    var restore: String? = null,
    /**
     * Путь к файлу конфигурации для загрузки
     * --load=<file>
     */
    var load: String? = null,
    /**
     * Путь к каталогу XML для загрузки
     * --import=<directory>
     */
    var import: String? = null,
    /**
     * Выполнить обновление конфигурации после загрузки
     * --apply
     */
    var apply: Boolean = false,
    /**
     * Подтверждение при наличии предупреждений
     * --force, -F
     */
    var force: Boolean = false,
) : IbcmdCommand {
    override val mode: String = "infobase"
    override val subCommand: String = "create"
    override val commandName: String = "infobase create"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            locale?.let { args.addAll(listOf("--locale", it)) }
            dateOffset?.let { args.addAll(listOf("--date-offset", it.toString())) }
            if (createDatabase) args.add("--create-database")
            restore?.let { args.addAll(listOf("--restore", it)) }
            load?.let { args.addAll(listOf("--load", it)) }
            import?.let { args.addAll(listOf("--import", it)) }
            if (apply) args.add("--apply")
            if (force) args.add("--force")

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()

        locale?.let { details.add("локаль: $it") }
        restore?.let { details.add("восстановление из: $it") }
        load?.let { details.add("загрузка конфигурации: $it") }
        import?.let { details.add("импорт из XML: $it") }
        if (createDatabase) details.add("создание БД")
        if (apply) details.add("обновление конфигурации")
        if (force) details.add("принудительно")

        return "Создание информационной базы" +
                if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}

/**
 * 2. dump — Выгрузка данных информационной базы
 *
 * Выгружает данные ИБ в файл.
 */
data class InfobaseDumpCommand(
    /**
     * Путь к файлу выгрузки
     */
    val path: String,
) : IbcmdCommand {
    override val mode: String = "infobase"
    override val subCommand: String = "dump"
    override val commandName: String = "infobase dump"

    override val arguments = listOf(path)

    override fun getFullDescription(): String = "Выгрузка данных информационной базы в файл: $path"
}

/**
 * 3. restore — Загрузка данных информационной базы
 *
 * Загружает данные из файла выгрузки в ИБ.
 */
data class InfobaseRestoreCommand(
    /**
     * Создать БД, если отсутствует
     * --create-database
     */
    var createDatabase: Boolean = false,
    /**
     * Принудительное завершение сеансов
     * --force, -F
     */
    var force: Boolean = false,
    /**
     * Путь к файлу выгрузки
     */
    val path: String,
) : IbcmdCommand {
    override val mode: String = "infobase"
    override val subCommand: String = "restore"
    override val commandName: String = "infobase restore"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            if (createDatabase) args.add("--create-database")
            if (force) args.add("--force")
            args.add(path)

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        if (createDatabase) details.add("создание БД")
        if (force) details.add("принудительное завершение сеансов")

        return "Загрузка данных информационной базы из файла: $path" +
                if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}

/**
 * 4. clear — Очистка информационной базы
 *
 * Очищает информационную базу.
 */
class InfobaseClearCommand : IbcmdCommand {
    override val mode: String = "infobase"
    override val subCommand: String = "clear"
    override val commandName: String = "infobase clear"

    override val arguments = emptyList<String>()

    override fun getFullDescription(): String = "Очистка информационной базы"
}

/**
 * 5. replicate — Репликация информационной базы
 *
 * Копирует данные между ИБ или СУБД.
 */
data class InfobaseReplicateCommand(
    /**
     * Тип целевой СУБД
     * --target-dbms=<kind>
     */
    var targetDbms: String? = null,
    /**
     * Сервер целевой СУБД
     * --target-database-server=<server>, --target-db-server=<server>
     */
    var targetDatabaseServer: String? = null,
    /**
     * Имя целевой БД
     * --target-database-name=<name>, --target-db-name=<name>
     */
    var targetDatabaseName: String? = null,
    /**
     * Пользователь целевой СУБД
     * --target-database-user=<name>, --target-db-user=<name>
     */
    var targetDatabaseUser: String? = null,
    /**
     * Пароль целевой СУБД
     * --target-database-password=<password>, --target-db-pwd=<password>
     */
    var targetDatabasePassword: String? = null,
    /**
     * Запрос пароля целевой СУБД
     * --target-request-database-password, --target-request-db-pwd
     */
    var targetRequestDatabasePassword: Boolean = false,
    /**
     * Путь к целевой файловой БД
     * --target-database-path=<path>, --target-db-path=<path>
     */
    var targetDatabasePath: String? = null,
    /**
     * Создать целевую БД, если отсутствует
     * --target-create-database
     */
    var targetCreateDatabase: Boolean = false,
    /**
     * Смещение дат в целевой БД
     * --target-date-offset=<years>
     */
    var targetDateOffset: Int? = null,
    /**
     * Принудительное завершение сеансов
     * --force
     */
    var force: Boolean = false,
    /**
     * Количество потоков выгрузки
     * --jobs-count=<n>, -j <n>
     */
    var jobsCount: Int? = null,
    /**
     * Количество потоков загрузки
     * --target-jobs-count=<n>, -J <n>
     */
    var targetJobsCount: Int? = null,
    /**
     * Размер пакета строк
     * --batch-size=<n>, -B <n>
     */
    var batchSize: Int? = null,
    /**
     * Размер пакета данных (байт)
     * --batch-data-size=<n>
     */
    var batchDataSize: Int? = null,
) : IbcmdCommand {
    override val mode: String = "infobase"
    override val subCommand: String = "replicate"
    override val commandName: String = "infobase replicate"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            targetDbms?.let { args.addAll(listOf("--target-dbms", it)) }
            targetDatabaseServer?.let { args.addAll(listOf("--target-db-server", it)) }
            targetDatabaseName?.let { args.addAll(listOf("--target-db-name", it)) }
            targetDatabaseUser?.let { args.addAll(listOf("--target-db-user", it)) }
            targetDatabasePassword?.let { args.addAll(listOf("--target-db-pwd", it)) }
            if (targetRequestDatabasePassword) args.add("--target-request-db-pwd")
            targetDatabasePath?.let { args.addAll(listOf("--target-db-path", it)) }
            if (targetCreateDatabase) args.add("--target-create-database")
            targetDateOffset?.let { args.addAll(listOf("--target-date-offset", it.toString())) }
            if (force) args.add("--force")
            jobsCount?.let { args.addAll(listOf("--jobs-count", it.toString())) }
            targetJobsCount?.let { args.addAll(listOf("--target-jobs-count", it.toString())) }
            batchSize?.let { args.addAll(listOf("--batch-size", it.toString())) }
            batchDataSize?.let { args.addAll(listOf("--batch-data-size", it.toString())) }

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()

        targetDbms?.let { details.add("целевая СУБД: $it") }
        targetDatabaseServer?.let { details.add("целевой сервер: $it") }
        targetDatabaseName?.let { details.add("целевая БД: $it") }
        targetDatabasePath?.let { details.add("целевой путь: $it") }
        jobsCount?.let { details.add("потоков выгрузки: $it") }
        targetJobsCount?.let { details.add("потоков загрузки: $it") }

        return "Репликация информационной базы" +
                if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}
