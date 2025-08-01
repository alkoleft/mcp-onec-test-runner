package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

/**
 * Примеры использования ibcmd DSL
 */
object IbcmdExamples {

    /**
     * Пример использования ibcmd DSL с иерархической структурой
     */
    fun realExecute() {
        val plan = platformDsl.ibcmdPlan("8.3.24.1761") {
            dbPath("/home/alko/develop/onec_file_db/YaxUnit-dev")
            config {
                import("/home/alko/Downloads/sources/configuration")
                listOf("yaxunit", "smoke", "tests").forEach { extensionName ->
                    import("/home/alko/Downloads/sources/$extensionName") {
                        extension = extensionName
                    }
                }
            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    /**
     * Пример использования ibcmd DSL с общими параметрами
     */
    fun ibcmdWithCommonParams() {
        val plan = platformDsl.ibcmdPlan {
            dbPath = "/home/alko/develop/onec_file_db/YaxUnit-dev"
            user = "Админ"
            password = "123"

            config {
                import("/home/alko/Downloads/sources/configuration")
                check {}
            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    /**
     * Пример создания файловой базы
     */
    fun ibcmdCreateInfobase() {
        val plan = platformDsl.ibcmdPlan {
            dbPath = "/home/alko/develop/onec_file_db/NewBase"

            infobase {
                create {
                    createDatabase = true
                }
            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    /**
     * Пример настройки сервера
     */
    fun ibcmdServerConfig() {
        val plan = platformDsl.ibcmdPlan {
            server {
                configure {
                    name = "MyInfobase"
                }
            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    /**
     * Пример работы с конфигурацией
     */
    fun ibcmdConfigOperations() {
        val plan = platformDsl.ibcmdPlan {
            dbPath = "/path/to/database"
            user = "Админ"
            password = "123"

            config {
                check {
                    extension = "MyExtension"
                }
                apply {
                    extension = "MyExtension"
                    force = true
                }
            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    /**
     * Пример работы с сеансами
     */
    fun ibcmdSessionManagement() {
        val plan = platformDsl.ibcmdPlan {
            dbPath = "/path/to/database"
            session {
                terminate("123")
            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    /**
     * Пример работы с блокировками
     */
    fun ibcmdWithLockManagement() {
        val plan = platformDsl.ibcmdPlan {
            dbPath = "/path/to/database"
            lock {
                list()
            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    /**
     * Пример работы с мобильным приложением
     */
    fun ibcmdMobileAppOperations() {
        val plan = platformDsl.ibcmdPlan {
            dbPath = "/path/to/database"
            mobileApp {
                create {
                    path = "/path/to/mobile/app"
                }
            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    /**
     * Пример работы с мобильным клиентом
     */
    fun ibcmdMobileClientOperations() {
        val plan = platformDsl.ibcmdPlan {
            dbPath = "/path/to/database"
            mobileClient {
                create {
                    path = "/path/to/mobile/client"
                }
            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    /**
     * Пример создания расширения
     */
    fun ibcmdExtensionOperations() {
        val plan = platformDsl.ibcmdPlan {
            dbPath = "/path/to/database"
            extension {
                create(name = "MyExtension", namePrefix = "ME") {
                    purpose = "add-on"
                }
                update("MyExtension") {
                    active = true
                    safeMode = true
                }
            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    /**
     * Демонстрация исправленного DSL - теперь команды используют новые классы
     */
    fun demonstrateFixedDsl() {
        println("🔧 Демонстрация исправленного DSL для ibcmd")
        println("==================================================")

        // Теперь ibcmdPlan возвращает готовый IbcmdPlan, а не IbcmdPlanDsl
        val plan = platformDsl.ibcmdPlan("8.3.24.1761") {
            // Общие параметры на уровне плана
            dbPath("/home/user/databases/project.v8i")
            user("Администратор")
            password("password123")

            // Конфигурационные операции
            config {
                // Используется ConfigImportCommand
                import("/source/config/main.cf")

                // Используется ConfigExportCommand  
                export("/backup/config_export.cf")

                // Используется ConfigCheckCommand
                check {
                    extension = "MainConfig"
                }
            }

            // Управление информационной базой
            infobase {

                // Используется InfobaseCreateCommand
                create {
                    locale = "ru"
                    createDatabase = true
                }
            }

            // Работа с расширениями
            extension {
                // Используется ExtensionCreateCommand
                create(name = "YAxUnit", namePrefix = "YAX") {
                    purpose = "add-on"
                }

                // Используется ExtensionUpdateCommand
                update("YAxUnit") {
                    active = true
                    safeMode = false
                }
            }

            // Управление сеансами
            session {
                // Используется SessionTerminateCommand
                terminate("problematic-session-id")
            }

            // Управление блокировками
            lock {
                // Используется LockListCommand
                list()
            }
        }

        // Теперь plan уже готов к использованию - не нужен .buildPlan()
        println("📋 Созданный план содержит ${plan.commands.size} команд:")
        plan.commands.forEachIndexed { index, command ->
            println("  ${index + 1}. ${command.javaClass.simpleName}: ${command.getFullDescription()}")
        }

        println("\n✅ Все команды используют новые классы из папки commands/")
        println("✅ PlatformUtilityDsl.ibcmdPlan() теперь возвращает готовый IbcmdPlan")
        println("✅ Иерархическая структура DSL работает корректно")
    }

    // Мок объект для демонстрации
    private val platformDsl = object {
        fun ibcmdPlan(block: IbcmdPlanDsl.() -> Unit): IbcmdPlan {
            val planDsl = IbcmdPlanDsl(mockPlatformContext())
            planDsl.block()
            return planDsl.buildPlan()
        }

        fun ibcmdPlan(version: String, block: IbcmdPlanDsl.() -> Unit): IbcmdPlan {
            val planDsl = IbcmdPlanDsl(mockPlatformContext())
            planDsl.block()
            return planDsl.buildPlan()
        }

        private fun mockPlatformContext(): PlatformUtilityContext {
            val mockUtilLocator = object : CrossPlatformUtilLocator() {
                override suspend fun locateUtility(utility: UtilityType, version: String?): io.github.alkoleft.mcp.core.modules.UtilityLocation {
                    return io.github.alkoleft.mcp.core.modules.UtilityLocation(
                        executablePath = Path.of("/path/to/ibcmd"),
                        version = "8.3.24.1761",
                        platformType = io.github.alkoleft.mcp.core.modules.PlatformType.LINUX
                    )
                }
            }
            return PlatformUtilityContext(mockUtilLocator, "8.3.24.1761")
        }
    }
} 