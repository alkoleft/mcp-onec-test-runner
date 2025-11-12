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

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Path

/**
 * Реализация BuildAction для сборки через ibcmd
 */
@Component
@ConditionalOnProperty(name = ["app.tools.builder"], havingValue = "IBCMD")
class IbcmdBuildAction(
    dsl: PlatformDsl,
) : AbstractBuildAction(dsl) {
    private lateinit var actionDsl: IbcmdDsl

    override fun initDsl(properties: ApplicationProperties) {
        actionDsl =
            dsl.ibcmd {
            }
    }

    override fun loadConfiguration(
        name: String,
        path: Path,
    ): ProcessResult {
        lateinit var result: ProcessResult
        actionDsl.config { result = import(path) }
        return result
    }

    override fun loadExtension(
        name: String,
        path: Path,
    ): ProcessResult {
        lateinit var result: ProcessResult
        actionDsl.config {
            result =
                import(path) {
                    extension = name
                }
        }
        return result
    }

    override fun updateDb(): ProcessResult {
        lateinit var result: ProcessResult
        actionDsl.config { result = apply() }
        return result
    }
}
