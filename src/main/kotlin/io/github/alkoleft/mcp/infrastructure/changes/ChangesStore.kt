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

package io.github.alkoleft.mcp.infrastructure.changes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger { }

class ChangesStore(
    private val storagePath: Path,
) {
    private val mapper = ObjectMapper().apply { findAndRegisterModules() }
    private val hashMap: ConcurrentHashMap<String, String>

    init {
        Files.createDirectories(storagePath.parent)
        hashMap = if (Files.exists(storagePath)) {
            try {
                ConcurrentHashMap(mapper.readValue<Map<String, String>>(storagePath.toFile()))
            } catch (e: Exception) {
                logger.warn { "Не удалось прочитать ChangesStore, начинаем заново: ${e.message}" }
                ConcurrentHashMap()
            }
        } else {
            ConcurrentHashMap()
        }
    }

    fun isEmpty() = hashMap.isEmpty()

    fun getHash(file: Path): String? = hashMap[normalizeKey(file)]

    fun batchUpdate(updates: Map<Path, String>) {
        if (updates.isEmpty()) return
        try {
            updates.forEach { (file, hash) -> hashMap[normalizeKey(file)] = hash }
            mapper.writeValue(storagePath.toFile(), HashMap(hashMap))
        } catch (e: Exception) {
            logger.error(e) { "Не удалось выполнить пакетное обновление хешей файлов" }
            throw e
        }
    }

    private fun normalizeKey(file: Path): String = file.toAbsolutePath().normalize().toString()
}
