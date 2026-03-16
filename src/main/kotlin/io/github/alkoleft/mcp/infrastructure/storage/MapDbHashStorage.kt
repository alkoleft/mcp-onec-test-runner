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
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

/**
 * Factory for creating and managing HashStorage instances per source set.
 * Each source set gets its own isolated database file.
 */
@Component
class MapDbHashStorageFactory(
    private val properties: ApplicationProperties,
) {
    private val storages = mutableMapOf<String, HashStorage>()

    private val dbDirectory: Path by lazy {
        properties.workPath.resolve("hash-storages").also {
            Files.createDirectories(it)
        }
    }

    /**
     * Gets or creates a HashStorage for the specified source set name.
     * Each source set gets its own isolated database file.
     */
    fun getStorage(sourceSetName: String): HashStorage =
        synchronized(storages) {
            storages.getOrPut(sourceSetName) {
                val dbPath = dbDirectory.resolve("$sourceSetName.db")
                logger.debug { "Создание HashStorage для source set: $sourceSetName" }
                HashStorage(sourceSetName, dbPath)
            }
        }

    /**
     * Closes all storage instances
     */
    fun closeAll() {
        synchronized(storages) {
            storages.values.forEach { it.close() }
            storages.clear()
        }
    }

    @PreDestroy
    private fun destroy() {
        closeAll()
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
