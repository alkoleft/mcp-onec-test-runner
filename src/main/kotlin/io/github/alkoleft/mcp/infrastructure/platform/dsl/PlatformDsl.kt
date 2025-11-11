/*
 * This file is part of METR.
 *
 * Copyright (C) 2025 Aleksey Koryakin <alkoleft@gmail.com> and contributors.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * METR is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * METR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with METR.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.alkoleft.mcp.infrastructure.platform.dsl

import io.github.alkoleft.mcp.application.core.UtilityType
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilities
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
    private val context: PlatformUtilities,
    private val properties: ApplicationProperties,
) {
    /**
     * DSL для работы с конфигуратором 1С
     */
    fun designer(block: (DesignerDsl.() -> Unit)? = null): DesignerDsl {
        val designerDsl = DesignerDsl(context)
        designerDsl.connect(properties)
        block?.let { designerDsl.block() }
        return designerDsl
    }

    /**
     * DSL для формирования плана команд ibcmd с поддержкой иерархической структуры
     */
    fun ibcmd(block: IbcmdDsl.() -> Unit): IbcmdDsl {
        val ibcmdDsl = IbcmdDsl(context)
        ibcmdDsl.connect(properties)
        ibcmdDsl.block()
        return ibcmdDsl
    }

    /**
     * DSL для работы с 1С:Предприятие
     */
    fun enterprise(block: EnterpriseDsl.() -> Unit): EnterpriseDsl {
        val enterpriseDsl = EnterpriseDsl(context, UtilityType.THIN_CLIENT)
        enterpriseDsl.connect(properties)
        enterpriseDsl.block()
        return enterpriseDsl
    }

    /**
     * DSL для работы с 1С:Предприятие
     */
    fun enterprise(
        utilityType: UtilityType,
        block: EnterpriseDsl.() -> Unit,
    ): EnterpriseDsl {
        val enterpriseDsl = EnterpriseDsl(context, utilityType)
        enterpriseDsl.connect(properties)
        enterpriseDsl.block()
        return enterpriseDsl
    }

    /**
     * DSL для работы с 1C:EDT CLI. Команды выполняются сразу и возвращают результат.
     */
    fun edt(block: (EdtDsl.() -> Unit)? = null): EdtDsl {
        val edtDsl = EdtDsl(context)
        if (block != null) {
            edtDsl.block()
        }
        return edtDsl
    }
}
