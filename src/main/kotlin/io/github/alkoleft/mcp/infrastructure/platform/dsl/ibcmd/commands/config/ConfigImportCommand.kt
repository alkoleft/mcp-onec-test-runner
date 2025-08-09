package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 8. import — Импорт конфигурации из XML
 *
 * Импортирует конфигурацию из XML формата.
 * Подкоманды: files, all-extensions
 */
class ConfigImportCommand(
    /**
     * Подкоманда импорта (files, all-extensions)
     */
    var importSubCommand: String? = null,
    /**
     * Файл для записи конфигурации
     * --out=<file>, -o <file>
     */
    var out: String? = null,
    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    var extension: String? = null,
    /**
     * Базовый каталог XML файлов
     * --base-dir=<directory>
     */
    var baseDir: String? = null,
    /**
     * Путь к архиву XML файлов
     * --archive=<path>
     */
    var archivePath: String? = null,
    /**
     * Отключить проверку метаданных
     * --no-check
     */
    var noCheck: Boolean = false,
    /**
     * Разрешить частичный набор файлов
     * --partial
     */
    var partial: Boolean = false,
    /**
     * Путь к каталогу или архиву XML
     */
    val path: String,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "import" + (importSubCommand?.let { " $it" } ?: "")
    override val commandName: String = "config import" + (importSubCommand?.let { " $it" } ?: "")

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            importSubCommand?.let { args.add(it) }
            out?.let { args.addAll(listOf("--out", it)) }
            extension?.let { args.addAll(listOf("--extension", it)) }
            baseDir?.let { args.addAll(listOf("--base-dir", it)) }
            archivePath?.let { args.addAll(listOf("--archive", it)) }
            if (noCheck) args.add("--no-check")
            if (partial) args.add("--partial")
            args.add(path)
            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()

        importSubCommand?.let { details.add("подкоманда: $it") }
        extension?.let { details.add("расширение: $it") }
        out?.let { details.add("файл записи: $it") }
        archivePath?.let { details.add("архив: $it") }
        if (noCheck) details.add("без проверки")
        if (partial) details.add("частичный импорт")

        if (extension.isNullOrBlank()) "конфигурации" else "расширения"
        return "Импорт ${if (extension.isNullOrBlank()) "конфигурации" else "расширения"} из XML: $path" +
            if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}
