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

package io.github.alkoleft.mcp

import io.github.alkoleft.mcp.application.services.LauncherService
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Ignore

@Ignore
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@ActiveProfiles("yaxunit-edt")
class LaunchTest(
    @Autowired private val launcher: LauncherService,
    @Autowired private val properties: ApplicationProperties,
) {
    @Test
    fun launchYaxUnit() {
        launcher.run(RunModuleTestsRequest("ОМ_ЮТКоллекции", properties))
    }
}
