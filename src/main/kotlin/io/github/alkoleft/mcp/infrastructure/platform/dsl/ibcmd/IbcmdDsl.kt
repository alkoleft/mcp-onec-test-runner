package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.Dsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.CommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.extension.ExtensionCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.infobase.InfobaseCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.lock.LockListCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.mobile.MobileAppExportCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.mobile.MobileClientExportCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.server.ServerConfigInitCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.session.SessionCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * DSL для формирования и выполнения команд ibcmd с поддержкой иерархической структуры.
 *
 * Основной класс для работы с утилитой ibcmd, предоставляющий fluent API для создания команд
 * управления информационными базами, конфигурациями, расширениями, сеансами и другими объектами.
 *
 * Поддерживает следующие режимы работы:
 * - [config] - работа с конфигурацией
 * - [infobase] - управление информационными базами
 * - [server] - управление сервером
 * - [session] - управление сеансами
 * - [lock] - управление блокировками
 * - [mobileApp] - экспорт мобильных приложений
 * - [mobileClient] - экспорт мобильных клиентов
 * - [extension] - управление расширениями
 *
 * ## Пример использования:
 * ```kotlin
 * val dsl = IbcmdDsl(context)
 * dsl.dbPath("path/to/database")
 * dsl.user("admin")
 * dsl.password("password")
 *
 * dsl.infobase {
 *   create { /* параметры */ }
 * }
 *
 * val plan = dsl.buildPlan()
 * plan.execute()
 * ```
 *
 * @param context контекст платформы, содержащий информацию о доступных утилитах
 *
 * @see [IbcmdContext] для параметров подключения к базе данных
 * @see [IbcmdCommand] базовый интерфейс команд
 *
 * @author alkoleft
 * @since 1.0
 */
