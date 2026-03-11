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
import io.github.alkoleft.mcp.application.actions.convert.EdtInteractiveConvertAction
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunAllTestsRequest
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunListTestsRequest
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunModuleTestsRequest
import io.github.alkoleft.mcp.application.actions.test.yaxunit.YaXUnitTestAction
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.BuilderType
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.utility.PartialLoadListGenerator
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
    companion object {
        const val SOURCE_PATH = "/home/alko/develop/temp/yaxunit-designer"
        const val IB_PATH = "/home/alko/develop/onec_file_db/YaxUnit-dev"
        const val VERSION = "8.5.1.1150"

        const val EDT_SOURCE_PATH = "/home/alko/develop/open-source/ai/mcp/onec-client-mcp-devkit"
        const val EDT_WORKING_DIR = "/home/alko/develop/EDT_WS/client-mcp-ai"

        fun edtApplicationProperties(): ApplicationProperties =
            ApplicationProperties(
                basePath = Path(EDT_SOURCE_PATH),
                sourceSet =
                    SourceSet(
                        items =
                            listOf(
                                SourceSetItem(
                                    path = "fixtures/configuration",
                                    name = "configuration",
                                    type = SourceSetType.CONFIGURATION,
                                    purpose = setOf(SourceSetPurpose.MAIN),
                                ),
                                SourceSetItem(
                                    path = "exts/client-mcp",
                                    name = "ClientMcp",
                                    type = SourceSetType.EXTENSION,
                                    purpose = setOf(SourceSetPurpose.MAIN),
                                ),
                                SourceSetItem(
                                    path = "tests/src",
                                    name = "tests",
                                    type = SourceSetType.EXTENSION,
                                    purpose = setOf(SourceSetPurpose.TESTS, SourceSetPurpose.YAXUNIT),
                                ),
                            ),
                    ),
                connection =
                    ConnectionProperties(
                        connectionString = "File='$IB_PATH';",
                    ),
                platformVersion = VERSION,
                tools =
                    ToolsProperties(
                        builder = BuilderType.DESIGNER,
                    ),
            )
    }

    @Test
    fun designerRealExecute() {
        platformDsl.designer {
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
        val action = DesignerBuildAction(platformDsl, PartialLoadListGenerator())
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

    // Тест конвертации EDT
    @Test
    fun edtConvert() {
        val convertAction = EdtInteractiveConvertAction(platformDsl)
        val properties = edtApplicationProperties()

        val edtSourceSet =
            SourceSet(
                items =
                    listOf(
                        SourceSetItem(
                            path = "fixtures/configuration",
                            name = "configuration",
                            type = SourceSetType.CONFIGURATION,
                            purpose = setOf(SourceSetPurpose.MAIN),
                        ),
                    ),
            )
        val designerSourceSet = properties.sourceSet

        val result = convertAction.run(properties, edtSourceSet, designerSourceSet)
        println("Результат конвертации: $result")
    }
}
