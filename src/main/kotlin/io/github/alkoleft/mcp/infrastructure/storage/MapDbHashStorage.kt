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
                    .hashMap("subproject_timestamps")
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

    fun batchUpdate(updates: Map<Path, String>) {
        if (updates.isEmpty()) return
        try {
            for ((file, hash) in updates) {
                val key = normalizeKey(file)
                hashMap[key] = hash
            }
            db.commit()
        } catch (e: Exception) {
            logger.error(e) { "Не удалось выполнить пакетное обновление хешей файлов" }
            db.rollback()
            throw e
        }
    }

    /**
     * Gets the stored timestamp for a file
     */
    fun getSourceSetTimestamp(sourceSetName: String): Long? =
        try {
            timestampMap[sourceSetName]
        } catch (e: Exception) {
            logger.debug(e) { "Не удалось получить временную метку для проекта: $sourceSetName" }
            null
        }

    /**
     * Stores the timestamp for a file
     */
    fun storeTimestamp(
        sourceSetName: String,
        timestamp: Long,
    ) = try {
        timestampMap[sourceSetName] = timestamp
        db.commit()

        logger.debug { "Временная метка сохранена для подпроекта: $sourceSetName" }
    } catch (e: Exception) {
        logger.error(e) { "Не удалось сохранить временную метку для подпроекта: $sourceSetName" }
        db.rollback()
        throw e
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
