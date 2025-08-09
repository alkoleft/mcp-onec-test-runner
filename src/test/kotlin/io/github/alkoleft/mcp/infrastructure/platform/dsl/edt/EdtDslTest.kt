package io.github.alkoleft.mcp.infrastructure.platform.dsl.edt

import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityLocator
import io.github.alkoleft.mcp.testApplicationProperties
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EdtDslTest {

    @Test
    fun `should create EdtDsl with context`() {
        val mockLocator = mock(UtilityLocator::class.java)
        val context = PlatformUtilityContext(mockLocator, testApplicationProperties())
        val edtDsl = EdtDsl(context)

        // Test that EdtDsl is created successfully and has the right type
        assertTrue(edtDsl::class == EdtDsl::class)
    }

    @Test
    fun `should create EdtContext with platform context`() {
        val mockLocator = mock(UtilityLocator::class.java)
        val platformContext = PlatformUtilityContext(mockLocator, testApplicationProperties())
        val edtContext = EdtContext(platformContext)

        // Test that EdtContext is created successfully and has the right type
        assertTrue(edtContext::class == EdtContext::class)
        assertEquals(platformContext, edtContext.platformContext)
    }

    @Test
    fun `should create EdtResult with correct properties`() {
        val result = EdtResult(
            success = true,
            output = "test output",
            error = null,
            exitCode = 0,
            duration = kotlin.time.Duration.ZERO
        )

        assertTrue(result.success)
        assertEquals("test output", result.output)
        assertEquals(null, result.error)
        assertEquals(0, result.exitCode)
        assertEquals(kotlin.time.Duration.ZERO, result.duration)
    }

    @Test
    fun `should create empty EdtResult`() {
        val emptyResult = EdtResult.EMPTY

        assertFalse(emptyResult.success)
        assertEquals("", emptyResult.output)
        assertEquals("", emptyResult.error)
        assertEquals(-1, emptyResult.exitCode)
        assertEquals(kotlin.time.Duration.ZERO, emptyResult.duration)
    }

    @Test
    fun `should build EDT arguments correctly`() {
        val mockLocator = mock(UtilityLocator::class.java)
        val platformContext = PlatformUtilityContext(mockLocator, testApplicationProperties())
        val edtContext = EdtContext(platformContext)

        // Mock the utility location
        val mockLocation = mock(UtilityLocation::class.java)
        `when`(mockLocation.executablePath).thenReturn(Paths.get("/opt/1C/1CE/components/1.0.0/1cedtcli"))
        `when`(platformContext.locateUtilitySync(UtilityType.EDT_CLI)).thenReturn(
            mockLocation
        )

        val args = edtContext.buildEdtArgs(listOf("version"))

        assertEquals(2, args.size)
        assertEquals("/opt/1C/1CE/components/1.0.0/1cedtcli", args[0])
        assertEquals("version", args[1])
    }

    @Test
    fun `should build build command correctly`() {
        val mockLocator = mock(UtilityLocator::class.java)
        val platformContext = PlatformUtilityContext(mockLocator, testApplicationProperties())
        val edtContext = EdtContext(platformContext)

        val mockLocation = mock(UtilityLocation::class.java)
        `when`(mockLocation.executablePath).thenReturn(Paths.get("/opt/1C/1CE/components/1.0.0/1cedtcli"))
        `when`(platformContext.locateUtilitySync(UtilityType.EDT_CLI)).thenReturn(
            mockLocation
        )

        val args = edtContext.buildEdtArgs(listOf("build", "--yes", "Project1", "Project2"))

        assertEquals(5, args.size)
        assertEquals("/opt/1C/1CE/components/1.0.0/1cedtcli", args[0])
        assertEquals("build", args[1])
        assertEquals("--yes", args[2])
        assertEquals("Project1", args[3])
        assertEquals("Project2", args[4])
    }

    @Test
    fun `should build infobase command correctly`() {
        val mockLocator = mock(UtilityLocator::class.java)
        val platformContext = PlatformUtilityContext(mockLocator, testApplicationProperties())
        val edtContext = EdtContext(platformContext)

        val mockLocation = mock(UtilityLocation::class.java)
        `when`(mockLocation.executablePath).thenReturn(Paths.get("/opt/1C/1CE/components/1.0.0/1cedtcli"))
        `when`(platformContext.locateUtilitySync(UtilityType.EDT_CLI)).thenReturn(
            mockLocation
        )

        val args = edtContext.buildEdtArgs(listOf("infobase", "--details"))

        assertEquals(3, args.size)
        assertEquals("/opt/1C/1CE/components/1.0.0/1cedtcli", args[0])
        assertEquals("infobase", args[1])
        assertEquals("--details", args[2])
    }

    @Test
    fun `should build export command correctly`() {
        val mockLocator = mock(UtilityLocator::class.java)
        val platformContext = PlatformUtilityContext(mockLocator, testApplicationProperties())
        val edtContext = EdtContext(platformContext)

        val mockLocation = mock(UtilityLocation::class.java)
        `when`(mockLocation.executablePath).thenReturn(Paths.get("/opt/1C/1CE/components/1.0.0/1cedtcli"))
        `when`(platformContext.locateUtilitySync(UtilityType.EDT_CLI)).thenReturn(
            mockLocation
        )

        val args = edtContext.buildEdtArgs(
            listOf(
                "export",
                "--project-name",
                "MyProject",
                "--configuration-files",
                "/path/to/config"
            )
        )

        assertEquals(6, args.size)
        assertEquals("/opt/1C/1CE/components/1.0.0/1cedtcli", args[0])
        assertEquals("export", args[1])
        assertEquals("--project-name", args[2])
        assertEquals("MyProject", args[3])
        assertEquals("--configuration-files", args[4])
        assertEquals("/path/to/config", args[5])
    }

    @Test
    fun `should set and build result correctly`() {
        val mockLocator = mock(UtilityLocator::class.java)
        val platformContext = PlatformUtilityContext(mockLocator, testApplicationProperties())
        val edtContext = EdtContext(platformContext)

        edtContext.setResult(
            success = true,
            output = "test output",
            error = null,
            exitCode = 0,
            duration = kotlin.time.Duration.ZERO
        )

        val result = edtContext.buildResult()
        assertTrue(result.success)
        assertEquals("test output", result.output)
        assertEquals(null, result.error)
        assertEquals(0, result.exitCode)
        assertEquals(kotlin.time.Duration.ZERO, result.duration)
    }
}
