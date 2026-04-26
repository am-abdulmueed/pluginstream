package com.lagradost.cloudstream3.ui.game

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ignoreAllSSLErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

object GameApiService {
    private const val GITHUB_RAW_URL = "https://raw.githubusercontent.com/am-abdulmueed/PluginStream-Games/main/games_final_lite.json"
    
    private val objectMapper = jacksonObjectMapper()
    private val client = Requests().apply {
        baseClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .ignoreAllSSLErrors()
            .build()
    }
    
    suspend fun fetchGames(): GameResponse = withContext(Dispatchers.IO) {
        val response = client.get(GITHUB_RAW_URL)
        if (response.code in 200..299) {
            val json = response.text
            val gameResponse = objectMapper.readValue<GameResponse>(json)
            
            // Set featured games: Premium pattern (4 normal + 1 poster = every 5th game)
            val featuredGames = gameResponse.hits.mapIndexed { index: Int, game: GameModel ->
                game.copy(isFeatured = (index + 1) % 5 == 0)
            }
            
            gameResponse.copy(hits = featuredGames)
        } else {
            throw Exception("Failed to fetch games: ${response.code}")
        }
    }
}
