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

import io.github.alkoleft.mcp.application.actions.change.SourceSetChanges
import io.github.alkoleft.mcp.application.actions.common.BuildAction
import io.github.alkoleft.mcp.application.actions.common.BuildResult
import io.github.alkoleft.mcp.application.actions.exceptions.BuildException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.alkoleft.mcp.infrastructure.utility.PartialLoadListGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger { }

/**
 * Абстрактный базовый класс для BuildAction, предоставляющий общую функциональность
 * для измерения времени выполнения, обработки ошибок, логирования и частичной загрузки.
 */
abstract class AbstractBuildAction(
    protected val dsl: PlatformDsl,
    protected val partialLoadListGenerator: PartialLoadListGenerator,
) : BuildAction {
    /**
     * Выполняет полную сборку проекта с измерением времени
     */
    override fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult = measureExecutionTime { executeBuildDsl(properties, sourceSet) }

    /**
     * Выполняет сборку проекта с поддержкой частичной загрузки.
     *
     * Автоматически определяет режим загрузки на основе:
     * - Количества измененных файлов (порог из properties.build.partialLoadThreshold)
     * - Наличия изменений в Configuration.xml
     */
    override fun runPartial(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        sourceSetChanges: Map<String, SourceSetChanges>,
    ): BuildResult =
        measureExecutionTime {
            executeBuildDslPartial(properties, sourceSet, sourceSetChanges)
        }

    /**
     * Измеряет время выполнения операции с обработкой ошибок
     */
    private fun measureExecutionTime(block: () -> BuildResult): BuildResult {
        val startTime = TimeSource.Monotonic.markNow()
        return try {
            val result = block()
            val duration = startTime.elapsedNow()
            logger.debug { "Сборка проекта завершена за $duration" }
            result.copy(duration = duration)
        } catch (e: Exception) {
            val duration = startTime.elapsedNow()
            logger.error(e) { "Сборка проекта завершилась с ошибкой после $duration" }
            throw BuildException("Сборка проекта завершилась с ошибкой: ${e.message}", e)
        }
    }

    /**
     * Метод для выполнения DSL сборки (полная загрузка)
     */
    fun executeBuildDsl(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult {
        logger.debug { "Сборка проекта (полная загрузка)" }

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

    /**
     * Метод для выполнения DSL сборки с поддержкой частичной загрузки
     */
    fun executeBuildDslPartial(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        sourceSetChanges: Map<String, SourceSetChanges>,
    ): BuildResult {
        logger.debug { "Сборка проекта с поддержкой частичной загрузки" }

        initDsl(properties)
        val state = BuildActionState()
        val threshold = properties.build.partialLoadThreshold

        // Загружаем основную конфигурацию
        sourceSet.configuration?.also { configuration ->
            val configPath = sourceSet.basePath.resolve(configuration.path)
            val changes = sourceSetChanges[configuration.name]

            val result =
                if (changes != null && shouldUsePartialLoad(changes, threshold, configPath)) {
                    logger.info { "Частичная загрузка конфигурации: ${changes.changedFiles.size} файлов" }
                    loadConfigurationPartial(configuration.name, configPath, changes.changedFiles)
                } else {
                    logger.info { "Полная загрузка конфигурации" }
                    loadConfiguration(configuration.name, configPath)
                }
            state.addResult(configuration.name, result, "Загрузка конфигурации")
        }

        if (!state.success) {
            return state.toResult("При загрузке исходников возникли ошибки")
        }

        // Загружаем расширения
        val extensions = sourceSet.extensions
        logger.info { "Загружаю ${extensions.size} расширений: ${extensions.joinToString(", ") { it.name }}" }
        extensions.forEach { extension ->
            val extensionPath = sourceSet.basePath.resolve(extension.path)
            val changes = sourceSetChanges[extension.name]

            val result =
                if (changes != null && shouldUsePartialLoad(changes, threshold, extensionPath)) {
                    logger.info { "Частичная загрузка расширения ${extension.name}: ${changes.changedFiles.size} файлов" }
                    loadExtensionPartial(extension.name, extensionPath, changes.changedFiles)
                } else {
                    logger.info { "Полная загрузка расширения ${extension.name}" }
                    loadExtension(extension.name, extensionPath)
                }
            state.addResult(extension.name, result, "Загрузка расширения ${extension.name}")

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

    /**
     * Определяет, следует ли использовать частичную загрузку для source set.
     */
    private fun shouldUsePartialLoad(
        changes: SourceSetChanges,
        threshold: Int,
        sourceSetPath: Path,
    ): Boolean =
        partialLoadListGenerator.shouldUsePartialLoad(
            changedFiles = changes.changedFiles,
            threshold = threshold,
            sourceSetPath = sourceSetPath,
        )

    /**
     * Инициализирует DSL для выполнения команд
     */
    protected abstract fun initDsl(properties: ApplicationProperties): Unit

    /**
     * Загружает конфигурацию полностью
     */
    protected abstract fun loadConfiguration(
        name: String,
        path: Path,
    ): ProcessResult

    /**
     * Загружает конфигурацию частично (только измененные файлы)
     */
    protected abstract fun loadConfigurationPartial(
        name: String,
        path: Path,
        changedFiles: Set<Path>,
    ): ProcessResult

    /**
     * Загружает расширение полностью
     */
    protected abstract fun loadExtension(
        name: String,
        path: Path,
    ): ProcessResult

    /**
     * Загружает расширение частично (только измененные файлы)
     */
    protected abstract fun loadExtensionPartial(
        name: String,
        path: Path,
        changedFiles: Set<Path>,
    ): ProcessResult

    /**
     * Обновляет конфигурацию базы данных
     */
    protected abstract fun updateDb(): ProcessResult?
}
