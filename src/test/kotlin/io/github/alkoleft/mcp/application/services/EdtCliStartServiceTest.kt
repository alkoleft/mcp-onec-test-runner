package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.EdtCliProperties
import io.github.alkoleft.mcp.configuration.properties.ProjectFormat
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.InteractiveProcessExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class EdtCliStartServiceTest {
    @Mock
    private lateinit var mockProperties: ApplicationProperties

    @Mock
    private lateinit var mockToolsProperties: ToolsProperties

    @Mock
    private lateinit var mockEdtCliProperties: EdtCliProperties

    @Mock
    private lateinit var mockInteractiveExecutor: InteractiveProcessExecutor

    private lateinit var edtCliStartService: EdtCliStartService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockProperties.tools).thenReturn(mockToolsProperties)
        `when`(mockToolsProperties.edtCli).thenReturn(mockEdtCliProperties)
        `when`(mockEdtCliProperties.startupTimeoutMs).thenReturn(15000L)
        edtCliStartService = EdtCliStartService(mockProperties)
    }

    @Test
    fun shouldReturnNullWhenEdtFormatNotSelected() {
        `when`(mockProperties.format).thenReturn(ProjectFormat.DESIGNER)
        val result = edtCliStartService.interactiveExecutor()
        assertNull(result)
    }

    @Test
    fun shouldReturnNullWhenEdtFormatIsNull() {
        `when`(mockProperties.format).thenReturn(null)
        val result = edtCliStartService.interactiveExecutor()
        assertNull(result)
    }

    @Test
    fun shouldReturnCorrectStatusWhenEdtFormatNotSelected() {
        `when`(mockProperties.format).thenReturn(ProjectFormat.DESIGNER)
        val status = edtCliStartService.getExecutorStatus()
        assertEquals("EDT формат не выбран", status)
    }

    @Test
    fun shouldReturnCorrectStatusWhenNotStarted() {
        `when`(mockProperties.format).thenReturn(ProjectFormat.EDT)
        val status = edtCliStartService.getExecutorStatus()
        assertEquals("Не запущен", status)
    }

    @Test
    fun shouldReturnCorrectStatusWhenAutoStartNotEnabled() {
        `when`(mockProperties.format).thenReturn(ProjectFormat.EDT)
        val status = edtCliStartService.getExecutorStatus()
        assertFalse(edtCliStartService.isAutoStartEnabled())
    }
}
