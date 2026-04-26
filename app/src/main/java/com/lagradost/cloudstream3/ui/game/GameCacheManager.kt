package com.lagradost.cloudstream3.ui.game

import android.content.Context
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ignoreAllSSLErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/**
 * Smart cache manager for games data
 * - Caches games JSON locally
 * - Only re-downloads when GitHub file is updated (checks commit hash)
 * - Works offline with cached data
 */
object GameCacheManager {
    private const val PREFS_NAME = "games_cache_prefs"
    private const val KEY_CACHED_GAMES = "cached_games_json"
    private const val KEY_LAST_COMMIT = "last_commit_hash"
    private const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
    
    private const val GAMES_JSON_URL = "https://raw.githubusercontent.com/am-abdulmueed/PluginStream-Games/main/games_final_lite.json"
    private const val GITHUB_API_URL = "https://api.github.com/repos/am-abdulmueed/PluginStream-Games/commits?path=games_final_lite.json&per_page=1"
    
    private val objectMapper = jacksonObjectMapper()
    private val client = Requests().apply {
        baseClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .ignoreAllSSLErrors()
            .build()
    }
    
    /**
     * Fetch games with smart caching
     * Returns cached data if available and no new updates
     */
    suspend fun fetchGamesWithCache(context: Context): GameResponse {
        return withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            try {
                // Check if cache exists
                val hasCache = prefs.contains(KEY_CACHED_GAMES)
                
                if (hasCache) {
                    // Cache exists, check for updates
                    val hasUpdate = checkForUpdates(prefs)
                    
                    if (hasUpdate) {
                        // Download new games data
                        try {
                            downloadAndCacheGames(context, prefs)
                        } catch (e: Exception) {
                            // If download fails, use cached data
                            android.util.Log.w("GameCacheManager", "Failed to update cache, using old data: ${e.message}")
                        }
                    }
                    
                    // Return cached games
                    getCachedGames(context) ?: throw Exception("Failed to load cached games")
                } else {
                    // No cache, download from GitHub
                    downloadAndCacheGames(context, prefs)
                    getCachedGames(context) ?: throw Exception("Failed to load downloaded games")
                }
            } catch (e: Exception) {
                // If everything fails, throw exception
                throw Exception("Failed to load games: ${e.message}")
            }
        }
    }
    
    /**
     * Check if there's a new commit on GitHub
     */
    private suspend fun checkForUpdates(prefs: android.content.SharedPreferences): Boolean {
        return try {
            val response = client.get(GITHUB_API_URL)
            if (response.code in 200..299) {
                val json = response.text
                val commits = objectMapper.readValue<List<GitHubCommit>>(json)
                
                if (commits.isNotEmpty()) {
                    val latestCommit = commits[0].sha
                    val lastCommit = prefs.getString(KEY_LAST_COMMIT, null)
                    
                    // If no cached commit or different commit, there's an update
                    if (lastCommit == null || lastCommit != latestCommit) {
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            // If can't check for updates, use cached data
            false
        }
    }
    
    /**
     * Download games and cache it
     */
    private suspend fun downloadAndCacheGames(
        context: Context,
        prefs: android.content.SharedPreferences
    ) {
        try {
            android.util.Log.d("GameCacheManager", "Downloading games from GitHub...")
            
            // Download games JSON
            val response = client.get(GAMES_JSON_URL)
            if (response.code in 200..299) {
                val gamesJson = response.text
                android.util.Log.d("GameCacheManager", "Successfully downloaded games JSON (${gamesJson.length} bytes)")
                
                // Get latest commit hash (optional, don't fail if this fails)
                var latestCommit = ""
                try {
                    val commitResponse = client.get(GITHUB_API_URL)
                    if (commitResponse.code in 200..299) {
                        val commits = objectMapper.readValue<List<GitHubCommit>>(commitResponse.text)
                        if (commits.isNotEmpty()) {
                            latestCommit = commits[0].sha
                            android.util.Log.d("GameCacheManager", "Latest commit: $latestCommit")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("GameCacheManager", "Failed to get commit info: ${e.message}")
                }
                
                // Cache the data
                prefs.edit().apply {
                    putString(KEY_CACHED_GAMES, gamesJson)
                    putString(KEY_LAST_COMMIT, latestCommit)
                    putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
                    apply()
                    android.util.Log.d("GameCacheManager", "Games cached successfully")
                }
            } else {
                throw Exception("HTTP ${response.code}: Failed to download games")
            }
        } catch (e: Exception) {
            android.util.Log.e("GameCacheManager", "Download failed: ${e.message}")
            throw Exception("Failed to download games: ${e.message}")
        }
    }
    
    /**
     * Get cached games from local storage
     */
    private fun getCachedGames(context: Context): GameResponse? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cachedJson = prefs.getString(KEY_CACHED_GAMES, null)
            
            if (cachedJson != null) {
                val gameResponse = objectMapper.readValue<GameResponse>(cachedJson)
                
                // Set featured games: Premium pattern (4 normal + 1 poster = every 5th game)
                val featuredGames = gameResponse.hits.mapIndexed { index: Int, game: GameModel ->
                    game.copy(isFeatured = (index + 1) % 5 == 0)
                }
                
                gameResponse.copy(hits = featuredGames)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Clear cache (force refresh)
     */
    fun clearCache(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
    
    /**
     * Check if cache exists
     */
    fun hasCache(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_CACHED_GAMES)
    }
    
    /**
     * Get cache info
     */
    fun getCacheInfo(context: Context): CacheInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0)
        val lastCommit = prefs.getString(KEY_LAST_COMMIT, "Unknown") ?: "Unknown"
        val hasData = prefs.contains(KEY_CACHED_GAMES)
        
        return CacheInfo(
            hasCache = hasData,
            lastUpdateTimestamp = timestamp,
            lastCommitHash = lastCommit
        )
    }
    
    data class CacheInfo(
        val hasCache: Boolean,
        val lastUpdateTimestamp: Long,
        val lastCommitHash: String
    )
}

/**
 * GitHub commit response model
 */
data class GitHubCommit(
    val sha: String,
    val commit: CommitInfo? = null
)

data class CommitInfo(
    val message: String? = null,
    val author: CommitAuthor? = null
)

data class CommitAuthor(
    val date: String? = null
)
