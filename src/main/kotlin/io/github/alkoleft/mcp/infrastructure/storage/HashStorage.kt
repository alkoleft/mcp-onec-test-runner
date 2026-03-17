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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger { }

internal data class StorageData(
    val hashes: Map<String, String> = emptyMap(),
    val timestamps: Map<String, Long> = emptyMap(),
)

/**
 * Hash storage for a single source set.
 * Provides thread-safe operations for storing and retrieving file hashes.
 * Uses JSON file for persistence instead of MapDB.
 *
 * @param sourceSetName Name of the source set this storage belongs to
 * @param dbPath Path to the storage file (will use .json extension)
 */
class HashStorage(
    private val sourceSetName: String,
    dbPath: Path,
) {
    private val storagePath: Path = dbPath.parent.resolve(dbPath.fileName.toString().removeSuffix(".db") + ".json")
    private val mapper = ObjectMapper().apply { findAndRegisterModules() }
    private val hashMap: ConcurrentHashMap<String, String>
    private val timestampMap: ConcurrentHashMap<String, Long>

    init {
        logger.debug { "Инициализация хранилища хешей для source set '$sourceSetName' по пути: $storagePath" }
        Files.createDirectories(storagePath.parent)

        val data = if (Files.exists(storagePath)) {
            try {
                mapper.readValue<StorageData>(storagePath.toFile())
            } catch (e: Exception) {
                logger.warn { "Не удалось прочитать хранилище хешей, начинаем заново: ${e.message}" }
                StorageData()
            }
        } else {
            StorageData()
        }

        hashMap = ConcurrentHashMap(data.hashes)
        timestampMap = ConcurrentHashMap(data.timestamps)
        logger.debug { "Хранилище хешей для source set '$sourceSetName' инициализировано с ${hashMap.size} хешами" }
    }

    fun isEmpty(): Boolean = hashMap.isEmpty() || timestampMap.isEmpty()

    fun clear() {
        hashMap.clear()
        timestampMap.clear()
        persist()
    }

    fun getHash(file: Path): String? = hashMap[normalizeKey(file)]

    fun batchUpdate(updates: Map<Path, String>) {
        if (updates.isEmpty()) return
        try {
            updates.forEach { (file, hash) -> hashMap[normalizeKey(file)] = hash }
            persist()
        } catch (e: Exception) {
            logger.warn { "Не удалось сохранить хеши файлов (${updates.size} шт.) - кэш будет перестроен при следующей сборке" }
        }
    }

    fun getSourceSetTimestamp(): Long? = timestampMap[sourceSetName]

    fun storeTimestamp(timestamp: Long) {
        try {
            timestampMap[sourceSetName] = timestamp
            persist()
            logger.debug { "Временная метка сохранена для source set: $sourceSetName" }
        } catch (e: Exception) {
            logger.warn { "Не удалось сохранить временную метку для '$sourceSetName'" }
        }
    }

    fun close() {
        // No-op: data is persisted on each write
    }

    private fun persist() {
        try {
            val data = StorageData(hashes = HashMap(hashMap), timestamps = HashMap(timestampMap))
            mapper.writeValue(storagePath.toFile(), data)
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при сохранении хранилища хешей для source set '$sourceSetName'" }
        }
    }

    private fun normalizeKey(file: Path): String = file.toAbsolutePath().normalize().toString()
}
