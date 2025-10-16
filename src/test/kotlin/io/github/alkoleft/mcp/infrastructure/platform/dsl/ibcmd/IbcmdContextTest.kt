package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Тесты для класса IbcmdContext.
 * Проверяют построение базовых аргументов и получение пути к утилите.
 */
class IbcmdContextTest {
    private lateinit var mockPlatformContext: PlatformUtilityContext
    private lateinit var context: IbcmdContext

    /**
     * Инициализация перед каждым тестом.
     * Настраивает мок контекста платформы.
     */
    @BeforeEach
    fun setUp() {
        mockPlatformContext = mockk()
        val mockUtil = mockk<io.github.alkoleft.mcp.core.modules.UtilityLocation>(relaxed = true)
        every { mockUtil.name } returns "ibcmd"
        every { mockUtil.executablePath.toString() } returns "/mock/path/to/ibcmd.exe"
        every { mockPlatformContext.locateUtilitySync(UtilityType.IBCMD) } returns mockUtil
        context = IbcmdContext(mockPlatformContext)
    }

    /**
     * Тест: buildBaseArgs должен включать все параметры, когда они заданы.
     * Проверяет наличие всех флагов и значений в аргументах.
     */
    @Test
    fun `buildBaseArgs should include all parameters when set`() {
        context.dbPath("/db/path")
        context.user("testuser")
        context.password("testpass")
        context.data("/temp/data")

        val args = context.buildBaseArgs()

        assertTrue(args.contains("--db-path"))
        assertTrue(args.contains("/db/path"))
        assertTrue(args.contains("--user"))
        assertTrue(args.contains("testuser"))
        assertTrue(args.contains("--password"))
        assertTrue(args.contains("testpass"))
        assertTrue(args.contains("--data"))
        assertTrue(args.contains("/temp/data"))
    }

    /**
     * Тест: buildBaseArgs должен опускать незаданные параметры.
     * Проверяет, что только заданные параметры включены.
     */
    @Test
    fun `buildBaseArgs should omit unset parameters`() {
        context.dbPath("/db/path")

        val args = context.buildBaseArgs()

        assertEquals(2, args.size)
        assertEquals("--db-path", args[0])
        assertEquals("/db/path", args[1])
    }

    /**
     * Тест: utilityPath должен возвращать расположенный путь.
     * Проверяет возврат мок пути к утилите.
     */
    @Test
    fun `utilityPath should return located path`() {
        assertEquals("/mock/path/to/ibcmd.exe", context.utilityPath)
    }
}
