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
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.DesignerDsl
import io.github.alkoleft.mcp.infrastructure.utility.ifNoBlank
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

/**
 * Реализация BuildAction для сборки через конфигуратор 1С
 */
@Component
@ConditionalOnProperty(name = ["app.tools.builder"], havingValue = "DESIGNER")
class DesignerBuildAction(
    dsl: PlatformDsl,
) : AbstractBuildAction(dsl) {
    private lateinit var actionDsl: DesignerDsl

    override fun initDsl(properties: ApplicationProperties) {
        actionDsl =
            dsl.configurator {
                // Подключаемся к информационной базе
                connect(properties.connection.connectionString)
                properties.connection.user?.ifNoBlank { user(it) }
                properties.connection.password?.ifNoBlank { password(it) }

                // Отключаем диалоги и сообщения для автоматической работы
                disableStartupDialogs()
                disableStartupMessages()
            }
    }

    override fun loadConfiguration(
        name: String,
        path: Path,
    ) = actionDsl.loadConfigFromFiles {
        fromPath(path)
    }

    override fun loadExtension(
        name: String,
        path: Path,
    ) = actionDsl.loadConfigFromFiles {
        fromPath(path)
        extension(name)
    }

    override fun updateDb() = actionDsl.updateDBCfg {}
}
