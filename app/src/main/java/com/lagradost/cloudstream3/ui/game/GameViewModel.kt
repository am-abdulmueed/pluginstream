package com.lagradost.cloudstream3.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _allGames = MutableLiveData<List<GameModel>>(emptyList())
    val allGames: LiveData<List<GameModel>> = _allGames

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var isDataLoaded = false

    fun fetchGamesIfNeeded(context: Context) {
        if (isDataLoaded) return

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = GameCacheManager.fetchGamesWithCache(context)
                _allGames.value = response.hits
                isDataLoaded = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load games"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun filterGames(query: String): List<GameModel> {
        val currentGames = _allGames.value ?: emptyList()
        if (query.isBlank()) return currentGames
        
        return currentGames.filter { game ->
            game.title.contains(query, ignoreCase = true) ||
            game.genres.any { it.contains(query, ignoreCase = true) }
        }
    }
}
