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

import io.github.alkoleft.mcp.application.actions.build.DesignerBuildAction
import io.github.alkoleft.mcp.application.actions.test.YaXUnitTestAction
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.yaxunit.ReportParser
import io.github.alkoleft.mcp.infrastructure.yaxunit.YaXUnitRunner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.io.path.Path
import kotlin.test.Ignore

@Ignore
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@ActiveProfiles("test")
class RealTests(
    @Autowired private val platformDsl: PlatformDsl,
    @Autowired private val reportParser: ReportParser,
    @Autowired private val runner: YaXUnitRunner,
) {
    @Test
    fun designerRealExecute() {
        platformDsl.configurator {
            connectToFile(IB_PATH)
            disableStartupDialogs()
            disableStartupMessages()
            loadConfigFromFiles {
                fromPath(Path("$SOURCE_PATH/configuration"))
            }
            listOf("yaxunit", "tests").forEach {
                loadConfigFromFiles {
                    fromPath(Path("$SOURCE_PATH/$it"))
                    extension = it
                }
            }
        }
    }

    @Test
    fun ibcmdRealExecute() {
        platformDsl.ibcmd {
            dbPath = IB_PATH
            config {
                import(Path("$SOURCE_PATH/configuration"))
            }
            listOf("yaxunit", "tests").forEach {
                config {
                    import(Path("$SOURCE_PATH/$it")) {
                        extension = it
                    }
                }
            }
        }
    }

    // Тесты для DesignerBuildAction
    @Test
    fun designerBuildActionFullBuild() {
        val action = DesignerBuildAction(platformDsl)
        val properties = testApplicationProperties()

        val result = action.run(properties, properties.sourceSet)
        println("Результат полной сборки: $result")
    }

    // Реальные тесты для YaXUnit
    @Test
    fun yaxunitRealTestRunAll() {
        val action = YaXUnitTestAction(reportParser, runner)

        println("=== Запуск всех тестов YaXUnit ===")
        action.run(RunAllTestsRequest())
    }

    @Test
    fun yaxunitRealTestRunModule() {
        val action = YaXUnitTestAction(reportParser, runner)
        val moduleName = "ОМ_ЮТКоллекции" // Модуль с тестами

        println("=== Запуск тестов модуля '$moduleName' ===")
        action.run(RunModuleTestsRequest(moduleName))
    }

    @Test
    fun yaxunitRealTestRunSpecificTests() {
        val action = YaXUnitTestAction(reportParser, runner)
        val testNames = listOf("TestExample", "TestCalculator") // Примеры имен тестов

        println("=== Запуск конкретных тестов: ${testNames.joinToString(", ")} ===")
        action.run(RunListTestsRequest(testNames))
    }

    @Test
    fun yaxunitRealTestRunSingleTest() {
        val action = YaXUnitTestAction(reportParser, runner)
        val testName = "TestExample" // Пример имени теста

        println("=== Запуск одного теста: '$testName' ===")
        action.run(RunListTestsRequest(listOf(testName)))
    }
}
