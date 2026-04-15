package io.github.alkoleft.mcp.infrastructure.platform.dsl.process

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InteractiveProcessExecutorTest {
    @Test
    fun `findAutoResponse should return y for accept changes prompt`() {
        val output = "Принять изменения и продолжить обновление [y/n] : [INFO] Объект изменен"

        val response = InteractiveProcessExecutor.findAutoResponse(output)

        assertEquals("y", response)
    }

    @Test
    fun `findAutoResponse should return null for ordinary output`() {
        val output = "[INFO] Экспорт завершен"

        val response = InteractiveProcessExecutor.findAutoResponse(output)

        assertNull(response)
    }
}
