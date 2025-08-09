package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

/**
 * Типы расширений согласно спецификации
 */
enum class ExtensionPurpose(
    val value: String,
) {
    CUSTOMIZATION("customization"),
    ADD_ON("add-on"),
    PATCH("patch"),
}

/**
 * Значения для булевых параметров ibcmd
 */
enum class IbcmdBoolean(
    val value: String,
) {
    YES("yes"),
    NO("no"),
}

/**
 * Режимы динамического обновления конфигурации
 */
enum class DynamicUpdateMode(
    val value: String,
) {
    AUTO("auto"),
    DISABLE("disable"),
    PROMPT("prompt"),
    FORCE("force"),
}

/**
 * Режимы завершения сеансов
 */
enum class SessionTerminateMode(
    val value: String,
) {
    DISABLE("disable"),
    PROMPT("prompt"),
    FORCE("force"),
}

/**
 * Области действия расширений
 */
enum class ExtensionScope(
    val value: String,
) {
    INFOBASE("infobase"),
    DATA_SEPARATION("data-separation"),
}

/**
 * Типы СУБД поддерживаемые ibcmd
 */
enum class DbmsType(
    val value: String,
) {
    MSSQL_SERVER("MSSQLServer"),
    POSTGRESQL("PostgreSQL"),
    IBM_DB2("IBMDB2"),
    ORACLE_DATABASE("OracleDatabase"),
}

/**
 * Флаги выдачи лицензий и планирования заданий для автономного сервера
 */
enum class ServerFlag(
    val value: String,
) {
    ALLOW("allow"),
    DENY("deny"),
}
