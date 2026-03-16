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

package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ProjectFormat
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.infrastructure.storage.MapDbHashStorageFactory
import io.github.alkoleft.mcp.infrastructure.storage.SourceSetContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

/**
 * Service for managing source sets and their hash storage.
 *
 * Provides access to source set contexts for change detection.
 * For EDT format, provides both original EDT source set and converted Designer source set.
 * For DESIGNER format, provides only the designer source set.
 */
@Service
class SourceSetsService(
    private val properties: ApplicationProperties,
    private val hashStorageFactory: MapDbHashStorageFactory,
) {
    private var edtSourceSetContext: SourceSetContext? = null
    private var designerSourceSetContext: SourceSetContext? = null

    /**
     * For DESIGNER format: returns the designer source set.
     * For EDT format: returns the converted Designer source set.
     */
    fun getSourceSet(): SourceSetContext = getDesignerSourceSet()!!

    /**
     * Returns the EDT source set context if format is EDT, null otherwise.
     */
    fun getEdtSourceSet(): SourceSetContext? {
        if (properties.format != ProjectFormat.EDT) {
            return null
        }

        return edtSourceSetContext ?: synchronized(this) {
            edtSourceSetContext ?: run {
                val sourceSet =
                    SourceSet(
                        properties.basePath,
                        properties.sourceSet,
                    )
                val hashStorage = hashStorageFactory.getStorage("edt")
                SourceSetContext(
                    sourceSet = sourceSet,
                    hashStorage = hashStorage,
                    format = ProjectFormat.EDT,
                    isDesignerSource = false,
                ).also { edtSourceSetContext = it }
            }
        }
    }

    /**
     * Returns the Designer source set context.
     * For EDT format, this is the converted source set in workPath.
     * For DESIGNER format, this is the original source set.
     */
    fun getDesignerSourceSet(): SourceSetContext? =
        designerSourceSetContext ?: synchronized(this) {
            designerSourceSetContext ?: run {
                val sourceSet = createDesignerSourceSet()
                val hashStorage = hashStorageFactory.getStorage("designer")
                SourceSetContext(
                    sourceSet = sourceSet,
                    hashStorage = hashStorage,
                    format = properties.format,
                    isDesignerSource = true,
                ).also { designerSourceSetContext = it }
            }
        }

    /**
     * Returns all source sets for the current format.
     * For EDT: both EDT and Designer source sets.
     * For DESIGNER: only the Designer source set.
     */
    fun getAllSourceSets(): List<SourceSetContext> =
        when (properties.format) {
            ProjectFormat.DESIGNER -> listOfNotNull(getDesignerSourceSet())
            ProjectFormat.EDT -> listOfNotNull(getEdtSourceSet(), getDesignerSourceSet())
        }

    /**
     * Creates the appropriate Designer source set based on format.
     */
    private fun createDesignerSourceSet(): SourceSet =
        if (properties.format == ProjectFormat.EDT) {
            // For EDT, converted sources go to workPath with item name as path
            SourceSet(
                properties.workPath,
                properties.sourceSet.map { it.copy(path = it.name) },
            )
        } else {
            // For DESIGNER, use base path directly
            SourceSet(
                properties.basePath,
                properties.sourceSet,
            )
        }
}
