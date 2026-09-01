package com.lagradost.cloudstream3.ui.game

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GameResponse(
    val title: String? = null,
    val total_count: Int? = null,
    val hits: List<GameModel>? = null,
    val games: List<GameModel>? = null
) {
    fun getGameList(): List<GameModel> {
        return games ?: hits ?: emptyList()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GameModel(
    val id: String? = null,
    val slug: String? = null,
    val title: String = "",
    val description: String? = null,
    val howToPlayText: String? = null,
    val gameURL: String = "",
    val playgamaGameUrl: String? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val images: Any? = null,
    val videos: List<String> = emptyList(),
    val mobileReady: List<String> = emptyList(),
    val gender: List<String> = emptyList(),
    val inGamePurchases: String? = null,
    val supportedLanguages: List<String> = emptyList(),
    val screenOrientation: ScreenOrientation? = null,
    val embed: String? = null,
    val isFeatured: Boolean = false,
    var isFavorite: Boolean = false
) {
    /**
     * Safely extract game icon URL regardless of JSON format (List, Map, String, or GameImages)
     */
    fun getIconUrl(): String {
        return when (images) {
            is List<*> -> images.firstOrNull()?.toString() ?: ""
            is Map<*, *> -> (images["icon"] ?: images["poster"] ?: images["0"])?.toString() ?: ""
            is GameImages -> images.icon.ifBlank { images.poster }
            is String -> images
            else -> ""
        }
    }

    /**
     * Get effective URL to play the game
     */
    fun getPlayUrl(): String {
        if (gameURL.isNotBlank()) return gameURL
        if (!playgamaGameUrl.isNullOrBlank()) return playgamaGameUrl
        if (!embed.isNullOrBlank()) {
            val srcMatch = Regex("""src=['"]([^'"]+)['"]""").find(embed)
            if (srcMatch != null) {
                return srcMatch.groupValues[1]
            }
        }
        return ""
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScreenOrientation(
    val horizontal: Boolean? = true,
    val vertical: Boolean? = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GameImages(
    val icon: String = "",
    val poster: String = ""
)
