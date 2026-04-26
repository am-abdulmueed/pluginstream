package com.lagradost.cloudstream3.ui.game

data class GameResponse(
    val title: String,
    val total_count: Int,
    val hits: List<GameModel>
)

data class GameModel(
    val title: String,
    val gameURL: String,
    val genres: List<String> = emptyList(),
    val images: GameImages,
    val isFeatured: Boolean = false // For large poster display
)

data class GameImages(
    val icon: String,
    val poster: String
)
