package io.github.alkoleft.mcp.infrastructure.platform.dsl

import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.ConfiguratorDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.ConfiguratorPlanDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdDsl
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

/**
 * DSL для работы с утилитами платформы 1С:Предприятие
 *
 * Предоставляет удобный интерфейс для работы с конфигуратором и ibcmd
 * через fluent API и DSL синтаксис.
 */
@Component
class PlatformUtilityDsl(
    private val utilLocator: CrossPlatformUtilLocator
) {

    /**
     * Создает контекст для работы с утилитами платформы
     */
    fun platform(version: String? = null, block: PlatformUtilityContext.() -> Unit): PlatformUtilityResult {
        val context = PlatformUtilityContext(utilLocator, version)
        context.block()
        return context.buildResult()
    }

    /**
     * DSL для работы с конфигуратором 1С
     */
    fun configurator(version: String? = null, block: ConfiguratorDsl.() -> Unit): ConfiguratorDsl {
        val context = PlatformUtilityContext(utilLocator, version)
        val configuratorDsl = ConfiguratorDsl(context)
        configuratorDsl.block()
        return configuratorDsl
    }

    /**
     * DSL для формирования плана команд конфигуратора 1С
     */
    fun configuratorPlan(version: String? = null, block: ConfiguratorPlanDsl.() -> Unit): ConfiguratorPlanDsl {
        val context = PlatformUtilityContext(utilLocator, version)
        val configuratorPlanDsl = ConfiguratorPlanDsl(context)
        configuratorPlanDsl.block()
        return configuratorPlanDsl
    }

    /**
     * DSL для работы с ibcmd (Инфобазный менеджер)
     */
    fun ibcmd(version: String? = null, block: IbcmdDsl.() -> Unit): IbcmdDsl {
        val context = PlatformUtilityContext(utilLocator, version)
        val ibcmdDsl = IbcmdDsl(context)
        ibcmdDsl.block()
        return ibcmdDsl
    }

    /**
     * Синхронная версия для использования в blocking контекстах
     */
    fun platformSync(version: String? = null, block: PlatformUtilityContext.() -> Unit): PlatformUtilityResult {
        return runBlocking {
            platform(version, block)
        }
    }
} 