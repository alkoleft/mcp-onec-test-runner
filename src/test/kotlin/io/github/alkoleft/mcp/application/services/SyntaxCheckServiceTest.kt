/*
 * This file is part of METR.
 *
 * Copyright (C) 2025 Aleksey Koryakin <alkoleft@gmail.com>
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

package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.designer.DesignerValidationLogParser
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.DesignerDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.CheckConfigCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration

class SyntaxCheckServiceTest {
    private val platformDsl: PlatformDsl = mockk()
    private val properties: ApplicationProperties = mockk(relaxed = true)
    private val validationLogParser: DesignerValidationLogParser = DesignerValidationLogParser()
    private val service = SyntaxCheckService(platformDsl, properties, validationLogParser)

    @Test
    fun `checkDesignerConfig should configure CheckConfigCommand with options`() {
        val designerDsl: DesignerDsl = mockk(relaxed = true)
        val tempLogFile: Path = Files.createTempFile("syntax-check", ".log")
        tempLogFile.toFile().deleteOnExit()
        val logContent: String =
            """
            {ОбщийМодуль.Тест.Модуль(10,5)}: Процедура или функция с указанным именем не определена (ТестФункция)
            	Значение = ТестФункция(); (Проверка: Веб-клиент)
            Справочник.ТестовыйОбъект.Форма.ФормаЭлемента.Форма Возможно ошибочное свойство: "Поле"
            """.trimIndent()
        Files.writeString(tempLogFile, logContent)
        val processResult =
            ProcessResult(
                success = true,
                output = "ok",
                error = null,
                exitCode = 0,
                duration = Duration.ZERO,
                logFilePath = tempLogFile,
            )
        every { platformDsl.designer() } returns designerDsl
        every { designerDsl.checkConfig(any()) } answers {
            val configure = firstArg<CheckConfigCommand.() -> Unit>()
            val command = CheckConfigCommand()
            configure.invoke(command)
            assertThat(command.configLogIntegrity).isTrue()
            assertThat(command.incorrectReferences).isTrue()
            assertThat(command.thinClient).isTrue()
            assertThat(command.webClient).isTrue()
            assertThat(command.mobileClient).isTrue()
            assertThat(command.server).isTrue()
            assertThat(command.externalConnection).isTrue()
            assertThat(command.externalConnectionServer).isTrue()
            assertThat(command.mobileAppClient).isTrue()
            assertThat(command.mobileAppServer).isTrue()
            assertThat(command.thickClientManagedApplication).isTrue()
            assertThat(command.thickClientServerManagedApplication).isTrue()
            assertThat(command.thickClientOrdinaryApplication).isTrue()
            assertThat(command.thickClientServerOrdinaryApplication).isTrue()
            assertThat(command.mobileClientDigiSign).isTrue()
            assertThat(command.distributiveModules).isTrue()
            assertThat(command.unreferenceProcedures).isTrue()
            assertThat(command.handlersExistence).isTrue()
            assertThat(command.emptyHandlers).isTrue()
            assertThat(command.extendedModulesCheck).isTrue()
            assertThat(command.checkUseSynchronousCalls).isTrue()
            assertThat(command.checkUseModality).isTrue()
            assertThat(command.unsupportedFunctional).isTrue()
            assertThat(command.extension).isEqualTo("ExtensionName")
            assertThat(command.allExtensions).isTrue()
            processResult
        }

        val options =
            DesignerConfigCheckRequest(
                configLogIntegrity = true,
                incorrectReferences = true,
                thinClient = true,
                webClient = true,
                mobileClient = true,
                server = true,
                externalConnection = true,
                externalConnectionServer = true,
                mobileAppClient = true,
                mobileAppServer = true,
                thickClientManagedApplication = true,
                thickClientServerManagedApplication = true,
                thickClientOrdinaryApplication = true,
                thickClientServerOrdinaryApplication = true,
                mobileClientDigiSign = true,
                distributiveModules = true,
                unreferenceProcedures = true,
                handlersExistence = true,
                emptyHandlers = true,
                extendedModulesCheck = true,
                checkUseSynchronousCalls = true,
                checkUseModality = true,
                unsupportedFunctional = true,
                extension = "ExtensionName",
                allExtensions = true,
            )

        val result = service.checkDesigner(options)

        assertThat(result.success).isTrue()
        assertThat(result.output).isEqualTo("ok")
        assertThat(result.error).isNull()
        assertThat(result.exitCode).isZero()
        assertThat(result.logFilePath).isEqualTo(tempLogFile.toString())
        assertThat(result.analysis).isNotNull()
    }

    @Test
    fun `checkDesignerConfig should throw when synchronous checks without extended`() {
        val options =
            DesignerConfigCheckRequest(
                checkUseSynchronousCalls = true,
            )

        assertThatThrownBy { service.checkDesigner(options) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("ExtendedModulesCheck")
    }
}
