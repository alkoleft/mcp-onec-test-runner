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

package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.application.actions.change.FileSystemChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.change.SourceSetChangeAnalyzer
import io.github.alkoleft.mcp.application.actions.common.BuildAction
import io.github.alkoleft.mcp.application.actions.common.ChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.common.ConvertAction
import io.github.alkoleft.mcp.application.actions.common.LaunchAction
import io.github.alkoleft.mcp.application.actions.common.RunTestAction
import io.github.alkoleft.mcp.application.actions.convert.EdtInteractiveConvertAction
import io.github.alkoleft.mcp.application.actions.test.yaxunit.YaXUnitTestAction
import io.github.alkoleft.mcp.configuration.properties.BuilderType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.storage.FileBuildStateManager
import io.github.alkoleft.mcp.infrastructure.yaxunit.ReportParser
import io.github.alkoleft.mcp.infrastructure.yaxunit.YaXUnitRunner
import org.springframework.stereotype.Component

/**
 * Фабрика для создания Actions
 */
@Component
class ActionFactory(
    private val platformDsl: PlatformDsl,
    private val yaxUnitRunner: YaXUnitRunner,
    private val reportParser: ReportParser,
    private val buildStateManager: FileBuildStateManager,
    private val sourceSetAnalyzer: SourceSetChangeAnalyzer,
    private val buildAction: BuildAction,
    private val launchAction: LaunchAction,
) {
    fun createBuildAction(type: BuilderType) = buildAction

    fun convertAction(): ConvertAction = EdtInteractiveConvertAction(platformDsl)

    fun createChangeAnalysisAction(): ChangeAnalysisAction = FileSystemChangeAnalysisAction(buildStateManager, sourceSetAnalyzer)

    fun createRunTestAction(): RunTestAction = YaXUnitTestAction(reportParser, yaxUnitRunner)

    fun createLaunchAction() = launchAction
}
