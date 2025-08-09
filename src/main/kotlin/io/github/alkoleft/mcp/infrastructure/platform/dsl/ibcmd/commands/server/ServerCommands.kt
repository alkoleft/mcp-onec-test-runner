package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.server

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 1. config init — Инициализация конфигурации автономного сервера
 *
 * Создаёт конфигурационный файл для автономного сервера.
 */
data class ServerConfigInitCommand(
    /**
     * Путь к файлу для записи конфигурации
     * --out=<file>, -o <file>
     */
    val out: String? = null,
    /**
     * IP адрес сервера (localhost, any, IPv4, IPv6)
     * --http-address=<address>, --address=<address>, -a <address>
     */
    val httpAddress: String? = null,
    /**
     * TCP порт (по умолчанию: 8314)
     * --http-port=<number>, --port=<number>, -p <number>
     */
    val httpPort: Int? = null,
    /**
     * Базовый путь публикации (по умолчанию: /)
     * --http-base=<location>, --base=<location>, -b <location>
     */
    val httpBase: String? = null,
    /**
     * Имя информационной базы
     * --name=<name>, -n <name>
     */
    val name: String? = null,
    /**
     * Идентификатор ИБ (UUID или auto)
     * --id=<uuid>
     */
    val id: String? = null,
    /**
     * Выдача клиентских лицензий (allow/deny)
     * --distribute-licenses=<flag>
     */
    val distributeLicenses: String? = null,
    /**
     * Планирование регламентных заданий (allow/deny)
     * --schedule-jobs=<flag>
     */
    val scheduleJobs: String? = null,
    /**
     * Запрет локального распознавания речи (yes/no)
     * --disable-local-speech-to-text=<flag>
     */
    val disableLocalSpeechToText: String? = null,
) : IbcmdCommand {
    override val mode: String = "server"
    override val subCommand: String = "config init"
    override val commandName: String = "server config init"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            out?.let { args.addAll(listOf("--out", it)) }
            httpAddress?.let { args.addAll(listOf("--http-address", it)) }
            httpPort?.let { args.addAll(listOf("--http-port", it.toString())) }
            httpBase?.let { args.addAll(listOf("--http-base", it)) }
            name?.let { args.addAll(listOf("--name", it)) }
            id?.let { args.addAll(listOf("--id", it)) }
            distributeLicenses?.let { args.addAll(listOf("--distribute-licenses", it)) }
            scheduleJobs?.let { args.addAll(listOf("--schedule-jobs", it)) }
            disableLocalSpeechToText?.let { args.addAll(listOf("--disable-local-speech-to-text", it)) }

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()

        out?.let { details.add("файл конфигурации: $it") }
        name?.let { details.add("имя ИБ: $it") }
        httpAddress?.let { details.add("адрес: $it") }
        httpPort?.let { details.add("порт: $it") }
        httpBase?.let { details.add("базовый путь: $it") }

        return "Инициализация конфигурации автономного сервера" +
            if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}

/**
 * 2. config import — Импорт конфигурации из кластера серверов 1С
 *
 * Импортирует конфигурацию из кластера серверов 1С:Предприятие.
 */
data class ServerConfigImportCommand(
    /**
     * Путь к каталогу данных центрального сервера
     * --cluster-data=<path>
     */
    val clusterData: String? = null,
    /**
     * Порт менеджера кластера (по умолчанию: 1541)
     * --manager-port=<port>
     */
    val managerPort: Int? = null,
    /**
     * Имя информационной базы (обязательно)
     * --name=<name>, -n <name>
     */
    val name: String? = null,
    /**
     * Путь к файлу для записи конфигурации
     * --out=<file>, -o <file>
     */
    val out: String? = null,
    /**
     * IP адрес сервера
     * --address=<address>, -a <address>
     */
    val address: String? = null,
    /**
     * TCP порт
     * --port=<number>
     */
    val port: Int? = null,
    /**
     * Базовый путь публикации
     * --base=<location>, -b <location>
     */
    val base: String? = null,
    /**
     * Путь к файлу дескриптора публикации
     * --publication=<path>, -p <path>
     */
    val publication: String? = null,
) : IbcmdCommand {
    override val mode: String = "server"
    override val subCommand: String = "config import"
    override val commandName: String = "server config import"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            clusterData?.let { args.addAll(listOf("--cluster-data", it)) }
            managerPort?.let { args.addAll(listOf("--manager-port", it.toString())) }
            name?.let { args.addAll(listOf("--name", it)) }
            out?.let { args.addAll(listOf("--out", it)) }
            address?.let { args.addAll(listOf("--address", it)) }
            port?.let { args.addAll(listOf("--port", it.toString())) }
            base?.let { args.addAll(listOf("--base", it)) }
            publication?.let { args.addAll(listOf("--publication", it)) }

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()

        name?.let { details.add("ИБ: $it") }
        clusterData?.let { details.add("данные кластера: $it") }
        out?.let { details.add("файл конфигурации: $it") }
        managerPort?.let { details.add("порт менеджера: $it") }

        return "Импорт конфигурации из кластера серверов 1С" +
            if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}
