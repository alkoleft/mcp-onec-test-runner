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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import io.github.alkoleft.mcp.application.core.UtilityLocation
import io.github.alkoleft.mcp.application.core.UtilityType
import io.github.alkoleft.mcp.application.services.EdtCliStartService
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.edt.EdtCliExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.CommandExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityLocator
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

private const val DEFAULT_VERSION = "default"

/**
 * Контекст для работы с утилитами платформы 1С
 *
 * @param utilLocator локатор утилит платформы
 * @param properties свойства приложения
 * @param applicationContext контекст приложения Spring
 */
@Component
class PlatformUtilities(
    private val utilLocator: UtilityLocator,
    private val properties: ApplicationProperties,
    private val applicationContext: ApplicationContext,
) {
    /**
     * Получает локацию утилиты
     *
     * @param utilityType тип утилиты
     * @param version версия утилиты
     * @return локация утилиты
     */
    fun locateUtility(
        utilityType: UtilityType,
        version: String = DEFAULT_VERSION,
    ): UtilityLocation =
        utilLocator.locateUtility(
            utilityType,
            version = actualVersion(utilityType, version),
        )

    /**
     * Определяет фактическую версию утилиты
     *
     * @param utilityType тип утилиты
     * @param version указанная версия
     * @return фактическая версия утилиты
     */
    private fun actualVersion(
        utilityType: UtilityType,
        version: String,
    ) = if (version == DEFAULT_VERSION) {
        if (utilityType.isPlatform()) properties.platformVersion else properties.tools.edtCli.version
    } else {
        version
    }

    /**
     * Получает путь к указанной утилите
     *
     * @param utilityType тип утилиты
     * @param version версия утилиты
     * @return путь к исполняемому файлу утилиты или null если утилита не найдена
     */
    fun getUtilityPath(
        utilityType: UtilityType,
        version: String = DEFAULT_VERSION,
    ): String? =
        try {
            val location = locateUtility(utilityType, version)
            location.executablePath.toString()
        } catch (e: Exception) {
            null
        }

    /**
     * Получает исполнитель команд для указанной утилиты
     *
     * @param utilityType тип утилиты
     * @return исполнитель команд
     * @throws IllegalStateException если EDT CLI не запущено в интерактивном режиме
     */
    fun executor(utilityType: UtilityType): CommandExecutor {
        if (utilityType == UtilityType.EDT_CLI && properties.tools.edtCli.interactiveMode) {
            val service = applicationContext.getBean(EdtCliStartService::class.java)
            val executor = service.interactiveExecutor()
            return executor?.let { EdtCliExecutor(it) } ?: throw IllegalStateException("EDT cli не запущено, попробуйте позже")
        } else {
            return ProcessExecutor()
        }
    }
}
