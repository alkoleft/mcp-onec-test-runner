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

package io.github.alkoleft.mcp.infrastructure.storage

import io.github.alkoleft.mcp.application.core.PlatformType
import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import io.github.oshai.kotlinlogging.KotlinLogging
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentMap

private val logger = KotlinLogging.logger { }

/**
 * Hash storage for a single source set.
 * Provides thread-safe operations for storing and retrieving file hashes.
 *
 * @param sourceSetName Name of the source set this storage belongs to
 * @param dbPath Path to the database file
 */
class HashStorage(
    private val sourceSetName: String,
    dbPath: Path,
) {
    private val db: DB
    private val hashMap: ConcurrentMap<String, String>
    private val timestampMap: ConcurrentMap<String, Long>

    init {
        logger.info { "Инициализация хранилища хешей для source set '$sourceSetName' по пути: $dbPath" }

        try {
            // Ensure directory exists
            Files.createDirectories(dbPath.parent)

            db = createDb(dbPath)

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
                    .hashMap("subproject_timestamps")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.LONG)
                    .createOrOpen()

            logger.info { "Хранилище хешей для source set '$sourceSetName' инициализировано с ${hashMap.size} хешами" }
        } catch (e: Exception) {
            logger.error(e) { "Не удалось инициализировать хранилище хешей для source set '$sourceSetName'" }
            throw RuntimeException("Не удалось инициализировать хранилище хешей для source set '$sourceSetName'", e)
        }
    }

    fun isEmpty(): Boolean = hashMap.isEmpty() || timestampMap.isEmpty()

    /**
     * Gets the stored hash for a file
     */
    fun getHash(file: Path): String? =
        try {
            val key = normalizeKey(file)
            hashMap[key]
        } catch (e: Exception) {
            logger.debug(e) { "Не удалось получить хеш для файла: $file" }
            null
        }

    /**
     * Batch update hashes for multiple files
     */
    fun batchUpdate(updates: Map<Path, String>) {
        if (updates.isEmpty()) return
        try {
            for ((file, hash) in updates) {
                val key = normalizeKey(file)
                hashMap[key] = hash
            }
            db.commit()
        } catch (e: Exception) {
            logger.warn { "Не удалось сохранить хеши файлов (${updates.size} шт.) - кэш будет перестроен при следующей сборке" }
            logger.debug(e) { "Детали ошибки сохранения хешей" }
            tryRollback()
        }
    }

    /**
     * Gets the stored timestamp for this source set
     */
    fun getSourceSetTimestamp(): Long? =
        try {
            timestampMap[sourceSetName]
        } catch (e: Exception) {
            logger.debug(e) { "Не удалось получить временную метку для source set: $sourceSetName" }
            null
        }

    /**
     * Stores the timestamp for this source set
     */
    fun storeTimestamp(timestamp: Long) =
        try {
            timestampMap[sourceSetName] = timestamp
            db.commit()

            logger.debug { "Временная метка сохранена для source set: $sourceSetName" }
        } catch (e: Exception) {
            logger.warn { "Не удалось сохранить временную метку для '$sourceSetName' - кэш будет перестроен при следующей сборке" }
            logger.debug(e) { "Детали ошибки сохранения временной метки" }
            tryRollback()
        }

    /**
     * Safe transaction rollback
     */
    private fun tryRollback() {
        try {
            db.rollback()
        } catch (e: Exception) {
            logger.debug { "Rollback не выполнен: ${e.message}" }
        }
    }

    /**
     * Normalizes file path to a consistent string key
     */
    private fun normalizeKey(file: Path): String = file.toAbsolutePath().normalize().toString()

    /**
     * Closes the storage and releases resources
     */
    fun close() {
        try {
            logger.info { "Закрытие хранилища хешей для source set: $sourceSetName" }

            if (!db.isClosed()) {
                db.commit()
                db.close()
            }

            logger.info { "Хранилище хешей для source set '$sourceSetName' успешно закрыто" }
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при закрытии хранилища хешей для source set '$sourceSetName'" }
        }
    }
}

private fun createDb(dbPath: Path): DB {
    val dbMaker =
        DBMaker
            .fileDB(dbPath.toFile())
            .transactionEnable()
            .closeOnJvmShutdown()

    if (shouldUseMemoryMapping(PlatformDetector.current)) {
        dbMaker.fileMmapEnable()
    } else {
        logger.info { "Для Windows отключено memory-mapped хранилище MapDB для обхода проблем с WAL/truncate" }
    }

    return dbMaker.make()
}

internal fun shouldUseMemoryMapping(platform: PlatformType): Boolean = platform != PlatformType.WINDOWS
