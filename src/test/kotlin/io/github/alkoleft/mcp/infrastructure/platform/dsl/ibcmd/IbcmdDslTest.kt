package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Тесты для DSL ibcmd
 */
class IbcmdDslTest {

    @Test
    fun `test ibcmd plan creation`() {
        val plan = createBasicTestPlan()

        assertNotNull(plan)
        assertNotNull(plan.commands)
        assertNotNull(plan.context)
    }

    @Test
    fun `test ibcmd plan with common parameters`() {
        val plan = createTestPlanWithCommonParams()

        assertNotNull(plan)
        assertTrue(plan.commands.isNotEmpty())

        // Проверяем, что общие параметры установлены в контексте
        val context = plan.context
        val baseArgs = context.buildBaseArgs()

        // Проверяем наличие базовых аргументов
        assertTrue(baseArgs.isNotEmpty())
    }

    @Test
    fun `test ibcmd plan with config commands`() {
        val plan = createConfigPlan()

        assertNotNull(plan)
        assertTrue(plan.commands.isNotEmpty())

        val commands = plan.commands
        assertTrue(commands.any { it.mode == "config" })
    }

    @Test
    fun `test ibcmd plan with infobase commands`() {
        val plan = createInfobasePlan()

        assertNotNull(plan)
        assertTrue(plan.commands.isNotEmpty())

        val commands = plan.commands
        assertTrue(commands.any { it.mode == "infobase" })
    }

    @Test
    fun `test ibcmd plan with session commands`() {
        val plan = createSessionPlan()

        assertNotNull(plan)
        assertTrue(plan.commands.isNotEmpty())

        val commands = plan.commands
        assertTrue(commands.any { it.mode == "session" })
    }

    @Test
    fun `test ibcmd plan with lock commands`() {
        val plan = createLockPlan()

        assertNotNull(plan)
        assertTrue(plan.commands.isNotEmpty())

        val commands = plan.commands
        assertTrue(commands.any { it.mode == "lock" })
    }

    @Test
    fun `test ibcmd plan with extension commands`() {
        val plan = createExtensionPlan()

        assertNotNull(plan)
        assertTrue(plan.commands.isNotEmpty())

        val commands = plan.commands
        assertTrue(commands.any { it.mode == "extension" })
    }

    @Test
    fun `test command arguments generation`() {
        val plan = createConfigPlan()

        assertNotNull(plan)
        assertTrue(plan.commands.isNotEmpty())

        // Проверяем, что команды генерируют аргументы
        plan.commands.forEach { command ->
            assertNotNull(command.arguments)
            assertTrue(command.arguments.isNotEmpty())
        }
    }

    @Test
    fun `test command descriptions`() {
        val plan = createConfigPlan()

        assertNotNull(plan)
        assertTrue(plan.commands.isNotEmpty())

        // Проверяем, что команды имеют описания
        plan.commands.forEach { command ->
            assertNotNull(command.getFullDescription())
            assertTrue(command.getFullDescription().isNotEmpty())
            assertNotNull(command.commandName)
            assertTrue(command.commandName.isNotEmpty())
        }
    }

    @Test
    fun `test comprehensive ibcmd plan with hierarchical structure and common parameters`() {
        val context = mockPlatformContext()
        val planDsl = IbcmdPlanDsl(context)

        // Устанавливаем общие параметры на уровне плана
        planDsl.dbPath = "/home/user/database/project.v8i"
        planDsl.user = "Администратор"
        planDsl.password = "password123"

        // Строим комплексный план с несколькими типами команд
        planDsl.config {
            // Импорт основной конфигурации
            import("/source/configuration")
            // Импорт расширений
            import("/source/extensions/yaxunit") {
                extension = "yaxunit"
            }
            // Проверка конфигурации
            check {
                extension = "yaxunit"
            }
            // Экспорт для резервного копирования
            export("/backup/config_backup.cf")
        }


        // Управление сеансами
        planDsl.session {
            terminate("active-session-uuid")
        }

        // Работа с расширениями
        planDsl.extension {
            create(name = "TestExtension", namePrefix = "TEST") {
                purpose = "patch"
            }

            update("TestExtension") {
                active = true
                safeMode = false
            }
        }

        val plan = planDsl.buildPlan()

        // Проверяем результат
        assertNotNull(plan)
        assertTrue(plan.commands.size >= 6) // Минимум 6 команд

        // Проверяем, что общие параметры применены
        val baseArgs = plan.context.buildBaseArgs()
        assertTrue(baseArgs.isNotEmpty())

        // Проверяем разнообразие команд
        val modes = plan.commands.map { it.mode }.toSet()
        assertTrue(modes.contains("config"))
        assertTrue(modes.contains("session"))
        assertTrue(modes.contains("extension"))

        // Проверяем, что все команды имеют корректные описания и аргументы
        plan.commands.forEach { command ->
            assertNotNull(command.commandName)
            assertTrue(command.commandName.isNotEmpty())
            assertNotNull(command.getFullDescription())
            assertTrue(command.getFullDescription().isNotEmpty())
            assertNotNull(command.arguments)
            assertTrue(command.arguments.isNotEmpty())
        }

        // Демонстрируем работу плана (без реального выполнения)
        plan.printPlan() // Это выведет план в лог
    }

