package io.github.alkoleft.mcp.infrastructure.utility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths

class ConnectionStringUtilsTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `extractDbPath should return original for non-File connection string`() {
        val connectionString = "Srvr=server;Ref=base;"
        val basePath = Paths.get("/project")
        val result = ConnectionStringUtils.extractDbPath(connectionString, basePath)
        assertEquals(connectionString, result)
    }

    @Test
    fun `extractDbPath should extract unquoted path`() {
        val connectionString = "File=/absolute/path/to/db;Ref=base;"
        val basePath = Paths.get("/project")
        val result = ConnectionStringUtils.extractDbPath(connectionString, basePath)
        assertEquals("/absolute/path/to/db", result)
    }

    @Test
    fun `extractDbPath should extract single quoted path`() {
        val connectionString = "File='/quoted/path';Ref=base;"
        val basePath = Paths.get("/project")
        val result = ConnectionStringUtils.extractDbPath(connectionString, basePath)
        assertEquals("/quoted/path", result)
    }

    @Test
    fun `extractDbPath should extract double quoted path`() {
        val connectionString = "File=\"/double/quoted/path\";Ref=base;"
        val basePath = Paths.get("/project")
        val result = ConnectionStringUtils.extractDbPath(connectionString, basePath)
        assertEquals("/double/quoted/path", result)
    }

    @Test
    fun `extractDbPath should resolve relative path`() {
        val connectionString = "File=relative/db;Ref=base;"
        val basePath = Paths.get("/project/base")
        val result = ConnectionStringUtils.extractDbPath(connectionString, basePath)
        assertTrue(result.startsWith("/project/base/relative/db"))
    }

    @Test
    fun `extractDbPath should handle Windows drive letter`() {
        val connectionString = "File=C:\\windows\\path;Ref=base;"
        val basePath = Paths.get("C:\\project")
        val result = ConnectionStringUtils.extractDbPath(connectionString, basePath)
        assertEquals("C:\\windows\\path", result)
    }

    @Test
    fun `normalizeForCli should return original for non-File`() {
        val connectionString = "Srvr=server;Ref=base;"
        val basePath = Paths.get("/project")
        val result = ConnectionStringUtils.normalizeForCli(connectionString, basePath)
        assertEquals(connectionString, result)
    }

    @Test
    fun `normalizeForCli should format without quotes if no spaces`() {
        val connectionString = "File=/path/no spaces;"
        val basePath = Paths.get("/project")
        val result = ConnectionStringUtils.normalizeForCli(connectionString, basePath)
        assertEquals("File=/path/no spaces", result)
    }

    @Test
    fun `normalizeForCli should add double quotes for paths with spaces`() {
        val connectionString = "File=/path with spaces;"
        val basePath = Paths.get("/project")
        val result = ConnectionStringUtils.normalizeForCli(connectionString, basePath)
        assertEquals("File=\"/path with spaces\"", result)
    }

    @Test
    fun `normalizeForCli should add quotes for paths with semicolons`() {
        val connectionString = "File=/path;with;semicolon;"
        val basePath = Paths.get("/project")
        val result = ConnectionStringUtils.normalizeForCli(connectionString, basePath)
        assertEquals("File=\"/path;with;semicolon\"", result)
    }

    @Test
    fun `normalizeForCli should resolve relative and format`() {
        val connectionString = "File=relative/with space;"
        val basePath = Paths.get("/project")
        val result = ConnectionStringUtils.normalizeForCli(connectionString, basePath)
        val expectedPath = Paths.get("/project/relative/with space").toAbsolutePath().toString()
        assertEquals("File=\"$expectedPath\"", result)
    }
}
