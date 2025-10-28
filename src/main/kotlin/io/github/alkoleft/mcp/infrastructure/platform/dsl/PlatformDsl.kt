package io.github.alkoleft.mcp.infrastructure.platform.dsl

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.DesignerDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.edt.EdtDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.enterprise.EnterpriseDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdDsl
import org.springframework.stereotype.Component

/**
 * DSL для работы с утилитами платформы 1С:Предприятие
 *
 * Предоставляет удобный интерфейс для работы с конфигуратором и ibcmd
 * через fluent API и DSL синтаксис.
 */
@Component
class PlatformDsl(
    private val context: PlatformUtilityContext,
) {
    /**
     * DSL для работы с конфигуратором 1С
     */
    fun configurator(block: DesignerDsl.() -> Unit): DesignerDsl {
        val designerDsl = DesignerDsl(context)
        designerDsl.block()
        return designerDsl
    }

    /**
     * DSL для формирования плана команд ibcmd с поддержкой иерархической структуры
     */
    fun ibcmd(block: IbcmdDsl.() -> Unit): IbcmdDsl {
        val ibcmdDsl = IbcmdDsl(context)
        ibcmdDsl.block()
        return ibcmdDsl
    }

    /**
     * DSL для работы с 1С:Предприятие
     */
    fun enterprise(block: EnterpriseDsl.() -> Unit): EnterpriseDsl {
        val enterpriseDsl = EnterpriseDsl(context)
        enterpriseDsl.block()
        return enterpriseDsl
    }

    /**
     * DSL для работы с 1C:EDT CLI. Команды выполняются сразу и возвращают результат.
     */
    fun edt(block: EdtDsl.() -> Unit): EdtDsl {
        val edtDsl = EdtDsl(context)
        edtDsl.block()
        return edtDsl
    }
}
