package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.ConfigCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigApplyCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigCheckCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigDataSeparationListCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigExportCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigExtensionCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigGenerationIdCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigImportCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigLoadCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigRepairCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigResetCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigSaveCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigSignCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigSupportDisableCommand
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Тесты для генерации всех команд блока config в ibcmd
 *
 * Покрывает следующие команды:
 * - ConfigLoadCommand - загрузка конфигурации
 * - ConfigSaveCommand - выгрузка конфигурации
 * - ConfigCheckCommand - проверка конфигурации
 * - ConfigApplyCommand - применение конфигурации
 * - ConfigResetCommand - сброс конфигурации
 * - ConfigExportCommand - экспорт конфигурации в XML
 * - ConfigImportCommand - импорт конфигурации из XML
 * - ConfigSignCommand - цифровая подпись конфигурации
 * - ConfigDataSeparationListCommand - список разделителей ИБ
 * - ConfigRepairCommand - восстановление конфигурации
 * - ConfigSupportDisableCommand - снятие с поддержки
 * - ConfigGenerationIdCommand - идентификатор поколения
 * - ConfigExtensionCommand - управление расширениями
 *
 * Тесты проверяют:
 * - Корректность создания команд
 * - Правильность генерации аргументов
 * - Формирование описаний команд
 * - Работу с общими параметрами
 * - Подкоманды расширений
 * - Использование ConfigCommandBuilder
 */
class IbCmdDslConfig {

    lateinit var builder: ConfigCommandBuilder

    @BeforeEach
    fun initBuilder() {
        builder = ConfigCommandBuilder()
    }


    @Test
    fun testConfigLoadCommand() {
        val command = builder.load("/path/to/config.cf") {
            extension = "MyExtension"
            force = true
        }.let { builder.result[0] } as ConfigLoadCommand

        assertEquals("config load", command.commandName)
        assertEquals("/path/to/config.cf", command.path)
        assertEquals("MyExtension", command.extension)
        assertTrue(command.force)
    }

    @Test
    fun testConfigSaveCommand() {
        val command = builder.save("/path/to/export.cf") {
            extension = "MyExtension"
            db = true
        }.let { builder.result[0] } as ConfigSaveCommand

        assertEquals("config save", command.commandName)
        assertEquals("/path/to/export.cf", command.path)
        assertEquals("MyExtension", command.extension)
        assertTrue(command.db)
    }

    @Test
    fun testConfigCheckCommand() {
        val command = builder.check {
            extension = "MyExtension"
            force = true
        }.let { builder.result[0] } as ConfigCheckCommand

        assertEquals("config check", command.commandName)
        assertEquals("MyExtension", command.extension)
        assertTrue(command.force)
    }

    @Test
    fun testConfigApplyCommand() {
        val command = builder.apply {
            extension = "MyExtension"
            force = true
        }.let { builder.result[0] } as ConfigApplyCommand

        assertEquals("config apply", command.commandName)
        assertEquals("MyExtension", command.extension)
        assertTrue(command.force)
    }

    @Test
    fun testConfigResetCommand() {
        val command = builder.reset {
            extension = "MyExtension"
        }.let { builder.result[0] } as ConfigResetCommand

        assertEquals("config reset", command.commandName)
        assertEquals("MyExtension", command.extension)
    }

    @Test
    fun testConfigExportCommand() {
        val command = builder.export("/path/to/export.xml") {
            extension = "MyExtension"
            force = true
        }.let { builder.result[0] } as ConfigExportCommand

        assertEquals("config export", command.commandName)
        assertEquals("/path/to/export.xml", command.path)
        assertEquals("MyExtension", command.extension)
        assertTrue(command.force)
    }

    @Test
    fun testConfigImportCommand() {
        val command = builder.import("/path/to/import.xml") {
            extension = "MyExtension"
        }.let { builder.result[0] } as ConfigImportCommand

        assertEquals("config import", command.commandName)
        assertEquals("/path/to/import.xml", command.path)
        assertEquals("MyExtension", command.extension)
    }

    @Test
    fun testConfigSignCommand() {
        val command = builder.sign {
            key = "/path/to/private.key"
            extension = "MyExtension"
            db = true
            out = "/path/to/signed.cf"
            path = "/path/to/config.cf"
        }.let { builder.result[0] } as ConfigSignCommand

        assertEquals("config sign", command.commandName)
        assertEquals("/path/to/private.key", command.key)
        assertEquals("MyExtension", command.extension)
        assertTrue(command.db)
        assertEquals("/path/to/signed.cf", command.out)
        assertEquals("/path/to/config.cf", command.path)
    }

    @Test
    fun testConfigDataSeparationListCommand() {
        val command = builder.dataSeparationList()
            .let { builder.result[0] } as ConfigDataSeparationListCommand

        assertEquals("config data-separation list", command.commandName)
        assertEquals("Список разделителей информационной базы", command.getFullDescription())
    }

