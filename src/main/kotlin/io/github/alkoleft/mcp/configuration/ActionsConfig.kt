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

package io.github.alkoleft.mcp.configuration

import io.github.alkoleft.mcp.application.actions.common.BuildAction
import io.github.alkoleft.mcp.application.actions.build.DesignerBuildAction
import io.github.alkoleft.mcp.application.actions.build.IbcmdBuildAction
import io.github.alkoleft.mcp.application.actions.common.DumpAction
import io.github.alkoleft.mcp.application.actions.dump.DesignerDumpAction
import io.github.alkoleft.mcp.application.actions.dump.IbcmdDumpAction
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.BuilderType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.utility.PartialLoadListGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ActionsConfig {

    @Bean
    fun dumpAction(
        properties: ApplicationProperties,
        dsl: PlatformDsl,
    ): DumpAction = when (properties.tools.builder) {
        BuilderType.IBCMD -> IbcmdDumpAction(dsl)
        else -> DesignerDumpAction(dsl)
    }

    @Bean
    fun buildAction(
        properties: ApplicationProperties,
        dsl: PlatformDsl,
        partialLoadListGenerator: PartialLoadListGenerator,
    ): BuildAction = when (properties.tools.builder) {
        BuilderType.IBCMD -> IbcmdBuildAction(dsl, partialLoadListGenerator)
        else -> DesignerBuildAction(dsl, partialLoadListGenerator)
    }
}
