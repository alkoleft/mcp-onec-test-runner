package io.github.alkoleft.mcp.infrastructure.storage

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentMap

/**
 * MapDB-based persistent storage for file hashes.
 * Implements thread-safe operations with atomic transactions and efficient batch updates.
 */
private val logger = KotlinLogging.logger { }

@Component
class MapDbHashStorage(
    properties: ApplicationProperties,
) {
    private lateinit var db: DB
    private lateinit var hashMap: ConcurrentMap<String, String>
    private lateinit var timestampMap: ConcurrentMap<String, Long>

    private val dbPath = properties.workPath.resolve("file-hashes.db")

    @PostConstruct
    private fun initialize() {
        logger.info { "Инициализация хранилища хешей MapDB по пути: $dbPath" }

        try {
            // Ensure directory exists
            Files.createDirectories(dbPath.parent)

            // Initialize MapDB with optimized settings
            db =
                DBMaker
                    .fileDB(dbPath.toFile())
                    .transactionEnable()
                    .closeOnJvmShutdown()
                    .fileMmapEnable()
                    .make()

            // Create hash map for file content hashes
            hashMap =
                db
                    .hashMap("file_hashes")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .createOrOpen()

            // Create timestamp map for file modification times
            timestampMap =
                db
                    .hashMap("file_timestamps")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.LONG)
                    .createOrOpen()

            logger.info { "Хранилище хешей MapDB инициализировано с ${hashMap.size} существующими хешами файлов" }
        } catch (e: Exception) {
            logger.error(e) { "Не удалось инициализировать хранилище хешей MapDB" }
            throw RuntimeException("Не удалось инициализировать хранилище хешей", e)
        }
    }

    fun isEmpty() = hashMap.isEmpty() || timestampMap.isEmpty()

    fun getHash(file: Path): String? =
        try {
            val key = normalizeKey(file)
            hashMap[key]
        } catch (e: Exception) {
            logger.debug(e) { "Не удалось получить хеш для файла: $file" }
            null
        }

    fun storeHash(
        file: Path,
        hash: String,
    ) = try {
        val key = normalizeKey(file)
        val timestamp = Files.getLastModifiedTime(file).toMillis()

        hashMap[key] = hash
        timestampMap[key] = timestamp

        db.commit()

        logger.debug { "Хеш сохранен для файла: $file" }
    } catch (e: Exception) {
        logger.error(e) { "Не удалось сохранить хеш для файла: $file" }
        db.rollback()
        throw e
    }

    fun batchUpdate(updates: Map<Path, String>) {
        if (updates.isEmpty()) return
        try {
            logger.debug { "Начало пакетного обновления для ${updates.size} файлов" }

            for ((file, hash) in updates) {
                val key = normalizeKey(file)
                val timestamp =
                    if (Files.exists(file)) {
                        Files.getLastModifiedTime(file).toMillis()
                    } else {
                        System.currentTimeMillis()
                    }

                hashMap[key] = hash
                timestampMap[key] = timestamp
            }

            db.commit()

            logger.info { "Пакетно обновлено ${updates.size} хешей файлов" }
        } catch (e: Exception) {
            logger.error(e) { "Не удалось выполнить пакетное обновление хешей файлов" }
            db.rollback()
            throw e
        }
    }

    fun removeHash(file: Path) =
        try {
            val key = normalizeKey(file)

            hashMap.remove(key)
            timestampMap.remove(key)

            db.commit()

            logger.debug { "Хеш удален для файла: $file" }
        } catch (e: Exception) {
            logger.error(e) { "Не удалось удалить хеш для файла: $file" }
            db.rollback()
            throw e
        }

    fun getAllHashes(): Map<String, String> =
        try {
            HashMap(hashMap)
        } catch (e: Exception) {
            logger.error(e) { "Не удалось получить все хеши" }
            emptyMap()
        }

    /**
     * Gets the stored timestamp for a file
     */
    fun getTimestamp(file: Path): Long? =
        try {
            val key = normalizeKey(file)
            timestampMap[key]
        } catch (e: Exception) {
            logger.debug(e) { "Не удалось получить временную метку для файла: $file" }
            null
        }

    /**
     * Stores the timestamp for a file
     */
    fun storeTimestamp(
        file: Path,
        timestamp: Long,
    ) = try {
        val key = normalizeKey(file)
        timestampMap[key] = timestamp
        db.commit()

        logger.debug { "Временная метка сохранена для файла: $file" }
    } catch (e: Exception) {
        logger.error(e) { "Не удалось сохранить временную метку для файла: $file" }
        db.rollback()
        throw e
    }

    /**
     * Gets statistics about the hash storage
     */
    fun getStorageStats(): HashStorageStats =
        try {
            HashStorageStats(
                totalFiles = hashMap.size,
                dbSizeBytes = Files.size(dbPath),
                oldestTimestamp = timestampMap.values.minOrNull(),
                newestTimestamp = timestampMap.values.maxOrNull(),
            )
        } catch (e: Exception) {
            logger.error(e) { "Не удалось получить статистику хранилища" }
            HashStorageStats(0, 0, null, null)
        }

    /**
     * Performs cleanup of old entries beyond the specified retention period
     */
    fun cleanup(retentionDays: Int = 30) {
        try {
            val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
            val keysToRemove = mutableListOf<String>()

            for ((key, timestamp) in timestampMap) {
                if (timestamp < cutoffTime) {
                    keysToRemove.add(key)
                }
            }

            for (key in keysToRemove) {
                hashMap.remove(key)
                timestampMap.remove(key)
            }

            db.commit()

            logger.info { "Очищено ${keysToRemove.size} старых записей хешей (срок хранения: $retentionDays дней)" }
        } catch (e: Exception) {
            logger.error(e) { "Не удалось очистить старые записи хешей" }
            db.rollback()
        }
    }

    /**
     * Normalizes file path to a consistent string key
     */
    private fun normalizeKey(file: Path): String = file.toAbsolutePath().normalize().toString()

    fun close() {
        try {
            logger.info { "Закрытие хранилища хешей MapDB" }

            if (::db.isInitialized && !db.isClosed()) {
                db.commit()
                db.close()
            }

            logger.info { "Хранилище хешей MapDB успешно закрыто" }
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при закрытии хранилища хешей MapDB" }
        }
    }

    @PreDestroy
    private fun destroy() {
        close()
    }
}

/**
 * Statistics about the hash storage
 */
data class HashStorageStats(
    val totalFiles: Int,
    val dbSizeBytes: Long,
    val oldestTimestamp: Long?,
    val newestTimestamp: Long?,
)
