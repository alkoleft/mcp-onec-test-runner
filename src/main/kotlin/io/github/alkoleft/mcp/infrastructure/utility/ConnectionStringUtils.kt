package io.github.alkoleft.mcp.infrastructure.utility

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Утилитный класс для обработки строк подключения 1С, специально для баз типа File=.
 * Предоставляет функции для извлечения путей, разрешения относительных путей и форматирования для CLI.
 */
object ConnectionStringUtils {
    /**
     * Извлекает сырой путь к базе данных из строки подключения File=.
     * Поддерживает пути в одинарных/двойных кавычках или без, за которыми может следовать точка с запятой.
     *
     * @param connectionString Исходная строка подключения.
     * @return Извлеченный сырой путь или null, если не File= или путь не найден.
     */
    private fun extractRawPath(connectionString: String): String? {
        if (!connectionString.startsWith("File=")) return null

        val pathMatch =
            Regex("""File=(?:'([^']+)'|"([^"]+)"|([^;]+));?""").find(connectionString)
                ?: return null

        return when {
            !pathMatch.groupValues[1].isEmpty() -> pathMatch.groupValues[1] // Одинарные кавычки
            !pathMatch.groupValues[2].isEmpty() -> pathMatch.groupValues[2] // Двойные кавычки
            else -> pathMatch.groupValues[3] // Без кавычек
        }
    }

    /**
     * Разрешает путь к базе данных относительно базового пути, если необходимо.
     *
     * @param rawPath Сырой извлеченный путь.
     * @param basePath Базовый путь для разрешения.
     * @return Разрешенный абсолютный путь.
     */
    private fun resolvePath(
        rawPath: String,
        basePath: Path,
    ): String =
        if (rawPath.startsWith('/') || rawPath.matches(Regex("""[A-Za-z]:[/\\].*"""))) {
            // Абсолютный путь
            Paths.get(rawPath).toAbsolutePath().toString()
        } else {
            // Относительный путь
            val baseString = basePath.toString().removeSuffix("/")
            Paths.get("$baseString/$rawPath").toAbsolutePath().toString()
        }

    /**
     * Форматирует путь к базе данных в строку подключения File= подходящую для CLI.
     * Добавляет кавычки, если путь содержит пробелы или точки с запятой.
     *
     * @param dbPath Абсолютный путь к базе.
     * @return Форматированная строка "File=path".
     */
    private fun formatFileConnection(dbPath: String): String {
        val needsQuotes = dbPath.contains(" ") || dbPath.contains(";")
        return if (needsQuotes) "File=\"$dbPath\"" else "File=$dbPath"
    }

    /**
     * Извлекает и разрешает путь к базе данных из строки подключения.
     * Возвращает разрешенный абсолютный путь, если это File=, иначе исходную строку.
     *
     * @param connectionString Исходная строка подключения.
     * @param basePath Базовый путь для разрешения относительных путей.
     * @return Разрешенный путь или исходная строка, если не File=.
     */
    fun extractDbPath(
        connectionString: String,
        basePath: Path,
    ): String {
        val rawPath = extractRawPath(connectionString) ?: return connectionString
        return resolvePath(rawPath, basePath)
    }

    /**
     * Нормализует строку подключения для использования в CLI.
     * Для File= извлекает, разрешает и форматирует с подходящими кавычками.
     * Для других типов возвращает без изменений.
     *
     * @param connectionString Исходная строка подключения.
     * @param basePath Базовый путь для разрешения относительных путей.
     * @return Нормализованная строка подключения.
     */
    fun normalizeForCli(
        connectionString: String,
        basePath: Path,
    ): String {
        if (!connectionString.startsWith("File=")) return connectionString

        val rawPath = extractRawPath(connectionString) ?: return connectionString
        val resolvedPath = resolvePath(rawPath, basePath)
        return formatFileConnection(resolvedPath)
    }
}

