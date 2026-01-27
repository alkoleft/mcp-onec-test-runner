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
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Path

/**
 * Реализация DumpAction для выгрузки через ibcmd
 */
@Component
@ConditionalOnProperty(name = ["app.tools.builder"], havingValue = "IBCMD")
class IbcmdDumpAction(
    dsl: PlatformDsl,
) : AbstractDumpAction(dsl) {
    private lateinit var actionDsl: IbcmdDsl

    override fun initDsl(properties: ApplicationProperties) {
        actionDsl = dsl.ibcmd { }
    }

    override fun dumpConfiguration(targetPath: Path): ProcessResult {
        lateinit var result: ProcessResult
        actionDsl.config {
            result = export(targetPath) {
                force = true
            }
        }
        return result
    }

    override fun dumpConfigurationIncremental(targetPath: Path): ProcessResult {
        // Для инкрементальной выгрузки используем --sync
        lateinit var result: ProcessResult
        actionDsl.config {
            result = export(targetPath) {
                sync = true
            }
        }
        return result
    }

    override fun dumpConfigurationPartial(
        targetPath: Path,
        objects: List<String>,
    ): ProcessResult {
        // ibcmd не поддерживает частичную выгрузку по списку объектов напрямую
        // Используем подкоманду objects если нужно
        lateinit var result: ProcessResult
        actionDsl.config {
            result = export(targetPath) {
                exportSubCommand = "objects"
                // Объекты передаются через файл или строку - зависит от реализации ibcmd
                // Пока используем стандартный экспорт с синхронизацией
                sync = true
            }
        }
        return result
    }

    override fun dumpExtension(
        name: String,
        targetPath: Path,
    ): ProcessResult {
        lateinit var result: ProcessResult
        actionDsl.config {
            result = export(targetPath) {
                extension = name
                force = true
            }
        }
        return result
    }

    override fun dumpExtensionIncremental(
        name: String,
        targetPath: Path,
    ): ProcessResult {
        lateinit var result: ProcessResult
        actionDsl.config {
            result = export(targetPath) {
                extension = name
                sync = true
            }
        }
        return result
    }

    override fun dumpExtensionPartial(
        name: String,
        targetPath: Path,
        objects: List<String>,
    ): ProcessResult {
        // ibcmd не поддерживает частичную выгрузку расширений по списку объектов
        // Используем инкрементальную выгрузку
        lateinit var result: ProcessResult
        actionDsl.config {
            result = export(targetPath) {
                extension = name
                sync = true
            }
        }
        return result
    }

    override fun dumpAllExtensions(targetPath: Path): ProcessResult {
        lateinit var result: ProcessResult
        actionDsl.config {
            result = export(targetPath) {
                exportSubCommand = "all-extensions"
                force = true
            }
        }
        return result
    }
}
