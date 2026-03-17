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

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.BuildProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.EdtCliProperties
import io.github.alkoleft.mcp.configuration.properties.EnterpriseProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import io.github.alkoleft.mcp.infrastructure.storage.StorageData
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.context.annotation.ImportRuntimeHints

@ImportRuntimeHints(AppRuntimeHints::class)
@org.springframework.context.annotation.Configuration
class NativeHintsConfig

class AppRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        // Configuration properties
        listOf(
            ApplicationProperties::class.java,
            ConnectionProperties::class.java,
            SourceSet::class.java,
            SourceSetItem::class.java,
            ToolsProperties::class.java,
            EnterpriseProperties::class.java,
            EdtCliProperties::class.java,
            BuildProperties::class.java,
            StorageData::class.java,
            KotlinModule::class.java,
        ).forEach { clazz ->
            hints.reflection().registerType(clazz, *MemberCategory.entries.toTypedArray())
        }

        // Resource patterns
        hints.resources().registerPattern("application*.yml")
        hints.resources().registerPattern("logback*.xml")
        hints.resources().registerPattern("git.properties")
    }
}