class IbcmdDsl(
    context: PlatformUtilityContext,
) : Dsl<IbcmdContext, IbcmdCommand>(IbcmdContext(context)) {
    private val ibcmdContext = IbcmdContext(context)

    private val commands = mutableListOf<IbcmdCommand>()

    /**
     * Путь к базе данных для всех команд.
     *
     * При установке автоматически обновляет контекст [IbcmdContext].
     */
    var dbPath: String? = null
        set(value) {
            field = value
            ibcmdContext.dbPath(value ?: "")
        }

    /**
     * Имя пользователя для подключения к базе данных.
     *
     * При установке автоматически обновляет контекст [IbcmdContext].
     */
    var user: String? = null
        set(value) {
            field = value
            ibcmdContext.user(value ?: "")
        }

    /**
     * Пароль для подключения к базе данных.
     *
     * При установке автоматически обновляет контекст [IbcmdContext].
     */
    var password: String? = null
        set(value) {
            field = value
            ibcmdContext.password(value ?: "")
        }

    /**
     * Устанавливает путь к базе данных.
     *
     * @param path путь к файлу базы данных или строка подключения
     *
     * @see [IbcmdContext.dbPath]
     */
    fun dbPath(path: String) {
        this.dbPath = path
    }

    /**
     * Устанавливает имя пользователя для подключения к базе данных.
     *
     * @param user имя пользователя
     *
     * @see [IbcmdContext.user]
     */
    fun user(user: String) {
        this.user = user
    }

    /**
     * Устанавливает пароль для подключения к базе данных.
     *
     * @param password пароль пользователя
     *
     * @see [IbcmdContext.password]
     */
    fun password(password: String) {
        this.password = password
    }

    /**
     * Добавляет команды конфигурации в план.
     *
     * Поддерживаемые операции:
     * - [ConfigCommandBuilder.load] - загрузка конфигурации из файла
     * - [ConfigCommandBuilder.save] - сохранение конфигурации в файл
     * - [ConfigCommandBuilder.check] - проверка конфигурации
     * - [ConfigCommandBuilder.apply] - применение конфигурации
     * - [ConfigCommandBuilder.export] - экспорт конфигурации в XML
     * - [ConfigCommandBuilder.import] - импорт конфигурации из XML
     * - и другие операции с конфигурацией
     *
     * @param block блок конфигурации для добавления команд
     * @return результата выполнения команды через [configureAndExecute]
     */
    fun config(block: ConfigCommandBuilder.() -> Unit) = appendSubCommands(ConfigCommandBuilder(this), block)

    /**
     * Добавляет команды управления информационными базами в план.
     *
     * Поддерживаемые операции:
     * - [InfobaseCommandBuilder.create] - создание новой информационной базы
     * - [InfobaseCommandBuilder.dump] - выгрузка данных
     * - [InfobaseCommandBuilder.restore] - загрузка данных
     * - [InfobaseCommandBuilder.clear] - очистка базы
     * - [InfobaseCommandBuilder.replicate] - репликация базы
     *
     * @param block блок для добавления команд управления ИБ
     * @return результата выполнения команды через [configureAndExecute]
     */
    fun infobase(block: InfobaseCommandBuilder.() -> Unit) = appendSubCommands(InfobaseCommandBuilder(this), block)

    /**
     * Добавляет команды управления сервером в план.
     *
     * Поддерживаемые операции:
     * - [ServerPlanDsl.configure] - настройка сервера через [ServerConfigurePlanDsl]
     *
     * @param block блок для добавления команд сервера
     */
    fun server(block: ServerPlanDsl.() -> Unit) {
        val dsl = ServerPlanDsl()
        dsl.block()
        commands.addAll(dsl.buildCommands())
    }

    /**
     * Добавляет команды управления сеансами в план.
     *
     * Поддерживаемые операции:
     * - [SessionCommandBuilder.info] - получение информации о сеансе
     * - [SessionCommandBuilder.list] - получение списка сеансов
     * - [SessionCommandBuilder.terminate] - завершение сеанса
     *
     * @param block блок для добавления команд управления сеансами
     * @return результата выполнения команды через [configureAndExecute]
     */
    fun session(block: SessionCommandBuilder.() -> Unit) = appendSubCommands(SessionCommandBuilder(this), block)

    /**
     * Добавляет команды управления блокировками в план.
     *
     * Поддерживаемые операции:
     * - [LockPlanDsl.list] - получение списка активных блокировок
     *
     * @param block блок для добавления команд управления блокировками
     */
    fun lock(block: LockPlanDsl.() -> Unit) {
        val dsl = LockPlanDsl()
        dsl.block()
        commands.addAll(dsl.buildCommands())
    }

    /**
     * Добавляет команды экспорта мобильных приложений в план.
     *
     * Поддерживаемые операции:
     * - [MobileAppPlanDsl.create] - экспорт мобильного приложения через [MobileAppCreatePlanDsl]
     *
     * @param block блок для добавления команд мобильного приложения
     */
    fun mobileApp(block: MobileAppPlanDsl.() -> Unit) {
        val dsl = MobileAppPlanDsl()
        dsl.block()
        commands.addAll(dsl.buildCommands())
    }

    /**
     * Добавляет команды экспорта мобильных клиентов в план.
     *
     * Поддерживаемые операции:
     * - [MobileClientPlanDsl.create] - экспорт мобильного клиента через [MobileClientCreatePlanDsl]
     *
     * @param block блок для добавления команд мобильного клиента
     */
    fun mobileClient(block: MobileClientPlanDsl.() -> Unit) {
        val dsl = MobileClientPlanDsl()
        dsl.block()
        commands.addAll(dsl.buildCommands())
    }

    /**
     * Добавляет команды управления расширениями в план.
     *
     * Поддерживаемые операции:
     * - [ExtensionCommandBuilder.create] - создание расширения
     * - [ExtensionCommandBuilder.info] - получение информации о расширении
     * - [ExtensionCommandBuilder.list] - список расширений
     * - [ExtensionCommandBuilder.update] - обновление расширения
     * - [ExtensionCommandBuilder.delete] - удаление расширения
     *
     * @param block блок для добавления команд управления расширениями
     * @return результата выполнения команды через [configureAndExecute]
     */
    fun extension(block: ExtensionCommandBuilder.() -> Unit) = appendSubCommands(ExtensionCommandBuilder(this), block)

    /**
     * Добавляет подкоманды через builder в список команд.
     *
     * @param builder билдер команд заданного типа
     * @param block блок конфигурации команд
     */
    private inline fun <reified T : CommandBuilder> appendSubCommands(builder: T, block: T.() -> Unit) {
        builder.block()
        commands.addAll(builder.result)
    }

    /**
     * Выполняет команду ibcmd с заданными аргументами.
     *
     * Формирует полный список аргументов команды (включая базовые параметры),
     * запускает процесс выполнения с логированием и возвращает результат.
     *
     * @param command команда для выполнения
     * @return результат выполнения команды с метриками
     *
     * @see [buildCommandArgsWithArgs] для формирования аргументов
     * @see [ProcessExecutor] для выполнения команды
     */
    override suspend fun executeCommand(command: IbcmdCommand): ShellCommandResult {
        val duration =
            measureTime {
                try {
                    val executor = ProcessExecutor()

                    val args = buildCommandArgsWithArgs(command.arguments)
                    val result = executor.executeWithLogging(args)

                    context.setResult(
                        success = result.exitCode == 0,
                        output = result.output,
                        error = result.error,
                        exitCode = result.exitCode,
                        duration = result.duration,
                    )
                } catch (e: Exception) {
                    context.setResult(
                        success = false,
                        output = "",
                        error = e.message ?: "Unknown error",
                        exitCode = -1,
                        duration = Duration.ZERO,
                    )
                }
            }

        return ProcessResult(
            success = context.buildResult().success,
            output = context.buildResult().output,
            error = context.buildResult().error,
            exitCode = context.buildResult().exitCode,
            duration = duration,
        )
    }

    /**
     * Настраивает и выполняет команду.
     *
     * Применяет блок конфигурации к команде и выполняет её синхронно
     * в блокирующем контексте через [runBlocking].
     *
     * @param command команда для настройки и выполнения
     * @param configure опциональный блок конфигурации команды
     * @return результат выполнения команды
     */
    internal fun <C : IbcmdCommand> configureAndExecute(
        command: C,
        configure: (C.() -> Unit)?,
    ): ShellCommandResult {
        if (configure != null) {
            command.configure()
        }
        return runBlocking {
            executeCommand(command)
        }
    }
}

