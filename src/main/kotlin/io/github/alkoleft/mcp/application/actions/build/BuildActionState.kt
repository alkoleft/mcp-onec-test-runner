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

package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.common.ActionState
import io.github.alkoleft.mcp.application.actions.common.ActionStepResult
import io.github.alkoleft.mcp.application.actions.common.BuildResult
import io.github.alkoleft.mcp.application.actions.common.fullError
import io.github.alkoleft.mcp.application.actions.common.toActionStepResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration

private val logger = KotlinLogging.logger { }

/**
 * Состояние выполнения сборки проекта
 *
 * Отслеживает результаты выполнения команд для каждого элемента source set
 * и результаты обновления базы данных.
 */
class BuildActionState : ActionState(logger) {
    val sourceSet = mutableMapOf<String, ProcessResult>()
    var updateResult: ProcessResult? = null

    /**
     * Добавляет результат выполнения команды для элемента source set
     *
     * @param name Имя элемента source set
     * @param result Результат выполнения команды
     * @param description Описание операции
     */
    fun addResult(
        name: String,
        result: ProcessResult,
        description: String,
    ) {
        sourceSet[name] = result
        if (!result.success) {
            success = false
        }
        addStep(result.toActionStepResult(description))
    }

    /**
     * Регистрирует результат обновления базы данных
     *
     * @param result Результат выполнения команды обновления
     */
    fun registerUpdateResult(result: ProcessResult) {
        updateResult = result
        if (!result.success) {
            success = false
        }
        addStep(result.toActionStepResult("Обновление конфигурации"))
    }

    fun registerSkippedConfiguration() {
        addStep(
            ActionStepResult(
                message = "Загрузка основной конфигурации: пропущена по параметру skipMainConfigurationUpdate",
                success = true,
                duration = Duration.ZERO,
            ),
        )
    }

    /**
     * Преобразует состояние в результат сборки
     *
     * @param message Базовое сообщение о результате
     * @return Результат сборки проекта
     */
    fun toResult(message: String): BuildResult {
        val errors = mutableListOf<String>()
        errors.addAll(sourceSet.values.mapNotNull { it.fullError() })
        updateResult?.fullError()?.let(errors::add)

        return BuildResult(
            message = message + if (success) " успешно" else " неудачно",
            success = success,
            errors = errors,
            sourceSet = sourceSet,
            steps = steps.toList(),
        )
    }
}
