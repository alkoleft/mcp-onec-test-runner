/*
 * This file is part of METR.
 *
 * Copyright (C) 2025 Aleksey Koryakin <alkoleft@gmail.com> and contributors.
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

package io.github.alkoleft.mcp.infrastructure.storage

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

private val logger = KotlinLogging.logger { }

/**
 * Calculates SHA-256 hash of file content with optimized buffering
 */
fun calculateFileHash(file: Path): String {
    try {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192) // 8KB buffer for optimal I/O performance

        Files.newInputStream(file).use { inputStream ->
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
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
