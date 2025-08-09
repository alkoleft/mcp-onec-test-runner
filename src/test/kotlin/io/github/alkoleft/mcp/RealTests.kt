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
import io.github.alkoleft.mcp.infrastructure.yaxunit.EnhancedReportParser
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.io.path.Path
import kotlin.test.Ignore

const val SOURCE_PATH = "~/Загрузки/sources"
const val IB_PATH = "/home/common/develop/file-data-base/YAxUnit"
const val VERSION = "8.3.22.1709"

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@ActiveProfiles("test")
@Ignore
class RealTests(
    @Autowired private val platformDsl: PlatformDsl,
    @Autowired private val utilLocator: UtilityLocator,
    @Autowired private val reportParser: EnhancedReportParser,
) {
    @Ignore
    @Test
    fun designerRealExecute() {
        platformDsl.configurator(VERSION) {
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

    @Ignore
    @Test
    fun ibcmdRealExecute() {
        val plan =
            platformDsl.ibcmd(VERSION) {
                dbPath(IB_PATH)

                config {
                    import("$SOURCE_PATH/configuration")
                    listOf("yaxunit", "tests").forEach {
                        import("$SOURCE_PATH/$it") {
                            extension = it
                        }
                    }
                }
            }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    // Тесты для DesignerBuildAction
    @Ignore
    @Test
    fun designerBuildActionFullBuild() {
        val action = DesignerBuildAction(platformDsl)
        val properties = createTestApplicationProperties()

        runBlocking {
            val result = action.build(properties, properties.sourceSet)
            println("Результат полной сборки: $result")
        }
    }

    @Ignore
    @Test
    fun designerBuildActionConfigurationBuild() {
        val action = DesignerBuildAction(platformDsl)
        val properties = createTestApplicationProperties()

        runBlocking {
            val result = action.buildConfiguration(properties)
            println("Результат сборки конфигурации: $result")
        }
    }

    @Ignore
    @Test
    fun designerBuildActionExtensionBuild() {
        val action = DesignerBuildAction(platformDsl)
        val properties = createTestApplicationProperties()
        val extensionName = "yaxunit"

        runBlocking {
            val result = action.buildExtension(extensionName, properties)
            println("Результат сборки расширения $extensionName: $result")
        }
    }

    @Ignore
    @Test
    fun designerBuildActionMultipleExtensionsBuild() {
        val action = DesignerBuildAction(platformDsl)
        val properties = createTestApplicationProperties()
        val extensions = listOf("yaxunit", "tests")

        runBlocking {
            extensions.forEach { extensionName ->
                val result = action.buildExtension(extensionName, properties)
                println("Результат сборки расширения $extensionName: $result")
            }
        }
    }

    @Ignore
    @Test
    fun designerBuildActionWithCustomPaths() {
        val action = DesignerBuildAction(platformDsl)
        val properties = createCustomApplicationProperties()

        runBlocking {
            val result = action.build(properties)
            println("Результат сборки с кастомными путями: $result")
        }
    }

    // Реальные тесты для YaXUnit
    @Ignore
    @Test
    fun yaxunitRealTestRunAll() {
        val properties = createTestApplicationProperties()
        val action = YaXUnitTestAction(platformDsl, utilLocator, reportParser)

        runBlocking {
            println("=== Запуск всех тестов YaXUnit ===")
            action.run(RunAllTestsRequest(properties))
        }
    }

    @Ignore
    @Test
    fun yaxunitRealTestRunModule() {
        val properties = createTestApplicationProperties()
        val action = YaXUnitTestAction(platformDsl, utilLocator, reportParser)
        val moduleName = "ОМ_ЮТКоллекции" // Модуль с тестами

        runBlocking {
            println("=== Запуск тестов модуля '$moduleName' ===")
            action.run(RunModuleTestsRequest(moduleName, properties))
        }
    }

    @Ignore
    @Test
    fun yaxunitRealTestRunSpecificTests() {
        val properties = createTestApplicationProperties()
        val action = YaXUnitTestAction(platformDsl, utilLocator, reportParser)
        val testNames = listOf("TestExample", "TestCalculator") // Примеры имен тестов

        runBlocking {
            println("=== Запуск конкретных тестов: ${testNames.joinToString(", ")} ===")
            action.run(RunListTestsRequest(testNames, properties))
        }
    }

    @Ignore
    @Test
    fun yaxunitRealTestRunSingleTest() {
        val properties = createTestApplicationProperties()
        val action = YaXUnitTestAction(platformDsl, utilLocator, reportParser)
        val testName = "TestExample" // Пример имени теста

        runBlocking {
            println("=== Запуск одного теста: '$testName' ===")
            action.run(RunListTestsRequest(listOf(testName), properties))
        }
    }

    /**
     * Создает тестовые свойства приложения для тестирования
     */
    private fun createTestApplicationProperties(): ApplicationProperties =
        ApplicationProperties(
            basePath = Path(SOURCE_PATH),
            sourceSet =
                SourceSet(
                    listOf(
                        SourceSetItem(
                            path = "configuration",
                            name = "configuration",
                            type = SourceSetType.CONFIGURATION,
                            purpose = setOf(SourceSetPurpose.MAIN),
                        ),
                        SourceSetItem(
                            path = "yaxunit",
                            name = "yaxunit",
                            type = SourceSetType.EXTENSION,
                            purpose = setOf(SourceSetPurpose.YAXUNIT),
                        ),
                        SourceSetItem(
                            path = "tests",
                            name = "tests",
                            type = SourceSetType.EXTENSION,
                            purpose = setOf(SourceSetPurpose.TESTS),
                        ),
                    ),
                ),
            connection =
                ConnectionProperties(
                    connectionString = "File=\"$IB_PATH\";",
                ),
            platformVersion = VERSION,
            tools = ToolsProperties(),
        )

    /**
     * Создает кастомные свойства приложения для тестирования
     */
    private fun createCustomApplicationProperties(): ApplicationProperties =
        ApplicationProperties(
            basePath = Path("/custom/path/to/project"),
            sourceSet =
                SourceSet(
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
