package io.github.alkoleft.mcp.infrastructure.platform.dsl.edt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EdtCliExecutorTest {
    @Test
    fun `resolveCommandTimeout should return configured timeout`() {
        assertEquals(5_400_000L, EdtCliExecutor.resolveCommandTimeout(5_400_000L))
    }

    @Test
    fun `findRelevantErrors should ignore known lexer based converter error`() {
        val output =
            """
            INFO Export started
            ERROR org.eclipse.xtext.conversion.impl.AbstractLexerBasedConverter - Only terminal rules are supported by lexer based converters but got ID which is an instance of ParserRule
            """.trimIndent()

        val errors = EdtCliExecutor.findRelevantErrors(output)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `findRelevantErrors should keep ordinary error`() {
        val output =
            """
            INFO Export started
            ERROR Unexpected failure during export
            """.trimIndent()

        val errors = EdtCliExecutor.findRelevantErrors(output)

        assertEquals(listOf("ERROR Unexpected failure during export"), errors)
    }

    @Test
    fun `findRelevantErrors should keep non ignored error when output contains both`() {
        val output =
            """
            ERROR org.eclipse.xtext.conversion.impl.AbstractLexerBasedConverter - Only terminal rules are supported by lexer based converters but got ID which is an instance of ParserRule
            CRITICAL Real conversion failure
            """.trimIndent()

        val errors = EdtCliExecutor.findRelevantErrors(output)

        assertEquals(listOf("CRITICAL Real conversion failure"), errors)
    }

    @Test
    fun `findRelevantErrors should return empty list when there are no error lines`() {
        val output =
            """
            INFO Export started
            WARN Some warning
            1C:EDT>
            """.trimIndent()

        val errors = EdtCliExecutor.findRelevantErrors(output)

        assertTrue(errors.isEmpty())
    }
}