    @Test
    fun testConfigRepairCommand() {
        val command = builder.repair {
            commit = true
            rollback = false
            fixMetadata = true
        }.let { builder.result[0] } as ConfigRepairCommand

        assertEquals("config repair", command.commandName)
        assertTrue(command.commit)
        assertTrue(!command.rollback)
        assertTrue(command.fixMetadata)
    }

    @Test
    fun testConfigSupportDisableCommand() {
        val command = builder.supportDisable {
            force = true
        }.let { builder.result[0] } as ConfigSupportDisableCommand

        assertEquals("config support disable", command.commandName)
        assertTrue(command.force)
    }

    @Test
    fun testConfigGenerationIdCommand() {
        val command = builder.generationId {
            extension = "MyExtension"
        }.let { builder.result[0] } as ConfigGenerationIdCommand

        assertEquals("config generation-id", command.commandName)
        assertEquals("MyExtension", command.extension)
    }

    @Test
    fun testConfigExtensionCommand() {
        val command = builder.extension("MyExtension", "create") {
            namePrefix = "ME"
            purpose = "add-on"
            active = true
            safeMode = false
        }.let { builder.result[0] } as ConfigExtensionCommand

        assertEquals("config extension create", command.commandName)
        assertEquals("MyExtension", command.name)
        assertEquals("ME", command.namePrefix)
        assertEquals("add-on", command.purpose)
        assertTrue(command.active!!)
        assertTrue(!command.safeMode!!)
    }

    @Test
    fun testConfigExtensionSubCommands() {
        val createCommand = ConfigExtensionCommand(
            extensionSubCommand = "create",
            name = "TestExtension",
            namePrefix = "TE",
            purpose = "add-on"
        )

        val infoCommand = ConfigExtensionCommand(
            extensionSubCommand = "info",
            name = "TestExtension"
        )

        val listCommand = ConfigExtensionCommand(
            extensionSubCommand = "list"
        )

        val updateCommand = ConfigExtensionCommand(
            extensionSubCommand = "update",
            name = "TestExtension",
            active = true
        )

        val deleteCommand = ConfigExtensionCommand(
            extensionSubCommand = "delete",
            name = "TestExtension"
        )

        assertEquals("config extension create", createCommand.commandName)
        assertEquals("config extension info", infoCommand.commandName)
        assertEquals("config extension list", listCommand.commandName)
        assertEquals("config extension update", updateCommand.commandName)
        assertEquals("config extension delete", deleteCommand.commandName)
    }

    @Test
    fun testConfigCommandsArgumentsGeneration() {
        val loadCommand = ConfigLoadCommand(
            path = "/path/to/config.cf",
            extension = "MyExtension",
            force = true
        )

        val arguments = loadCommand.arguments

        assertTrue(arguments.contains("/path/to/config.cf"))
        assertTrue(arguments.contains("--extension"))
        assertTrue(arguments.contains("MyExtension"))
        assertTrue(arguments.contains("--force"))
    }

    @Test
    fun testConfigSignCommandArguments() {
        val signCommand = ConfigSignCommand(
            key = "/path/to/key.pem",
            extension = "MyExtension",
            db = true,
            out = "/path/to/signed.cf",
            path = "/path/to/config.cf"
        )

        val arguments = signCommand.arguments
        assertTrue(arguments.contains("--key"))
        assertTrue(arguments.contains("/path/to/key.pem"))
        assertTrue(arguments.contains("--extension"))
        assertTrue(arguments.contains("MyExtension"))
        assertTrue(arguments.contains("--db"))
        assertTrue(arguments.contains("--out"))
        assertTrue(arguments.contains("/path/to/signed.cf"))
        assertTrue(arguments.contains("/path/to/config.cf"))
    }

    @Test
    fun testConfigCommandsFullDescription() {
        val loadCommand = ConfigLoadCommand(
            path = "/path/to/config.cf",
            extension = "MyExtension"
        )

        val description = loadCommand.getFullDescription()

        assertTrue(description.contains("Загрузка конфигурации"))
        assertTrue(description.contains("/path/to/config.cf"))
        assertTrue(description.contains("расширение: MyExtension"))
    }

    @Test
    fun testConfigExtensionCommandWithAllParameters() {
        val command = ConfigExtensionCommand(
            extensionSubCommand = "create",
            name = "TestExtension",
            namePrefix = "TE",
            synonym = "Test Ext",
            purpose = "add-on",
            active = true,
            safeMode = false,
            securityProfileName = true,
            unsafeActionProtection = true,
            usedInDistributedInfobase = false
        )

        assertEquals("TestExtension", command.name)
        assertEquals("TE", command.namePrefix)
        assertEquals("Test Ext", command.synonym)
        assertEquals("add-on", command.purpose)
        assertTrue(command.active!!)
        assertTrue(!command.safeMode!!)
        assertTrue(command.securityProfileName!!)
        assertTrue(command.unsafeActionProtection!!)
        assertTrue(!command.usedInDistributedInfobase!!)
    }
}