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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands

/**
 * Трейт для поддержки параметра /UpdateDBCfg в командах конфигуратора
 *
 * Параметр /UpdateDBCfg допускается указывать после следующих параметров:
 * /LoadCfg — загрузка конфигурации из файла;
 * /UpdateCfg — обновление конфигурации, находящейся на поддержке;
 * /ConfigurationRepositoryUpdateCfg — обновление конфигурации из хранилища;
 * /LoadConfigFiles — загрузка файлов конфигурации;
 * /LoadConfigFromFiles — загрузка конфигурации из файлов;
 * /MobileAppUpdatePublication — обновление публикации мобильного приложения;
 * /MobileAppWriteFile — запись xml-файла мобильного приложения;
 * /MobileClientWriteFile — запись xml-файла мобильного клиента;
 * /MobileClientDigiSign — подпись мобильного клиента.
 */
interface UpdateDBCfgSupport {
    /**
     * Флаг для добавления параметра /UpdateDBCfg
     */
    var updateDBCfg: Boolean

    /**
     * DSL метод для включения параметра /UpdateDBCfg
     */
    fun updateDBCfg() {
        this.updateDBCfg = true
    }

    /**
     * Добавляет параметр /UpdateDBCfg к списку аргументов, если он включен
     */
    fun addUpdateDBCfgToArguments(args: MutableList<String>) {
        if (updateDBCfg) {
            args.add("/UpdateDBCfg")
        }
    }

    /**
     * Добавляет параметр updateDBCfg к параметрам команды, если он включен
     */
    fun addUpdateDBCfgToParameters(params: MutableMap<String, String>) {
        if (updateDBCfg) {
            params["updateDBCfg"] = "true"
        }
    }
}
