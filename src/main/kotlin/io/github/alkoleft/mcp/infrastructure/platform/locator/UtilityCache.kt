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

package io.github.alkoleft.mcp.infrastructure.platform.locator

import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger { }

/**
 * Multi-level caching system with TTL and validation for utility locations
 */
class UtilityCache {
    private val memoryCache = ConcurrentHashMap<CacheKey, UtilityLocation>()

    data class CacheKey(
        val utility: UtilityType,
        val version: String?,
    )

    fun getCachedLocation(
        utility: UtilityType,
        version: String?,
    ): UtilityLocation? {
        val key = CacheKey(utility, version)
        val entry = memoryCache[key]

        return if (entry != null) {
            logger.debug { "Попадание в кэш для утилиты: $utility, версия: $version" }
            entry
        } else {
            logger.debug { "Промах кэша для утилиты: $utility, версия: $version" }
            null
        }
    }

    fun store(
        utility: UtilityType,
        version: String?,
        location: UtilityLocation,
    ) {
        val key = CacheKey(utility, version)
        memoryCache[key] = location
        logger.debug { "Сохранено в кэш: $utility, версия: $version по пути ${location.executablePath}" }
    }

    fun invalidate(
        utility: UtilityType,
        version: String?,
    ) {
        val key = CacheKey(utility, version)
        memoryCache.remove(key)
        logger.debug { "Кэш инвалидирован для: $utility, версия: $version" }
    }
}
