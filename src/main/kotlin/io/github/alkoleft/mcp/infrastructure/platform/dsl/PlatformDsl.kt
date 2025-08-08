package io.github.alkoleft.mcp.infrastructure.platform.dsl

import io.github.alkoleft.mcp.infrastructure.platform.locator.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.DesignerDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.enterprise.EnterpriseDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdPlan
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdPlanDsl
import org.springframework.stereotype.Component

/**
 * DSL для работы с утилитами платформы 1С:Предприятие
 *
 * Предоставляет удобный интерфейс для работы с конфигуратором и ibcmd
 * через fluent API и DSL синтаксис.
 */
@Component
class PlatformDsl(
    private val utilLocator: CrossPlatformUtilLocator
) {
    /**
     * DSL для работы с конфигуратором 1С
     */
    fun configurator(version: String? = null, block: DesignerDsl.() -> Unit): DesignerDsl {
        val context = PlatformUtilityContext(utilLocator, version)
        val designerDsl = DesignerDsl(context)
        designerDsl.block()
        return designerDsl
    }

    /**
     * DSL для формирования плана команд ibcmd с поддержкой иерархической структуры
     */
    fun ibcmd(version: String? = null, block: IbcmdPlanDsl.() -> Unit): IbcmdPlan {
        val context = PlatformUtilityContext(utilLocator, version)
        val ibcmdPlanDsl = IbcmdPlanDsl(context)
        ibcmdPlanDsl.block()
        return ibcmdPlanDsl.buildPlan()
    }

    /**
     * DSL для работы с 1С:Предприятие
     */
    fun enterprise(version: String? = null, block: EnterpriseDsl.() -> Unit): EnterpriseDsl {
        val context = PlatformUtilityContext(utilLocator, version)
        val enterpriseDsl = EnterpriseDsl(context)
        enterpriseDsl.block()
        return enterpriseDsl
    }
} 