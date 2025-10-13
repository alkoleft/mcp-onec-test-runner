package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.CommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.ConfigCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.ExtensionCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.InfobaseCommandBuilder
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.SessionCommandBuilder

/**
 * DSL для формирования плана команд ibcmd с поддержкой иерархической структуры.
 * Позволяет строить команды конфигурации, информационной базы, сеансов и расширений.
 */
class IbcmdPlanDsl(
    context: PlatformUtilityContext,
) {
    private val ibcmdContext = IbcmdContext(context)

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

    var tempDataPath: String? = null
        set(value) {
            field = value
            ibcmdContext.data(value ?: "")
        }

    /**
     * Устанавливает путь к каталогу временных данных.
     *
     * @param path Путь к каталогу.
     */
    fun data(path: String) {
        this.tempDataPath = path
        ibcmdContext.data(path)
    }

    /**
     * Устанавливает путь к базе данных.
     *
     * @param path Путь к базе.
     */
    fun dbPath(path: String) {
        this.dbPath = path
    }

    /**
     * Устанавливает пользователя для подключения.
     *
     * @param user Имя пользователя.
     */
    fun user(user: String) {
        this.user = user
    }

    /**
     * Устанавливает пароль для подключения.
     *
     * @param password Пароль.
     */
    fun password(password: String) {
        this.password = password
    }

    /**
     * Добавляет команду конфигурации в план.
     *
     * @param block Блок для построения команд конфигурации.
     */
    fun config(block: ConfigCommandBuilder.() -> Unit) = appendSubCommands(block)

    /**
     * Добавляет команды информационной базы в план.
     *
     * @param block Блок для построения команд ИБ.
     */
    fun infobase(block: InfobaseCommandBuilder.() -> Unit) = appendSubCommands(block)

    /**
     * Добавляет команды сеансов в план.
     *
     * @param block Блок для построения команд сеансов.
     */
    fun session(block: SessionCommandBuilder.() -> Unit) = appendSubCommands(block)

    /**
     * Добавляет команды расширений в план.
     *
     * @param block Блок для построения команд расширений.
     */
    fun extension(block: ExtensionCommandBuilder.() -> Unit) = appendSubCommands(block)

    /**
     * Строит план выполнения команд.
     *
     * @return IbcmdPlan с командами и контекстом.
     */
    fun buildPlan(): IbcmdPlan = IbcmdPlan(commands.toList(), ibcmdContext)

    /**
     * Добавляет подкоманды из блока построителя.
     * Использует рефлексию для создания экземпляра построителя.
     *
     * @param block Блок DSL.
     */
    private inline fun <reified T : CommandBuilder> appendSubCommands(block: T.() -> Unit) {
        val clazz = T::class.java
        val builder = clazz.getDeclaredConstructor().newInstance()
        builder.block()
        commands.addAll(builder.result)
    }
}

