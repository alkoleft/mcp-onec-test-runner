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

package io.github.alkoleft.mcp

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.alkoleft.mcp.configuration.ExternalConfigLoader
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import org.slf4j.LoggerFactory
import org.springframework.ai.util.json.JsonParser
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableConfigurationProperties(ApplicationProperties::class)
@EnableAsync
@SpringBootApplication
class McpYaxUnitRunnerApplication

fun main(args: Array<String>) {
    if (PlatformDetector.isWindows) {
        System.setProperty("file.encoding", "UTF-8")
    }

    reconfigureLogback()
    configureJsonParser()

    runApplication<McpYaxUnitRunnerApplication>(*args) {
        setAdditionalProfiles("mcp")
        addInitializers(ExternalConfigLoader())
    }
}

/**
 * Настраивает ObjectMapper в Spring AI JsonParser для корректной
 * сериализации Kotlin data-классов в native image.
 */
private fun configureJsonParser() {
    val mapper = JsonParser.getObjectMapper()
    mapper.registerModule(KotlinModule.Builder().build())
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
    mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
}

private fun reconfigureLogback() {
    val configPath = System.getProperty("logging.config")
        ?: System.getenv("LOGGING_CONFIG")

    val resourceName = if (configPath != null) {
        configPath.removePrefix("classpath:")
    } else {
        "logback-mcp.xml"
    }

    val url = Thread.currentThread().contextClassLoader?.getResource(resourceName)
        ?: return

    try {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.reset()
        val configurator = JoranConfigurator()
        configurator.context = loggerContext
        configurator.doConfigure(url)
    } catch (e: Exception) {
        System.err.println("Ошибка переконфигурации logback: ${e.message}")
    }
}