/**
 * DSL для планирования команд управления сервером.
 *
 * Предоставляет fluent API для создания команд настройки и управления сервером 1С.
 *
 * ## Пример использования:
 * ```kotlin
 * server {
 *   configure {
 *     out("server.xml")
 *     httpAddress("localhost")
 *     httpPort(8080)
 *   }
 * }
 * ```
 *
 * @see [ServerConfigurePlanDsl] для настройки параметров сервера
 */
class ServerPlanDsl {
    private val commands = mutableListOf<IbcmdCommand>()

    /**
     * Добавляет команду инициализации конфигурации сервера.
     *
     * @param block блок конфигурации сервера через [ServerConfigurePlanDsl]
     */
    fun configure(block: ServerConfigurePlanDsl.() -> Unit) {
        val dsl = ServerConfigurePlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Строит список команд управления сервером.
     *
     * @return неизменяемый список команд для добавления в основной план
     */
    fun buildCommands(): List<IbcmdCommand> = commands.toList()
}

/**
 * DSL для планирования команды инициализации конфигурации сервера.
 *
 * Используется для создания файла конфигурации сервера с заданными параметрами.
 *
 * ## Пример использования:
 * ```kotlin
 * configure {
 *   out("server.xml")
 *   httpAddress("192.168.1.1")
 *   httpPort(8080)
 *   httpBase("/ib")
 *   name("my-server")
 * }
 * ```
 *
 * @see [ServerConfigInitCommand] для выполнения команды
 */
class ServerConfigurePlanDsl {
    var out: String? = null
    var httpAddress: String? = null
    var httpPort: Int? = null
    var httpBase: String? = null
    var name: String? = null

    /**
     * Устанавливает путь к выходному файлу конфигурации сервера.
     *
     * @param out путь к XML файлу конфигурации
     */
    fun out(out: String) {
        this.out = out
    }

    /**
     * Устанавливает IP-адрес для HTTP подключений к серверу.
     *
     * @param address IP-адрес или hostname
     */
    fun httpAddress(address: String) {
        this.httpAddress = address
    }

    /**
     * Устанавливает TCP порт для HTTP подключений к серверу.
     *
     * @param port номер порта
     */
    fun httpPort(port: Int) {
        this.httpPort = port
    }

    /**
     * Устанавливает базовый путь (URL path) для доступа к серверу.
     *
     * @param base базовый путь, например "/ib" или "/base"
     */
    fun httpBase(base: String) {
        this.httpBase = base
    }

    /**
     * Устанавливает имя информационной базы на сервере.
     *
     * @param name имя базы данных
     */
    fun name(name: String) {
        this.name = name
    }

