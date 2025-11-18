package com.numina.services

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class CacheService(
    private val redisUrl: String = System.getenv("REDIS_URL") ?: "redis://localhost:6379"
) {
    private val logger = LoggerFactory.getLogger(CacheService::class.java)
    private val redisClient: RedisClient = RedisClient.create(redisUrl)
    private val connection: StatefulRedisConnection<String, String> = redisClient.connect()
    private val syncCommands: RedisCommands<String, String> = connection.sync()

    // Cache TTL constants
    companion object {
        val USER_PROFILE_TTL = 30.minutes
        val CLASS_LISTING_TTL = 15.minutes
        val MATCH_RESULTS_TTL = 1.hours
        val FEED_DATA_TTL = 5.minutes
        val RATING_AGGREGATES_TTL = 1.hours
    }

    /**
     * Get a value from cache
     */
    suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        try {
            val value = syncCommands.get(key)
            if (value != null) {
                logger.debug("Cache HIT for key: $key")
            } else {
                logger.debug("Cache MISS for key: $key")
            }
            value
        } catch (e: Exception) {
            logger.error("Error getting from cache: ${e.message}", e)
            null
        }
    }

    /**
     * Get and deserialize a value from cache
     */
    suspend inline fun <reified T> getObject(key: String): T? = withContext(Dispatchers.IO) {
        try {
            val json = get(key) ?: return@withContext null
            Json.decodeFromString<T>(json)
        } catch (e: Exception) {
            logger.error("Error deserializing cache value for key $key: ${e.message}", e)
            null
        }
    }

    /**
     * Set a value in cache with TTL
     */
    suspend fun set(key: String, value: String, ttl: Duration): Boolean = withContext(Dispatchers.IO) {
        try {
            syncCommands.setex(key, ttl.inWholeSeconds, value)
            logger.debug("Cache SET for key: $key (TTL: ${ttl.inWholeSeconds}s)")
            true
        } catch (e: Exception) {
            logger.error("Error setting cache: ${e.message}", e)
            false
        }
    }

    /**
     * Set and serialize an object in cache with TTL
     */
    suspend inline fun <reified T> setObject(key: String, value: T, ttl: Duration): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = Json.encodeToString(value)
            set(key, json, ttl)
        } catch (e: Exception) {
            logger.error("Error serializing cache value for key $key: ${e.message}", e)
            false
        }
    }

    /**
     * Invalidate (delete) a cache key
     */
    suspend fun invalidate(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val deleted = syncCommands.del(key)
            logger.debug("Cache INVALIDATE for key: $key (deleted: $deleted)")
            deleted > 0
        } catch (e: Exception) {
            logger.error("Error invalidating cache: ${e.message}", e)
            false
        }
    }

    /**
     * Invalidate multiple cache keys by pattern
     */
    suspend fun invalidatePattern(pattern: String): Int = withContext(Dispatchers.IO) {
        try {
            val keys = syncCommands.keys(pattern)
            if (keys.isNotEmpty()) {
                val deleted = syncCommands.del(*keys.toTypedArray())
                logger.debug("Cache INVALIDATE PATTERN: $pattern (deleted: $deleted keys)")
                deleted.toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            logger.error("Error invalidating cache pattern: ${e.message}", e)
            0
        }
    }

    /**
     * Check if a key exists in cache
     */
    suspend fun exists(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            syncCommands.exists(key) > 0
        } catch (e: Exception) {
            logger.error("Error checking cache existence: ${e.message}", e)
            false
        }
    }

    /**
     * Get or set pattern - retrieve from cache or compute and cache
     */
    suspend fun <T> getOrSet(
        key: String,
        ttl: Duration,
        provider: suspend () -> T
    ): T where T : Any {
        val cached = get(key)
        if (cached != null) {
            @Suppress("UNCHECKED_CAST")
            return cached as T
        }

        val value = provider()
        set(key, value.toString(), ttl)
        return value
    }

    /**
     * Close connections
     */
    fun close() {
        connection.close()
        redisClient.shutdown()
    }
}
