package io.github.alkoleft.mcp.infrastructure.strategy.handlers

import io.github.alkoleft.mcp.core.modules.strategy.ErrorContext
import io.github.alkoleft.mcp.core.modules.strategy.ErrorResolution
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionErrorHandlerTest {

    private val handler = ConnectionErrorHandler()

    @Test
    fun `should handle connection timeout error`() {
        // Given
        val error = RuntimeException("Connection timeout")
        val context = ErrorContext(attempt = 1, maxAttempts = 3)

        // When
        val resolution = handler.handle(error, context)

        // Then
        assertTrue(resolution is ErrorResolution.Retry)
        val retry = resolution as ErrorResolution.Retry
        assertEquals(3, retry.maxAttempts)
        assertEquals(Duration.ofSeconds(5), retry.delay)
        assertEquals("Connection failed, retrying", retry.reason)
    }

    @Test
    fun `should handle connection refused error`() {
        // Given
        val error = RuntimeException("Connection refused")
        val context = ErrorContext(attempt = 3, maxAttempts = 3)

        // When
        val resolution = handler.handle(error, context)

        // Then
        assertTrue(resolution is ErrorResolution.Fail)
        val fail = resolution as ErrorResolution.Fail
        assertEquals("Connection refused", fail.reason)
        assertTrue(fail.details?.contains("Database server is not available") == true)
    }

    @Test
    fun `should handle timeout error after max attempts`() {
        // Given
        val error = RuntimeException("Connection timeout")
        val context = ErrorContext(attempt = 3, maxAttempts = 3)

        // When
        val resolution = handler.handle(error, context)

        // Then
        assertTrue(resolution is ErrorResolution.Fail)
        val fail = resolution as ErrorResolution.Fail
        assertEquals("Connection timeout", fail.reason)
        assertTrue(fail.details?.contains("Failed to connect to database after 3 attempts") == true)
    }

    @Test
    fun `should handle generic connection error`() {
        // Given
        val error = RuntimeException("Network error")
        val context = ErrorContext(attempt = 3, maxAttempts = 3)

        // When
        val resolution = handler.handle(error, context)

        // Then
        assertTrue(resolution is ErrorResolution.Fail)
        val fail = resolution as ErrorResolution.Fail
        assertEquals("Connection failed", fail.reason)
        assertEquals("Network error", fail.details)
    }

    @Test
    fun `should detect connection errors`() {
        // When & Then
        assertTrue(handler.canHandle(RuntimeException("Connection failed")))
        assertTrue(handler.canHandle(RuntimeException("подключение не удалось")))
        assertTrue(handler.canHandle(RuntimeException("Connect timeout")))
        assertTrue(handler.canHandle(RuntimeException("Network error")))
        assertTrue(handler.canHandle(RuntimeException("Connection refused")))
        assertTrue(!handler.canHandle(RuntimeException("Configuration error")))
    }

    @Test
    fun `should set next handler in chain`() {
        // Given
        val nextHandler = ConfigurationErrorHandler()

        // When
        val result = handler.setNext(nextHandler)

        // Then
        assertEquals(handler, result)
        assertEquals(nextHandler, handler.getNext())
    }
}
