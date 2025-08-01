package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

// Импорты общих интерфейсов и параметров

// Импорты команд режима infobase

// Импорты команд режима server

// Импорты команд режима config

// Импорты команд режима session

// Импорты команд режима lock

// Импорты команд режима mobile

// Импорты команд режима extension

/**
 * Агрегатор всех команд IBCMD
 *
 * Предоставляет удобный доступ ко всем командам различных режимов работы ibcmd.
 * Все команды декомпозированы по отдельным файлам согласно принципам SOLID.
 *
 * Поддерживаемые режимы:
 * - infobase — Управление информационной базой
 * - server — Настройка автономного сервера
 * - config — Работа с конфигурациями и расширениями
 * - session — Администрирование сеансов
 * - lock — Администрирование блокировок
 * - mobile-app — Работа с мобильным приложением
 * - mobile-client — Работа с мобильным клиентом
 * - extension — Работа с расширениями
 */
object IbcmdCommands {

    /**
     * Команды режима infobase — Управление информационной базой
     */
    object Infobase {
        // Основные команды управления ИБ
        const val CREATE = "create"
        const val DUMP = "dump"
        const val RESTORE = "restore"
        const val CLEAR = "clear"
        const val REPLICATE = "replicate"

        // Команды управления конфигурацией и расширениями в ИБ
        const val EXTENSION = "extension"
        const val GENERATION_ID = "generation-id"
        const val SIGN = "sign"
    }

    /**
     * Команды режима server — Настройка автономного сервера
     */
    object Server {
        const val CONFIG_INIT = "config init"
        const val CONFIG_IMPORT = "config import"
    }

    /**
     * Команды режима config — Работа с конфигурациями и расширениями
     */
    object Config {
        // Основные команды работы с конфигурацией
        const val LOAD = "load"
        const val SAVE = "save"
        const val CHECK = "check"
        const val APPLY = "apply"
        const val RESET = "reset"
        const val REPAIR = "repair"

        // Команды экспорта/импорта XML
        const val EXPORT = "export"
        const val IMPORT = "import"

        // Команды управления поддержкой и разделителями
        const val SUPPORT_DISABLE = "support disable"
        const val DATA_SEPARATION_LIST = "data-separation list"

        // Команды управления расширениями в конфигурации
        const val EXTENSION = "extension"
        const val GENERATION_ID = "generation-id"
        const val SIGN = "sign"
    }

    /**
     * Команды режима session — Администрирование сеансов
     */
    object Session {
        const val INFO = "info"
        const val LIST = "list"
        const val TERMINATE = "terminate"
        const val INTERRUPT_CURRENT_SERVER_CALL = "interrupt-current-server-call"
    }

    /**
     * Команды режима lock — Администрирование блокировок
     */
    object Lock {
        const val LIST = "list"
    }

    /**
     * Команды режима mobile-app — Работа с мобильным приложением
     */
    object MobileApp {
        const val EXPORT = "export"
    }

    /**
     * Команды режима mobile-client — Работа с мобильным клиентом
     */
    object MobileClient {
        const val EXPORT = "export"
        const val SIGN = "sign"
    }

    /**
     * Команды режима extension — Работа с расширениями
     */
    object Extension {
        const val CREATE = "create"
        const val INFO = "info"
        const val LIST = "list"
        const val UPDATE = "update"
        const val DELETE = "delete"
    }
}

/**
 * Типы расширений согласно спецификации
 */
enum class ExtensionPurpose(val value: String) {
    CUSTOMIZATION("customization"),
    ADD_ON("add-on"),
    PATCH("patch")
}

/**
 * Значения для булевых параметров ibcmd
 */
enum class IbcmdBoolean(val value: String) {
    YES("yes"),
    NO("no")
}

/**
 * Режимы динамического обновления конфигурации
 */
enum class DynamicUpdateMode(val value: String) {
    AUTO("auto"),
    DISABLE("disable"),
    PROMPT("prompt"),
    FORCE("force")
}

/**
 * Режимы завершения сеансов
 */
enum class SessionTerminateMode(val value: String) {
    DISABLE("disable"),
    PROMPT("prompt"),
    FORCE("force")
}

/**
 * Области действия расширений
 */
enum class ExtensionScope(val value: String) {
    INFOBASE("infobase"),
    DATA_SEPARATION("data-separation")
}

/**
 * Типы СУБД поддерживаемые ibcmd
 */
enum class DbmsType(val value: String) {
    MSSQL_SERVER("MSSQLServer"),
    POSTGRESQL("PostgreSQL"),
    IBM_DB2("IBMDB2"),
    ORACLE_DATABASE("OracleDatabase")
}

/**
 * Флаги выдачи лицензий и планирования заданий для автономного сервера
 */
enum class ServerFlag(val value: String) {
    ALLOW("allow"),
    DENY("deny")
} 