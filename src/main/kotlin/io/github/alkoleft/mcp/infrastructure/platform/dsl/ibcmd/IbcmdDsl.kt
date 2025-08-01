package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.CommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.CommonParameters
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.ConfigCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.ExtensionCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.InfobaseCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.SessionCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.lock.LockListCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.mobile.MobileAppExportCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.mobile.MobileClientExportCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.server.ServerConfigInitCommand

/**
 * DSL для формирования плана команд ibcmd с поддержкой иерархической структуры
 */
class IbcmdPlanDsl(
    context: PlatformUtilityContext
) {
    private val ibcmdContext = IbcmdContext(context)
    private val commonParameters = CommonParameters()

    private val commands = mutableListOf<IbcmdCommand>()

    // Общие параметры для всех команд
    var dbPath: String? = null
        set(value) {
            field = value
            ibcmdContext.dbPath(value ?: "")
        }

    var user: String? = null
        set(value) {
            field = value
            ibcmdContext.user(value ?: "")
        }

    var password: String? = null
        set(value) {
            field = value
            ibcmdContext.password(value ?: "")
        }

    /**
     * Устанавливает путь к базе данных
     */
    fun dbPath(path: String) {
        this.dbPath = path
    }

    /**
     * Устанавливает пользователя для подключения
     */
    fun user(user: String) {
        this.user = user
    }

    /**
     * Устанавливает пароль для подключения
     */
    fun password(password: String) {
        this.password = password
    }

    /**
     * Добавляет команду конфигурации в план
     */
    fun config(block: ConfigCommandBuilder.() -> Unit) =
        appendSubCommands(block)

    /**
     * Добавляет команды информационной базы в план
     */
    fun infobase(block: InfobaseCommandBuilder.() -> Unit) =
        appendSubCommands(block)

    /**
     * Добавляет команды сервера в план
     */
    fun server(block: ServerPlanDsl.() -> Unit) {
        val dsl = ServerPlanDsl()
        dsl.block()
        commands.addAll(dsl.buildCommands())
    }

    /**
     * Добавляет команды сеансов в план
     */
    fun session(block: SessionCommandBuilder.() -> Unit) = appendSubCommands(block)

    /**
     * Добавляет команды блокировок в план
     */
    fun lock(block: LockPlanDsl.() -> Unit) {
        val dsl = LockPlanDsl()
        dsl.block()
        commands.addAll(dsl.buildCommands())
    }

    /**
     * Добавляет команды мобильного приложения в план
     */
    fun mobileApp(block: MobileAppPlanDsl.() -> Unit) {
        val dsl = MobileAppPlanDsl()
        dsl.block()
        commands.addAll(dsl.buildCommands())
    }

    /**
     * Добавляет команды мобильного клиента в план
     */
    fun mobileClient(block: MobileClientPlanDsl.() -> Unit) {
        val dsl = MobileClientPlanDsl()
        dsl.block()
        commands.addAll(dsl.buildCommands())
    }

    /**
     * Добавляет команды расширений в план
     */
    fun extension(block: ExtensionCommandBuilder.() -> Unit) =
        appendSubCommands(block)

    /**
     * Строит план выполнения команд
     */
    fun buildPlan(): IbcmdPlan {
        return IbcmdPlan(commands.toList(), ibcmdContext)
    }

    private inline fun <reified T : CommandBuilder> appendSubCommands(block: T.() -> Unit) {
        val clazz = T::class.java
        val builder = clazz.getDeclaredConstructor().newInstance()
        builder.block()
        commands.addAll(builder.result)
    }
}

/**
 * DSL для планирования команд сервера
 */
class ServerPlanDsl {
    private val commands = mutableListOf<IbcmdCommand>()

