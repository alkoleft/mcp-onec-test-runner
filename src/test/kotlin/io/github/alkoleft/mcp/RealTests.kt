package io.github.alkoleft.mcp

import io.github.alkoleft.mcp.application.actions.ActionConfiguration
import io.github.alkoleft.mcp.application.actions.build.DesignerBuildAction
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.io.path.Path
import kotlin.test.Ignore

const val sourcesPath = "/home/akoryakin@dellin.local/Загрузки/sources"
const val ibPath = "/home/common/develop/file-data-base/YAxUnit"
const val version = "8.3.22.1709"

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [
        PlatformUtilityDsl::class,
        CrossPlatformUtilLocator::class,
        ActionConfiguration::class
    ]
)
class RealTests(
    @Autowired private val platformDsl: PlatformUtilityDsl
) {
    @Test
    fun designerRealExecute() {
        val plan = platformDsl.configuratorPlan(version) {
            connectToFile(ibPath)
            disableStartupDialogs()
            disableStartupMessages()
            loadConfigFromFiles {
                fromPath(Path("$sourcesPath/configuration"))
            }
            listOf("yaxunit", "tests").forEach {
                loadConfigFromFiles {
                    fromPath(Path("$sourcesPath/$it"))
                    extension = it
                }
            }
        }.buildPlan()
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    @Ignore
    @Test
    fun ibcmdRealExecute() {
        val plan = platformDsl.ibcmd(version) {
            dbPath(ibPath)

            config {
                import("$sourcesPath/configuration")
                listOf("yaxunit", "tests").forEach {
                    import("$sourcesPath/$it") {
                        extension = it
                    }
                }

            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    // Тесты для DesignerBuildAction
    @Test
    fun designerBuildActionFullBuild() {
        val action = DesignerBuildAction(platformDsl)
        val projectProperties = createTestApplicationProperties()

        runBlocking {
            val result = action.build(projectProperties)
            println("Результат полной сборки: $result")
        }
    }

    @Ignore
    @Test
    fun designerBuildActionConfigurationBuild() {
        val action = DesignerBuildAction(platformDsl)
        val projectProperties = createTestApplicationProperties()

        runBlocking {
            val result = action.buildConfiguration(projectProperties)
            println("Результат сборки конфигурации: $result")
        }
    }

    @Ignore
    @Test
    fun designerBuildActionExtensionBuild() {
        val action = DesignerBuildAction(platformDsl)
        val projectProperties = createTestApplicationProperties()
        val extensionName = "yaxunit"

        runBlocking {
            val result = action.buildExtension(extensionName, projectProperties)
            println("Результат сборки расширения $extensionName: $result")
        }
    }

    @Ignore
    @Test
    fun designerBuildActionMultipleExtensionsBuild() {
        val action = DesignerBuildAction(platformDsl)
        val projectProperties = createTestApplicationProperties()
        val extensions = listOf("yaxunit", "tests")

        runBlocking {
            extensions.forEach { extensionName ->
                val result = action.buildExtension(extensionName, projectProperties)
                println("Результат сборки расширения $extensionName: $result")
            }
        }
    }

    @Ignore
    @Test
    fun designerBuildActionWithCustomPaths() {
        val action = DesignerBuildAction(platformDsl)
        val projectProperties = createCustomApplicationProperties()

        runBlocking {
            val result = action.build(projectProperties)
            println("Результат сборки с кастомными путями: $result")
        }
    }

    /**
     * Создает тестовые свойства приложения для тестирования
     */
    private fun createTestApplicationProperties(): ApplicationProperties {
        return ApplicationProperties(
            basePath = Path(sourcesPath),
            sourceSet = listOf(
                SourceSetItem(
                    path = "configuration",
                    name = "configuration",
                    type = SourceSetType.CONFIGURATION,
                    purpose = setOf(SourceSetPurpose.MAIN)
                ),
                SourceSetItem(
                    path = "yaxunit",
                    name = "yaxunit",
                    type = SourceSetType.EXTENSION,
                    purpose = setOf(SourceSetPurpose.YAXUNIT)
                ),
                SourceSetItem(
                    path = "tests",
                    name = "tests",
                    type = SourceSetType.EXTENSION,
                    purpose = setOf(SourceSetPurpose.TESTS)
                )
            ),
            connection = ConnectionProperties(
                connectionString = "File=\"$ibPath\";"
            ),
            platformVersion = version,
            tools = ToolsProperties()
        )
    }

    /**
     * Создает кастомные свойства приложения для тестирования
     */
    private fun createCustomApplicationProperties(): ApplicationProperties {
        return ApplicationProperties(
            basePath = Path("/custom/path/to/project"),
            sourceSet = listOf(
                SourceSetItem(
                    path = "src/configuration",
                    name = "main-config",
                    type = SourceSetType.CONFIGURATION,
                    purpose = setOf(SourceSetPurpose.MAIN)
                ),
                SourceSetItem(
                    path = "src/extensions/custom-extension",
                    name = "custom-extension",
                    type = SourceSetType.EXTENSION,
                    purpose = setOf(SourceSetPurpose.YAXUNIT)
                )
            ),
            connection = ConnectionProperties(
                connectionString = "File=\"/custom/path/to/database\"",
                user = "Admin",
                password = ""
            ),
            platformVersion = version,
            tools = ToolsProperties()
        )
    }
}