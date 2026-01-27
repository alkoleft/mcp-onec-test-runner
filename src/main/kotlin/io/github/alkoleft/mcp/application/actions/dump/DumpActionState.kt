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

package io.github.alkoleft.mcp.application.actions.dump

import io.github.alkoleft.mcp.application.actions.common.ActionState
import io.github.alkoleft.mcp.application.actions.common.DumpMode
import io.github.alkoleft.mcp.application.actions.common.DumpResult
import io.github.alkoleft.mcp.application.actions.common.toActionStepResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

/**
 * Состояние выполнения выгрузки конфигурации
 *
 * Отслеживает результаты выполнения команд выгрузки.
 */
class DumpActionState(
    private val mode: DumpMode,
) : ActionState(logger) {
    private var dumpResult: ProcessResult? = null
    private var dumpedObjects: Int? = null

    /**
     * Регистрирует результат выгрузки
     *
     * @param result Результат выполнения команды
     * @param description Описание операции
     * @param objectsCount Количество выгруженных объектов (если известно)
     */
    fun registerDumpResult(
        result: ProcessResult,
        description: String,
        objectsCount: Int? = null,
    ) {
        dumpResult = result
        dumpedObjects = objectsCount
        if (!result.success) {
            success = false
        }
        addStep(result.toActionStepResult(description))
    }

    /**
     * Преобразует состояние в результат выгрузки
     *
     * @param message Базовое сообщение о результате
     * @return Результат выгрузки конфигурации
     */
    fun toResult(message: String): DumpResult {
        val errors = mutableListOf<String>()
        dumpResult?.error?.let(errors::add)

        return DumpResult(
            message = message + if (success) " успешно" else " неудачно",
            success = success,
            mode = mode,
            errors = errors,
            steps = steps.toList(),
            dumpedObjects = dumpedObjects,
        )
    }
}
