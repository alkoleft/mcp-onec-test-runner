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

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.DesignerDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

/**
 * Реализация DumpAction для выгрузки через конфигуратор 1С
 */
@Component
@ConditionalOnProperty(name = ["app.tools.builder"], havingValue = "DESIGNER")
class DesignerDumpAction(
    dsl: PlatformDsl,
) : AbstractDumpAction(dsl) {
    private lateinit var actionDsl: DesignerDsl

    override fun initDsl(properties: ApplicationProperties) {
        actionDsl = dsl.designer {
            disableStartupDialogs()
            disableStartupMessages()
        }
    }

    override fun dumpConfiguration(targetPath: Path): ProcessResult {
        // Полная выгрузка: без -updateConfigDumpInfo выгружает всё,
        // но обновляем ConfigDumpInfo.xml для последующих инкрементальных выгрузок
        return actionDsl.dumpConfigToFiles {
            toPath(targetPath)
            updateConfigDumpInfo()
        }
    }

    override fun dumpConfigurationIncremental(targetPath: Path): ProcessResult {
        // Инкрементальная выгрузка: использует ConfigDumpInfo.xml
        // для определения изменённых объектов.
        // Сравнивает версии объектов в ИБ с сохранёнными в ConfigDumpInfo.xml
        // и выгружает только изменённые.
        // ВАЖНО: требует наличия ConfigDumpInfo.xml от предыдущей выгрузки
        return actionDsl.dumpConfigToFiles {
            toPath(targetPath)
            updateConfigDumpInfo()
        }
    }

    override fun dumpConfigurationPartial(
        targetPath: Path,
        objects: List<String>,
    ): ProcessResult {
        val listFile = createObjectsListFile(objects)
        return try {
            actionDsl.dumpConfigToFiles {
                toPath(targetPath)
                partial()
                listFile(listFile)
                updateConfigDumpInfo()
            }
        } finally {
            Files.deleteIfExists(listFile)
        }
    }

    override fun dumpExtension(
        name: String,
        targetPath: Path,
    ): ProcessResult {
        return actionDsl.dumpConfigToFiles {
            toPath(targetPath)
            extension(name)
            updateConfigDumpInfo()
        }
    }

    override fun dumpExtensionIncremental(
        name: String,
        targetPath: Path,
    ): ProcessResult {
        return actionDsl.dumpConfigToFiles {
            toPath(targetPath)
            extension(name)
            updateConfigDumpInfo()
        }
    }

    override fun dumpExtensionPartial(
        name: String,
        targetPath: Path,
        objects: List<String>,
    ): ProcessResult {
        val listFile = createObjectsListFile(objects)
        return try {
            actionDsl.dumpConfigToFiles {
                toPath(targetPath)
                extension(name)
                partial()
                listFile(listFile)
                updateConfigDumpInfo()
            }
        } finally {
            Files.deleteIfExists(listFile)
        }
    }

    override fun dumpAllExtensions(targetPath: Path): ProcessResult {
        return actionDsl.dumpConfigToFiles {
            toPath(targetPath)
            allExtensions()
            updateConfigDumpInfo()
        }
    }

    /**
     * Создает временный файл со списком объектов для частичной выгрузки
     */
    private fun createObjectsListFile(objects: List<String>): Path {
        val listFile = Files.createTempFile("dump-objects-", ".txt")
        Files.write(listFile, objects)
        return listFile
    }
}