    /**
     * Добавляет команду настройки сервера
     */
    fun configure(block: ServerConfigurePlanDsl.() -> Unit) {
        val dsl = ServerConfigurePlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Строит список команд сервера
     */
    fun buildCommands(): List<IbcmdCommand> {
        return commands.toList()
    }
}

/**
 * DSL для планирования команды настройки сервера
 */
class ServerConfigurePlanDsl {
    var out: String? = null
    var httpAddress: String? = null
    var httpPort: Int? = null
    var httpBase: String? = null
    var name: String? = null

    /**
     * Устанавливает путь к выходному файлу
     */
    fun out(out: String) {
        this.out = out
    }

    /**
     * Устанавливает HTTP адрес
     */
    fun httpAddress(address: String) {
        this.httpAddress = address
    }

    /**
     * Устанавливает HTTP порт
     */
    fun httpPort(port: Int) {
        this.httpPort = port
    }

    /**
     * Устанавливает HTTP базу
     */
    fun httpBase(base: String) {
        this.httpBase = base
    }

    /**
     * Устанавливает имя базы
     */
    fun name(name: String) {
        this.name = name
    }

    /**
     * Строит команду настройки сервера
     */
    fun buildCommand(): ServerConfigInitCommand {
        return ServerConfigInitCommand(
            out = out,
            httpAddress = httpAddress,
            httpPort = httpPort,
            httpBase = httpBase,
            name = name
        )
    }
}

/**
 * DSL для планирования команд блокировок
 */
class LockPlanDsl {
    private val commands = mutableListOf<IbcmdCommand>()

    /**
     * Добавляет команду списка блокировок
     */
    fun list(block: LockListPlanDsl.() -> Unit = {}) {
        val dsl = LockListPlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Строит список команд блокировок
     */
    fun buildCommands(): List<IbcmdCommand> {
        return commands.toList()
    }
}

/**
 * DSL для планирования команды списка блокировок
 */
class LockListPlanDsl {
    var session: String? = null

    /**
     * Устанавливает ID сеанса для фильтрации
     */
    fun session(sessionId: String) {
        this.session = sessionId
    }

    /**
     * Строит команду списка блокировок
     */
    fun buildCommand(): LockListCommand {
        return LockListCommand(
            session = session
        )
    }
}

/**
 * DSL для планирования команд мобильного приложения
 */
class MobileAppPlanDsl {
    private val commands = mutableListOf<IbcmdCommand>()

    /**
     * Добавляет команду создания мобильного приложения
     */
    fun create(block: MobileAppCreatePlanDsl.() -> Unit) {
        val dsl = MobileAppCreatePlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Строит список команд мобильного приложения
     */
    fun buildCommands(): List<IbcmdCommand> {
        return commands.toList()
    }
}

/**
 * DSL для планирования команды создания мобильного приложения
 */
class MobileAppCreatePlanDsl {
    var path: String = ""

    /**
     * Устанавливает путь для экспорта
     */
    fun path(path: String) {
        this.path = path
    }

    /**
     * Строит команду экспорта мобильного приложения
     */
    fun buildCommand(): MobileAppExportCommand {
        return MobileAppExportCommand(
            path = path.ifEmpty { "/default/mobile/app/path" }
        )
    }
}

/**
 * DSL для планирования команд мобильного клиента
 */
class MobileClientPlanDsl {
    private val commands = mutableListOf<IbcmdCommand>()

    /**
     * Добавляет команду создания мобильного клиента
     */
    fun create(block: MobileClientCreatePlanDsl.() -> Unit) {
        val dsl = MobileClientCreatePlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Строит список команд мобильного клиента
     */
    fun buildCommands(): List<IbcmdCommand> {
        return commands.toList()
    }
}

/**
 * DSL для планирования команды создания мобильного клиента
 */
class MobileClientCreatePlanDsl {
    var path: String = ""

    /**
     * Устанавливает путь для экспорта
     */
    fun path(path: String) {
        this.path = path
    }

    /**
     * Строит команду экспорта мобильного клиента
     */
    fun buildCommand(): MobileClientExportCommand {
        return MobileClientExportCommand(
            path = path.ifEmpty { "/default/mobile/client/path" }
        )
    }
}
