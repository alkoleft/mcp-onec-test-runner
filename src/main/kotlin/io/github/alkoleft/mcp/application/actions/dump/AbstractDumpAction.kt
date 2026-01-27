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

import io.github.alkoleft.mcp.application.actions.common.DumpAction
import io.github.alkoleft.mcp.application.actions.common.DumpMode
import io.github.alkoleft.mcp.application.actions.common.DumpResult
import io.github.alkoleft.mcp.application.actions.exceptions.DumpException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger { }

/**
 * Абстрактный базовый класс для DumpAction, предоставляющий общую функциональность
 * для измерения времени выполнения, обработки ошибок и логирования.
 */
abstract class AbstractDumpAction(
    protected val dsl: PlatformDsl,
) : DumpAction {
    /**
     * Выполняет полную выгрузку конфигурации с измерением времени
     */
    override fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        extension: String?,
        allExtensions: Boolean,
    ): DumpResult = measureExecutionTime(DumpMode.FULL) {
        executeDumpFull(properties, sourceSet, extension, allExtensions)
    }

    /**
     * Выполняет инкрементальную выгрузку с измерением времени
     */
    override fun runIncremental(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        extension: String?,
    ): DumpResult = measureExecutionTime(DumpMode.INCREMENTAL) {
        executeDumpIncremental(properties, sourceSet, extension)
    }

    /**
     * Выполняет частичную выгрузку конкретных объектов с измерением времени
     */
    override fun runPartial(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        objects: List<String>,
        extension: String?,
    ): DumpResult = measureExecutionTime(DumpMode.PARTIAL) {
        executeDumpPartial(properties, sourceSet, objects, extension)
    }

    /**
     * Измеряет время выполнения операции с обработкой ошибок
     */
    private fun measureExecutionTime(
        mode: DumpMode,
        block: () -> DumpResult,
    ): DumpResult {
        val startTime = TimeSource.Monotonic.markNow()
        return try {
            val result = block()
            val duration = startTime.elapsedNow()
            logger.info { "Выгрузка конфигурации (режим: $mode) завершена за $duration" }
            result.copy(duration = duration)
        } catch (e: Exception) {
            val duration = startTime.elapsedNow()
            logger.error(e) { "Выгрузка конфигурации завершилась с ошибкой после $duration" }
            throw DumpException("Выгрузка конфигурации завершилась с ошибкой: ${e.message}", e)
        }
    }

    /**
     * Выполняет полную выгрузку конфигурации
     */
    private fun executeDumpFull(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        extension: String?,
        allExtensions: Boolean,
    ): DumpResult {
        logger.debug { "Выгрузка конфигурации (полная)" }

        initDsl(properties)
        val state = DumpActionState(DumpMode.FULL)

        val targetPath = resolveTargetPath(sourceSet, extension)

        val result = when {
            allExtensions -> {
                logger.info { "Выгружаю все расширения в $targetPath" }
                dumpAllExtensions(targetPath)
            }
            extension != null -> {
                logger.info { "Выгружаю расширение '$extension' в $targetPath" }
                dumpExtension(extension, targetPath)
            }
            else -> {
                logger.info { "Выгружаю основную конфигурацию в $targetPath" }
                dumpConfiguration(targetPath)
            }
        }

        state.registerDumpResult(result, "Выгрузка конфигурации")

        return state.toResult("Выгрузка").also { if (it.success) logger.info { it.message } }
    }

    /**
     * Выполняет инкрементальную выгрузку
     */
    private fun executeDumpIncremental(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        extension: String?,
    ): DumpResult {
        logger.debug { "Выгрузка конфигурации (инкрементальная)" }

        initDsl(properties)
        val state = DumpActionState(DumpMode.INCREMENTAL)

        val targetPath = resolveTargetPath(sourceSet, extension)

        val result = if (extension != null) {
            logger.info { "Инкрементальная выгрузка расширения '$extension' в $targetPath" }
            dumpExtensionIncremental(extension, targetPath)
        } else {
            logger.info { "Инкрементальная выгрузка основной конфигурации в $targetPath" }
            dumpConfigurationIncremental(targetPath)
        }

        state.registerDumpResult(result, "Инкрементальная выгрузка")

        return state.toResult("Инкрементальная выгрузка").also { if (it.success) logger.info { it.message } }
    }

    /**
     * Выполняет частичную выгрузку конкретных объектов
     */
    private fun executeDumpPartial(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        objects: List<String>,
        extension: String?,
    ): DumpResult {
        logger.debug { "Выгрузка конфигурации (частичная): ${objects.size} объектов" }

        if (objects.isEmpty()) {
            return DumpResult(
                message = "Список объектов для выгрузки пуст",
                success = false,
                mode = DumpMode.PARTIAL,
                errors = listOf("Необходимо указать хотя бы один объект для выгрузки"),
            )
        }

        initDsl(properties)
        val state = DumpActionState(DumpMode.PARTIAL)

        val targetPath = resolveTargetPath(sourceSet, extension)

        val result = if (extension != null) {
            logger.info { "Частичная выгрузка расширения '$extension': ${objects.joinToString(", ")}" }
            dumpExtensionPartial(extension, targetPath, objects)
        } else {
            logger.info { "Частичная выгрузка конфигурации: ${objects.joinToString(", ")}" }
            dumpConfigurationPartial(targetPath, objects)
        }

        state.registerDumpResult(result, "Частичная выгрузка", objects.size)

        return state.toResult("Частичная выгрузка").also { if (it.success) logger.info { it.message } }
    }

    /**
     * Определяет целевой путь для выгрузки
     */
    private fun resolveTargetPath(
        sourceSet: SourceSet,
        extension: String?,
    ): Path {
        return if (extension != null) {
            val extensionConfig = sourceSet.extensions.find { it.name == extension }
            if (extensionConfig != null) {
                sourceSet.basePath.resolve(extensionConfig.path)
            } else {
                sourceSet.basePath.resolve("extensions/$extension")
            }
        } else {
            sourceSet.configuration?.let { sourceSet.basePath.resolve(it.path) }
                ?: sourceSet.basePath
        }
    }

    /**
     * Инициализирует DSL для выполнения команд
     */
    protected abstract fun initDsl(properties: ApplicationProperties): Unit

    /**
     * Выгружает основную конфигурацию полностью
     */
    protected abstract fun dumpConfiguration(targetPath: Path): ProcessResult

    /**
     * Выгружает основную конфигурацию инкрементально
     */
    protected abstract fun dumpConfigurationIncremental(targetPath: Path): ProcessResult

    /**
     * Выгружает основную конфигурацию частично (конкретные объекты)
     */
    protected abstract fun dumpConfigurationPartial(
        targetPath: Path,
        objects: List<String>,
    ): ProcessResult

    /**
     * Выгружает расширение полностью
     */
    protected abstract fun dumpExtension(
        name: String,
        targetPath: Path,
    ): ProcessResult

    /**
     * Выгружает расширение инкрементально
     */
    protected abstract fun dumpExtensionIncremental(
        name: String,
        targetPath: Path,
    ): ProcessResult

    /**
     * Выгружает расширение частично (конкретные объекты)
     */
    protected abstract fun dumpExtensionPartial(
        name: String,
        targetPath: Path,
        objects: List<String>,
    ): ProcessResult

    /**
     * Выгружает все расширения
     */
    protected abstract fun dumpAllExtensions(targetPath: Path): ProcessResult
}