    /**
     * Строит команду инициализации конфигурации сервера.
     *
     * @return команда для выполнения
     */
    fun buildCommand(): ServerConfigInitCommand =
        ServerConfigInitCommand(
            out = out,
            httpAddress = httpAddress,
            httpPort = httpPort,
            httpBase = httpBase,
            name = name,
        )
}

/**
 * DSL для планирования команд управления блокировками данных.
 *
 * Позволяет получать информацию о текущих блокировках в информационной базе.
 *
 * ## Пример использования:
 * ```kotlin
 * lock {
 *   list {
 *     session("uuid-123-456")
 *   }
 * }
 * ```
 *
 * @see [LockListPlanDsl] для настройки команды списка блокировок
 */
class LockPlanDsl {
    private val commands = mutableListOf<IbcmdCommand>()

    /**
     * Добавляет команду получения списка активных блокировок.
     *
     * @param block опциональный блок конфигурации команды
     */
    fun list(block: LockListPlanDsl.() -> Unit = {}) {
        val dsl = LockListPlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Строит список команд блокировок
     */
    fun buildCommands(): List<IbcmdCommand> = commands.toList()
}

/**
 * DSL для планирования команды получения списка блокировок.
 *
 * ## Пример использования:
 * ```kotlin
 * list {
 *   session("uuid-session-id")
 * }
 * ```
 *
 * @see [LockListCommand] для выполнения команды
 */
class LockListPlanDsl {
    var session: String? = null

    /**
     * Устанавливает идентификатор сеанса для фильтрации блокировок.
     *
     * @param sessionId UUID сеанса
     */
    fun session(sessionId: String) {
        this.session = sessionId
    }

    /**
     * Строит команду получения списка блокировок.
     *
     * @return команда для выполнения
     */
    fun buildCommand(): LockListCommand =
        LockListCommand(
            session = session,
        )
}

/**
 * DSL для планирования команд экспорта мобильных приложений.
 *
 * Используется для создания пакетов мобильных приложений для развертывания.
 *
 * ## Пример использования:
 * ```kotlin
 * mobileApp {
 *   create {
 *     path("./mobile_app.zip")
 *   }
 * }
 * ```
 */
class MobileAppPlanDsl {
    private val commands = mutableListOf<IbcmdCommand>()

    /**
     * Добавляет команду экспорта мобильного приложения.
     *
     * @param block блок конфигурации команды экспорта
     */
    fun create(block: MobileAppCreatePlanDsl.() -> Unit) {
        val dsl = MobileAppCreatePlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Строит список команд мобильного приложения
     */
    fun buildCommands(): List<IbcmdCommand> = commands.toList()
}

/**
 * DSL для планирования команды экспорта мобильного приложения.
 *
 * Создает пакет для развертывания мобильного приложения на устройстве пользователя.
 *
 * @see [MobileAppExportCommand] для выполнения команды
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
     * Строит команду экспорта мобильного приложения.
     *
     * @return команда для выполнения
     */
    fun buildCommand(): MobileAppExportCommand =
        MobileAppExportCommand(
            path = path.ifEmpty { "/default/mobile/app/path" },
        )
}

/**
 * DSL для планирования команд экспорта мобильных клиентов.
 *
 * Используется для создания пакетов мобильных клиентов для развертывания.
 *
 * ## Пример использования:
 * ```kotlin
 * mobileClient {
 *   create {
 *     path("./mobile_client.zip")
 *   }
 * }
 * ```
 */
class MobileClientPlanDsl {
    private val commands = mutableListOf<IbcmdCommand>()

    /**
     * Добавляет команду экспорта мобильного клиента.
     *
     * @param block блок конфигурации команды экспорта
     */
    fun create(block: MobileClientCreatePlanDsl.() -> Unit) {
        val dsl = MobileClientCreatePlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Строит список команд мобильного клиента
     */
    fun buildCommands(): List<IbcmdCommand> = commands.toList()
}

/**
 * DSL для планирования команды экспорта мобильного клиента.
 *
 * Создает пакет для развертывания мобильного клиента на устройстве пользователя.
 *
 * @see [MobileClientExportCommand] для выполнения команды
 */
class MobileClientCreatePlanDsl {
    var path: String = ""

    /**
     * Устанавливает путь для сохранения экспортированного мобильного клиента.
     *
     * @param path путь к выходному файлу (обычно .zip)
     */
    fun path(path: String) {
        this.path = path
    }

    /**
     * Строит команду экспорта мобильного клиента.
     *
     * @return команда для выполнения
     */
    fun buildCommand(): MobileClientExportCommand =
        MobileClientExportCommand(
            path = path.ifEmpty { "/default/mobile/client/path" },
        )
}
