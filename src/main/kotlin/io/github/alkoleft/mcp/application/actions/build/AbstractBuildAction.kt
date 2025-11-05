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

import io.github.alkoleft.mcp.application.actions.BuildAction
import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.application.actions.common.ActionState
import io.github.alkoleft.mcp.application.actions.common.toActionStepResult
import io.github.alkoleft.mcp.application.actions.exceptions.BuildException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger { }

/**
 * Абстрактный базовый класс для BuildAction, предоставляющий общую функциональность
 * для измерения времени выполнения, обработки ошибок и логирования
 */
abstract class AbstractBuildAction(
    protected val dsl: PlatformDsl,
) : BuildAction {
    /**
     * Выполняет полную сборку проекта с измерением времени
     */
    override fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult = measureExecutionTime { executeBuildDsl(properties, sourceSet) }

    /**
     * Измеряет время выполнения операции с обработкой ошибок
     */
    private fun measureExecutionTime(block: () -> BuildResult): BuildResult {
        val startTime = TimeSource.Monotonic.markNow()
        return try {
            val result = block()
            val duration = startTime.elapsedNow()
            logger.info { "Сборка проекта завершена за $duration" }
            result.copy(duration = duration)
        } catch (e: Exception) {
            val duration = startTime.elapsedNow()
            logger.error(e) { "Сборка проекта завершилась с ошибкой после $duration" }
            throw BuildException("Сборка проекта завершилась с ошибкой: ${e.message}", e)
        }
    }

    /**
     * Метод для выполнения DSL сборки
     */
    fun executeBuildDsl(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult {
        logger.debug { "Сборка проекта" }

        initDsl(properties)
        val state = BuildActionState()

        // Загружаем основную конфигурацию
        sourceSet.configuration?.also { configuration ->
            logger.info { "Загружаю основную конфигурацию" }
            val result = loadConfiguration(configuration.name, sourceSet.basePath.resolve(configuration.path))
            state.addResult(configuration.name, result, "Загрузка конфигурации")
        }

        if (!state.success) {
            return state.toResult("При загрузке исходников возникли ошибки")
        }

        // Загружаем расширения
        val extensions = sourceSet.extensions
        logger.info { "Загружаю ${extensions.size} расширений: ${extensions.joinToString(", ") { it.name }}" }
        extensions.forEach {
            val result = loadExtension(it.name, sourceSet.basePath.resolve(it.path))
            state.addResult(it.name, result, "Загрузка расширения ${it.name}")

            if (!result.success) {
                return@forEach
            }
        }

        if (!state.success) {
            return state.toResult("При загрузке исходников возникли ошибки")
        }

        updateDb()?.also(state::registerUpdateResult)

        return state.toResult("Сборка завершена").also { if (it.success) logger.info { it.message } }
    }

    protected abstract fun initDsl(properties: ApplicationProperties): Unit

    protected abstract fun loadConfiguration(
        name: String,
        path: Path,
    ): ProcessResult

    protected abstract fun loadExtension(
        name: String,
        path: Path,
    ): ProcessResult

    protected abstract fun updateDb(): ProcessResult?

    class BuildActionState : ActionState(logger) {
        val sourceSet = mutableMapOf<String, ProcessResult>()
        var updateResult: ProcessResult? = null

        fun addResult(
            name: String,
            result: ProcessResult,
            description: String,
        ) {
            sourceSet.put(name, result)
            if (!result.success) {
                success = false
            }
            addStep(result.toActionStepResult(description))
        }

        fun registerUpdateResult(result: ProcessResult) {
            updateResult = result
            if (!result.success) {
                success = false
            }
            addStep(result.toActionStepResult("Обновление конфигурации"))
        }

        fun toResult(message: String): BuildResult {
            val errors = mutableListOf<String>()
            errors.addAll(sourceSet.values.mapNotNull { it.error })
            updateResult?.error?.let(errors::add)

            return BuildResult(
                message = message + if (success) " успешно" else " неудачно",
                success = success,
                errors = errors,
                sourceSet = sourceSet,
                steps = steps.toList(),
            )
        }
    }
}
