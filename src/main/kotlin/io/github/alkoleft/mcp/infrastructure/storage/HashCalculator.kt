package io.github.alkoleft.mcp.infrastructure.storage

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

private val logger = KotlinLogging.logger { }

/**
 * Calculates SHA-256 hash of file content with optimized buffering
 */
suspend fun calculateFileHash(file: Path): String =
    withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192) // 8KB buffer for optimal I/O performance

            Files.newInputStream(file).use { inputStream ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.debug(e) { "Failed to calculate hash for file: $file" }
            throw e
        }
    }

fun calculateStringHash(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(value.toByteArray(Charsets.UTF_8))
    return bytes.fold("") { str, it -> str + "%02x".format(it) }
}
