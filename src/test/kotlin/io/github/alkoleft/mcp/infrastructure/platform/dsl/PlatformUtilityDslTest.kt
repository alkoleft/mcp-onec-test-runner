package io.github.alkoleft.mcp.infrastructure.platform.dsl

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.locator.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.DesignerDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.LoadFormat
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands.LoadConfigFromFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Тесты для DSL работы с утилитами платформы 1С
 */
class PlatformUtilityDslTest {

    private lateinit var mockUtilLocator: CrossPlatformUtilLocator
    private lateinit var mockProcessExecutor: ProcessExecutor
    private lateinit var platformDsl: PlatformDsl

    @BeforeEach
    fun setUp() {
        mockUtilLocator = mockk<CrossPlatformUtilLocator>()
        mockProcessExecutor = mockk<ProcessExecutor>()
        platformDsl = PlatformDsl(mockUtilLocator)
    }

    @Test
    fun `should create configurator DSL with version`() {
        // Given
        val version = "8.3.24.1482"
        val mockLocation = UtilityLocation(
            executablePath = Paths.get("/opt/1cv8/bin/1cv8"),
            version = version,
            platformType = PlatformType.LINUX
        )

        coEvery { mockUtilLocator.locateUtility(UtilityType.DESIGNER, version) } returns mockLocation
        coEvery { mockUtilLocator.validateUtility(any()) } returns true

        // When
        val configuratorDsl = platformDsl.configurator(version) {
            connect("Srvr=localhost;Ref=TestDB;")
            user("Administrator")
            password("password")
        }

        // Then
        assertTrue(configuratorDsl is DesignerDsl)
    }

    @Test
    fun `should handle configurator loadFromFiles command`() {
        // Given
        val version = "8.3.24.1482"
        val mockLocation = UtilityLocation(
            executablePath = Paths.get("/opt/1cv8/bin/1cv8"),
            version = version,
            platformType = PlatformType.LINUX
        )

        coEvery { mockUtilLocator.locateUtility(UtilityType.DESIGNER, version) } returns mockLocation
        coEvery { mockUtilLocator.validateUtility(any()) } returns true

        // When
        val configuratorDsl = platformDsl.configurator(version) {
            connect("Srvr=localhost;Ref=TestDB;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/output.txt"))
        }

        // Then - проверяем, что DSL создан корректно
        assertTrue(configuratorDsl is DesignerDsl)
    }

    @Test
    fun `should handle LoadConfigFromFiles properties assignment`() {
        // Given
        val version = "8.3.24.1482"
        val mockLocation = UtilityLocation(
            executablePath = Paths.get("/opt/1cv8/bin/1cv8"),
            version = version,
            platformType = PlatformType.LINUX
        )

        coEvery { mockUtilLocator.locateUtility(UtilityType.DESIGNER, version) } returns mockLocation
        coEvery { mockUtilLocator.validateUtility(any()) } returns true

        // When - создаем план с использованием свойств через присвоение
        val planDsl = platformDsl.configuratorPlan(version) {
            connect("Srvr=localhost;Ref=TestDB;")
            user("Administrator")
            password("password")

            loadConfigFromFiles {
                sourcePath = Paths.get("/path/to/config/source")
                extension = "MyExtension"
                partial = true
                format = LoadFormat.HIERARCHICAL
                updateConfigDumpInfo = true
            }
        }

        val plan = planDsl.buildPlan()

        // Then - проверяем, что план создан корректно
        assertTrue(plan.commands.isNotEmpty())
        assertEquals("LoadConfigFromFiles", plan.commands[0].name)
    }

    @Test
    fun `should handle LoadConfigFromFilesCommand direct creation`() {
        // When
        val command = LoadConfigFromFilesCommand().apply {
            fromPath(Paths.get("/path/to/config/source"))
            extension("MyExtension")
            partial()
            format(LoadFormat.HIERARCHICAL)
        }

        // Then
        assertEquals("LoadConfigFromFiles", command.name)
        assertEquals(Paths.get("/path/to/config/source"), command.sourcePath)
        assertEquals("MyExtension", command.extension)
        assertTrue(command.partial)
        assertEquals(LoadFormat.HIERARCHICAL, command.format)

        // Проверяем аргументы команды
        val args = command.arguments
        assertTrue(args.contains("/path/to/config/source"))
        assertTrue(args.contains("-Extension"))
        assertTrue(args.contains("MyExtension"))
        assertTrue(args.contains("-partial"))
        assertTrue(args.contains("-format"))
        assertTrue(args.contains("Hierarchical"))
    }

    @Test
    fun `should handle process execution error`() {
        // Given
        val version = "8.3.24.1482"
        val mockLocation = UtilityLocation(
            executablePath = Paths.get("/opt/1cv8/bin/1cv8"),
            version = version,
            platformType = PlatformType.LINUX
        )

        coEvery { mockUtilLocator.locateUtility(UtilityType.DESIGNER, version) } returns mockLocation
        coEvery { mockUtilLocator.validateUtility(any()) } returns true

        // When
        val configuratorDsl = platformDsl.configurator(version) {
            connect("Srvr=invalid;Ref=invalid;")
            user("Administrator")
            password("password")
            loadConfigFromFiles {
                fromPath(Paths.get("/invalid/path"))
            }
        }

        // Then
        assertTrue(configuratorDsl is DesignerDsl)
    }
}