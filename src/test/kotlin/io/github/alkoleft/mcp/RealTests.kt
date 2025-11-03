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
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityLocator
import io.github.alkoleft.mcp.infrastructure.yaxunit.ReportParser
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
    @Autowired private val utilLocator: UtilityLocator,
    @Autowired private val reportParser: ReportParser,
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
        val properties = testApplicationProperties()
        val action = YaXUnitTestAction(platformDsl, utilLocator, reportParser)

        println("=== Запуск всех тестов YaXUnit ===")
        action.run(RunAllTestsRequest(properties))
    }

    @Test
    fun yaxunitRealTestRunModule() {
        val properties = testApplicationProperties()
        val action = YaXUnitTestAction(platformDsl, utilLocator, reportParser)
        val moduleName = "ОМ_ЮТКоллекции" // Модуль с тестами

        println("=== Запуск тестов модуля '$moduleName' ===")
        action.run(RunModuleTestsRequest(moduleName, properties))
    }

    @Test
    fun yaxunitRealTestRunSpecificTests() {
        val properties = testApplicationProperties()
        val action = YaXUnitTestAction(platformDsl, utilLocator, reportParser)
        val testNames = listOf("TestExample", "TestCalculator") // Примеры имен тестов

        println("=== Запуск конкретных тестов: ${testNames.joinToString(", ")} ===")
        action.run(RunListTestsRequest(testNames, properties))
    }

    @Test
    fun yaxunitRealTestRunSingleTest() {
        val properties = testApplicationProperties()
        val action = YaXUnitTestAction(platformDsl, utilLocator, reportParser)
        val testName = "TestExample" // Пример имени теста

        println("=== Запуск одного теста: '$testName' ===")
        action.run(RunListTestsRequest(listOf(testName), properties))
    }

    /**
     * Создает кастомные свойства приложения для тестирования
     */
    private fun createCustomApplicationProperties(): ApplicationProperties =
        ApplicationProperties(
            basePath = Path("/custom/path/to/project"),
            sourceSet =
                SourceSet(
                    items =
                        listOf(
                            SourceSetItem(
                                path = "src/configuration",
                                name = "main-config",
                                type = SourceSetType.CONFIGURATION,
                                purpose = setOf(SourceSetPurpose.MAIN),
                            ),
                            SourceSetItem(
                                path = "src/extensions/custom-extension",
                                name = "custom-extension",
                                type = SourceSetType.EXTENSION,
                                purpose = setOf(SourceSetPurpose.YAXUNIT),
                            ),
                        ),
                ),
            connection =
                ConnectionProperties(
                    connectionString = "File=\"/custom/path/to/database\"",
                    user = "Admin",
                    password = "",
                ),
            platformVersion = VERSION,
            tools = ToolsProperties(),
        )
}