    @Test
    fun `test DSL uses new command classes`() {
        val context = mockPlatformContext()
        val planDsl = IbcmdPlanDsl(context)

        planDsl.config {
            import("/test/config")
            export("/test/export")
            check {
                extension = "TestConfig"
            }
        }

        planDsl.infobase {
            create {
                locale = "ru"
                createDatabase = true
            }
        }

        planDsl.extension {
            create(name = "TestExt", namePrefix = "TE") {
                purpose = "add-on"
            }
            update("TestExt") {
                active = true
            }
        }

        val plan = planDsl.buildPlan()

        // Проверяем, что используются новые классы команд
        val commands = plan.commands
        assertTrue(commands.isNotEmpty())

        // Проверяем типы команд с помощью их классов
        val configImportCommands = commands.filter { it.mode == "config" && it.subCommand == "import" }
        val configExportCommands = commands.filter { it.mode == "config" && it.subCommand == "export" }
        val configCheckCommands = commands.filter { it.mode == "config" && it.subCommand == "check" }
        val infobaseCreateCommands = commands.filter { it.mode == "infobase" && it.subCommand == "create" }
        val extensionCreateCommands = commands.filter { it.mode == "extension" && it.subCommand == "create" }
        val extensionUpdateCommands = commands.filter { it.mode == "extension" && it.subCommand == "update" }

        // Проверяем, что команды созданы
        assertTrue(configImportCommands.isNotEmpty(), "ConfigImportCommand should be created")
        assertTrue(configExportCommands.isNotEmpty(), "ConfigExportCommand should be created")
        assertTrue(configCheckCommands.isNotEmpty(), "ConfigCheckCommand should be created")
        assertTrue(infobaseCreateCommands.isNotEmpty(), "InfobaseCreateCommand should be created")
        assertTrue(extensionCreateCommands.isNotEmpty(), "ExtensionCreateCommand should be created")
        assertTrue(extensionUpdateCommands.isNotEmpty(), "ExtensionUpdateCommand should be created")

        // Проверяем, что команды имеют правильные типы в их строковом представлении
        // Это подтверждает, что используются новые классы команд с правильными именами классов
        val commandClassNames = commands.map { it.javaClass.simpleName }
        assertTrue(commandClassNames.contains("ConfigImportCommand"), "Should use ConfigImportCommand class")
        assertTrue(commandClassNames.contains("ConfigExportCommand"), "Should use ConfigExportCommand class")
        assertTrue(commandClassNames.contains("ConfigCheckCommand"), "Should use ConfigCheckCommand class")
        assertTrue(commandClassNames.contains("InfobaseCreateCommand"), "Should use InfobaseCreateCommand class")
        assertTrue(commandClassNames.contains("ExtensionCreateCommand"), "Should use ExtensionCreateCommand class")
        assertTrue(commandClassNames.contains("ExtensionUpdateCommand"), "Should use ExtensionUpdateCommand class")

        println("✅ DSL использует новые классы команд:")
        commandClassNames.forEach { className ->
            println("  - $className")
        }
    }

    private fun createBasicTestPlan(): IbcmdPlan {
        val context = mockPlatformContext()
        val planDsl = IbcmdPlanDsl(context)

        planDsl.dbPath = "test-db-path"
        planDsl.config {
            import("test-config-path")
        }

        return planDsl.buildPlan()
    }

    private fun createTestPlanWithCommonParams(): IbcmdPlan {
        val context = mockPlatformContext()
        val planDsl = IbcmdPlanDsl(context)

        planDsl.dbPath = "test-db-path"
        planDsl.user = "test-user"
        planDsl.password = "test-password"

        planDsl.config {
            import("test-config-path")
        }

        return planDsl.buildPlan()
    }

    private fun createConfigPlan(): IbcmdPlan {
        val context = mockPlatformContext()
        val planDsl = IbcmdPlanDsl(context)

        planDsl.config {
            import("test-config-path")
            export("test-export-path")
        }

        return planDsl.buildPlan()
    }

    private fun createInfobasePlan(): IbcmdPlan {
        val context = mockPlatformContext()
        val planDsl = IbcmdPlanDsl(context)

        planDsl.infobase {
            create {
                locale = "ru"
                createDatabase = true
            }
        }

        return planDsl.buildPlan()
    }

    private fun createSessionPlan(): IbcmdPlan {
        val context = mockPlatformContext()
        val planDsl = IbcmdPlanDsl(context)

        planDsl.session {
            terminate("test-session-id")
        }

        return planDsl.buildPlan()
    }

    private fun createLockPlan(): IbcmdPlan {
        val context = mockPlatformContext()
        val planDsl = IbcmdPlanDsl(context)

        planDsl.lock {
            list()
        }

        return planDsl.buildPlan()
    }

    private fun createExtensionPlan(): IbcmdPlan {
        val context = mockPlatformContext()
        val planDsl = IbcmdPlanDsl(context)

        planDsl.extension {
            create(name = "TestExtension", namePrefix = "TE") {
                purpose = "add-on"
            }
            update("TestExtension") {
                active = true
                safeMode = true
            }
        }

        return planDsl.buildPlan()
    }

    private fun mockPlatformContext(): PlatformUtilityContext {
        val mockUtilLocator = object : CrossPlatformUtilLocator() {
            override suspend fun locateUtility(utility: UtilityType, version: String?): UtilityLocation {
                return UtilityLocation(
                    executablePath = Path.of("/path/to/ibcmd"),
                    version = "8.3.24.1761",
                    platformType = PlatformType.LINUX
                )
            }
        }
        return PlatformUtilityContext(mockUtilLocator, "8.3.24.1761")
    }
} 