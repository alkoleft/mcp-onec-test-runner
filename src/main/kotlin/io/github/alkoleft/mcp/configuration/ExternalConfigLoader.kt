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

package io.github.alkoleft.mcp.configuration

import org.slf4j.LoggerFactory
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.FileSystemResource
import java.nio.file.Path

/**
 * Загрузчик внешних конфигурационных файлов для native image.
 *
 * Читает переменную окружения SPRING_CONFIG_IMPORT и загружает указанные
 * yml-файлы как property sources до создания бинов.
 */
class ExternalConfigLoader : ApplicationContextInitializer<ConfigurableApplicationContext> {

    private val logger = LoggerFactory.getLogger(ExternalConfigLoader::class.java)
    private val yamlLoader = YamlPropertySourceLoader()

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val environment = applicationContext.environment

        val configImport = environment.getProperty("spring.config.import")
            ?: System.getenv("SPRING_CONFIG_IMPORT")
            ?: return

        val files = configImport.split(",").map { it.trim() }.filter { it.isNotBlank() }

        for (entry in files) {
            val path = entry
                .removePrefix("optional:")
                .removePrefix("file:")
                .trim()

            val optional = entry.startsWith("optional:")
            val resource = FileSystemResource(Path.of(path))

            if (!resource.exists()) {
                if (optional) {
                    logger.debug("Опциональный конфиг не найден: {}", path)
                } else {
                    logger.warn("Конфигурационный файл не найден: {}", path)
                }
                continue
            }

            try {
                val propertySources = yamlLoader.load(path, resource)
                for (ps in propertySources) {
                    environment.propertySources.addLast(ps)
                    logger.info("Загружен внешний конфиг: {}", path)
                }
            } catch (e: Exception) {
                logger.error("Ошибка загрузки конфига {}: {}", path, e.message)
                if (!optional) {
                    throw IllegalStateException("Не удалось загрузить конфигурацию: $path", e)
                }
            }
        }
    }
}
