package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand
import java.nio.file.Path

/**
 * 7. export — Экспорт конфигурации в XML
 *
 * Экспортирует конфигурацию в XML формат.
 * Подкоманды: info, status, objects, all-extensions
 */
data class ConfigExportCommand(
    /**
     * Подкоманда экспорта (info, status, objects, all-extensions)
     */
    var exportSubCommand: String? = null,
    /**
     * Файл информации о конфигурации
     * --base=<file>, -b <file>
     */
    var base: String? = null,
    /**
     * Файл конфигурации
     * --file=<file>, -f <file>
     */
    var file: String? = null,
    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    var extension: String? = null,
    /**
     * Синхронизация с конфигурацией
     * --sync
     */
    var sync: Boolean = false,
    /**
     * Полная выгрузка
     * --force
     */
    var force: Boolean = false,
    /**
     * Количество потоков
     * --threads=<n>, -T <n>
     */
    var threads: Int? = null,
    /**
     * Упаковать в архив
     * --archive, -A
     */
    var archive: Boolean = false,
    /**
     * Игнорировать неразрешимые ссылки
     * --ignore-unresolved-refs
     */
    var ignoreUnresolvedRefs: Boolean = false,
    /**
     * Путь к каталогу экспорта
     */
    val path: Path,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "export" + (exportSubCommand?.let { " $it" } ?: "")
    override val commandName: String = "config export" + (exportSubCommand?.let { " $it" } ?: "")

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            exportSubCommand?.let { args.add(it) }
            base?.let { args.addAll(listOf("--base", it)) }
            file?.let { args.addAll(listOf("--file", it)) }
            extension?.let { args.addAll(listOf("--extension", it)) }
            if (sync) args.add("--sync")
            if (force) args.add("--force")
            threads?.let { args.addAll(listOf("--threads", it.toString())) }
            if (archive) args.add("--archive")
            if (ignoreUnresolvedRefs) args.add("--ignore-unresolved-refs")
            args.add(path.toString())

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()

        exportSubCommand?.let { details.add("подкоманда: $it") }
        extension?.let { details.add("расширение: $it") }
        threads?.let { details.add("потоков: $it") }
        if (sync) details.add("синхронизация")
        if (force) details.add("полная выгрузка")
        if (archive) details.add("архив")
        if (ignoreUnresolvedRefs) details.add("игнорировать ссылки")

        return "Экспорт конфигурации в XML: $path" +
            if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}
