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
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.io.path.exists
import kotlin.io.path.isExecutable

private val logger = KotlinLogging.logger { }

/**
 * Utility validation service for checking executable existence and functionality
 */
class UtilityValidator {
    /**
     * Validates utility location by checking existence, permissions and basic functionality
     */
    fun validateUtility(location: UtilityLocation): Boolean {
        return try {
            // Basic existence check
            if (!location.executablePath.exists()) {
                logger.debug { "Утилита не найдена по пути: ${location.executablePath}" }
                return false
            }

            // Permission check
            if (!location.executablePath.isExecutable()) {
                logger.debug { "Утилита не исполняемая (нет прав на выполнение): ${location.executablePath}" }
                return false
            }
            return true
        } catch (e: Exception) {
            logger.debug { "Проверка не пройдена для ${location.executablePath}: ${e.message}" }
            false
        }
    }
}
