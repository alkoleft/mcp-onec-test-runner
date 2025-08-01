package io.github.alkoleft.mcp.infrastructure.storage

import io.github.alkoleft.mcp.core.modules.HashStorage
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentMap

/**
 * MapDB-based persistent storage for file hashes.
 * Implements thread-safe operations with atomic transactions and efficient batch updates.
 */
private val logger = KotlinLogging.logger {  }

@Component
class MapDbHashStorage : HashStorage {
    private val mutex = Mutex()

    private lateinit var db: DB
    private lateinit var hashMap: ConcurrentMap<String, String>
    private lateinit var timestampMap: ConcurrentMap<String, Long>

    private val dbPath = Paths.get(".yaxunit", "file-hashes.db")

    @PostConstruct
    private fun initialize() {
        logger.info { "Initializing MapDB hash storage at: $dbPath" }

        try {
            // Ensure directory exists
            Files.createDirectories(dbPath.parent)

            // Initialize MapDB with optimized settings
            db =
                DBMaker
                    .fileDB(dbPath.toFile())
                    .transactionEnable()
                    .closeOnJvmShutdown()
                    .fileMmapEnable()
                    .make()

            // Create hash map for file content hashes
            hashMap =
                db
                    .hashMap("file_hashes")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .createOrOpen()

            // Create timestamp map for file modification times
            timestampMap =
                db
                    .hashMap("file_timestamps")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.LONG)
                    .createOrOpen()

            logger.info { "MapDB hash storage initialized with ${hashMap.size} existing file hashes" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize MapDB hash storage" }
            throw RuntimeException("Hash storage initialization failed", e)
        }
    }

    override suspend fun getHash(file: Path): String? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val key = normalizeKey(file)
                    hashMap[key]
                } catch (e: Exception) {
                    logger.debug(e) { "Failed to get hash for file: $file" }
                    null
                }
            }
        }

    override suspend fun storeHash(
        file: Path,
        hash: String,
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val key = normalizeKey(file)
                val timestamp = Files.getLastModifiedTime(file).toMillis()

                hashMap[key] = hash
                timestampMap[key] = timestamp

                db.commit()

                logger.debug { "Stored hash for file: $file" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to store hash for file: $file" }
                db.rollback()
                throw e
            }
        }
    }

    override suspend fun batchUpdate(updates: Map<Path, String>) =
        withContext(Dispatchers.IO) {
            if (updates.isEmpty()) return@withContext

            mutex.withLock {
                try {
                    logger.debug { "Starting batch update for ${updates.size} files" }

                    for ((file, hash) in updates) {
                        val key = normalizeKey(file)
                        val timestamp =
                            if (Files.exists(file)) {
                                Files.getLastModifiedTime(file).toMillis()
                            } else {
                                System.currentTimeMillis()
                            }

                        hashMap[key] = hash
                        timestampMap[key] = timestamp
                    }

                    db.commit()

                    logger.info { "Batch updated ${updates.size} file hashes" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to batch update file hashes" }
                    db.rollback()
                    throw e
                }
            }
        }

    override suspend fun removeHash(file: Path) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val key = normalizeKey(file)

                    hashMap.remove(key)
                    timestampMap.remove(key)

                    db.commit()

                    logger.debug { "Removed hash for file: $file" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to remove hash for file: $file" }
                    db.rollback()
                    throw e
                }
            }
        }

    override suspend fun getAllHashes(): Map<String, String> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    HashMap(hashMap)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to get all hashes" }
                    emptyMap()
                }
            }
        }

    /**
     * Gets the stored timestamp for a file
     */
    suspend fun getTimestamp(file: Path): Long? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val key = normalizeKey(file)
                    timestampMap[key]
                } catch (e: Exception) {
                    logger.debug(e) { "Failed to get timestamp for file: $file" }
                    null
                }
            }
        }

    /**
     * Stores the timestamp for a file
     */
    suspend fun storeTimestamp(
        file: Path,
        timestamp: Long,
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val key = normalizeKey(file)
                timestampMap[key] = timestamp
                db.commit()

                logger.debug { "Stored timestamp for file: $file" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to store timestamp for file: $file" }
                db.rollback()
                throw e
            }
        }
    }

    /**
     * Gets statistics about the hash storage
     */
    suspend fun getStorageStats(): HashStorageStats =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    HashStorageStats(
                        totalFiles = hashMap.size,
                        dbSizeBytes = Files.size(dbPath),
                        oldestTimestamp = timestampMap.values.minOrNull(),
                        newestTimestamp = timestampMap.values.maxOrNull(),
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Failed to get storage stats" }
                    HashStorageStats(0, 0, null, null)
                }
            }
        }

    /**
     * Performs cleanup of old entries beyond the specified retention period
     */
    suspend fun cleanup(retentionDays: Int = 30) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
                    val keysToRemove = mutableListOf<String>()

                    for ((key, timestamp) in timestampMap) {
                        if (timestamp < cutoffTime) {
                            keysToRemove.add(key)
                        }
                    }

                    for (key in keysToRemove) {
                        hashMap.remove(key)
                        timestampMap.remove(key)
                    }

                    db.commit()

                    logger.info { "Cleaned up ${keysToRemove.size} old hash entries (retention: $retentionDays days)" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to cleanup old hash entries" }
                    db.rollback()
                }
            }
        }

    /**
     * Normalizes file path to a consistent string key
     */
    private fun normalizeKey(file: Path): String = file.toAbsolutePath().normalize().toString()

    override suspend fun close() =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    logger.info { "Closing MapDB hash storage" }

                    if (::db.isInitialized && !db.isClosed()) {
                        db.commit()
                        db.close()
                    }

                    logger.info { "MapDB hash storage closed successfully" }
                } catch (e: Exception) {
                    logger.error(e) { "Error closing MapDB hash storage" }
                }
            }
        }

    @PreDestroy
    private fun destroy() {
        runBlocking {
            close()
        }
    }
}

/**
 * Statistics about the hash storage
 */
data class HashStorageStats(
    val totalFiles: Int,
    val dbSizeBytes: Long,
    val oldestTimestamp: Long?,
    val newestTimestamp: Long?,
)
