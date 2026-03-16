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

package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.common.DumpAction
import io.github.alkoleft.mcp.application.actions.common.DumpMode
import io.github.alkoleft.mcp.application.actions.common.DumpResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger { }

/**
 * Запрос на выгрузку конфигурации
 *
 * @param mode Режим выгрузки (по умолчанию FULL)
 * @param extension Имя расширения для выгрузки (null - основная конфигурация)
 * @param objects Список объектов для частичной выгрузки (только для режима PARTIAL)
 */
data class DumpRequest(
    val mode: DumpMode = DumpMode.FULL,
    val extension: String? = null,
    val objects: List<String> = emptyList(),
)

/**
 * Сервис для выгрузки конфигурации из ИБ в файлы проекта
 *
 * Предоставляет высокоуровневый интерфейс для выгрузки конфигурации
 * с автоматическим выбором целевого каталога на основе конфигурации проекта.
 */
@Service
class DumpService(
    private val dumpAction: DumpAction,
    private val properties: ApplicationProperties,
    private val sourceSetsService: SourceSetsService,
) {
    /**
     * Выполняет выгрузку конфигурации согласно запросу
     *
     * @param request Запрос на выгрузку с указанием режима и параметров
     * @return Результат выгрузки
     */
    fun dump(request: DumpRequest): DumpResult {
        val normalizedExtension = request.extension?.trim()?.takeIf { it.isNotEmpty() }
        logger.info {
            "Выгрузка конфигурации: режим=${request.mode}, " +
                "расширение=${normalizedExtension ?: "основная конфигурация"}"
        }

        val sourceSetContext = sourceSetsService.getDesignerSourceSet()!!

        return when (request.mode) {
            DumpMode.FULL ->
                dumpAction.run(
                    properties = properties,
                    sourceSet = sourceSetContext.sourceSet,
                    extension = normalizedExtension,
                )

            DumpMode.INCREMENTAL ->
                dumpAction.runIncremental(
                    properties = properties,
                    sourceSet = sourceSetContext.sourceSet,
                    extension = normalizedExtension,
                )

            DumpMode.PARTIAL -> {
                if (request.objects.isEmpty()) {
                    return DumpResult(
                        message = "Для режима PARTIAL необходимо указать список объектов",
                        success = false,
                        mode = DumpMode.PARTIAL,
                        errors = listOf("Список объектов пуст"),
                    )
                }
                dumpAction.runPartial(
                    properties = properties,
                    sourceSet = sourceSetContext.sourceSet,
                    objects = request.objects,
                    extension = normalizedExtension,
                )
            }
        }
    }
}
