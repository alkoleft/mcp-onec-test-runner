package io.github.alkoleft.mcp.infrastructure.platform.search

import java.nio.file.Path

interface VersionResolver {
    fun selectBest(
        candidates: List<Pair<Path, String?>>,
        requirement: String?,
    ): Path?
}

class DefaultVersionResolver : VersionResolver {
    override fun selectBest(
        candidates: List<Pair<Path, String?>>,
        requirement: String?,
    ): Path? {
        if (candidates.isEmpty()) return null
        val parsed =
            candidates.map { pair ->
                val path = pair.first
                val ver = pair.second?.let { v -> Version.parse(v) }
                path to ver
            }
        if (requirement == null || requirement.equals("latest", ignoreCase = true)) {
            val withVersion =
                parsed
                    .filter { it.second != null }
                    .map { it.first to it.second!! }
            if (withVersion.isNotEmpty()) {
                return withVersion.maxByOrNull { it.second }!!.first
            }
            return candidates.first().first
        }
        val reqVersion = Version.parse(requirement)
        if (reqVersion == null) {
            // Invalid requirement format -> signal no match
            return null
        }
        val isMask = reqVersion.parts.size < 4
        val withVersion =
            parsed
                .filter { it.second != null }
                .map { it.first to it.second!! }
        val filtered =
            if (isMask) {
                withVersion.filter { it.second.startsWith(reqVersion) }
            } else {
                withVersion.filter { it.second == reqVersion }
            }
        if (filtered.isNotEmpty()) {
            return filtered.maxByOrNull { it.second }!!.first
        }
        // No candidates satisfy the version requirement -> signal no match
        return null
    }
}
