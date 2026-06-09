package com.lagradost.cloudstream3.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _allGames = MutableLiveData<List<GameModel>>(emptyList())
    val allGames: LiveData<List<GameModel>> = _allGames

    private val _savedGames = MutableLiveData<List<GameModel>>(emptyList())
    val savedGames: LiveData<List<GameModel>> = _savedGames

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var isDataLoaded = false
    var scrollPosition: Int = 0
    
    companion object {
        private const val SAVED_GAMES_KEY = "saved_games_urls"
    }

    fun fetchGamesIfNeeded(context: Context) {
        if (isDataLoaded) return

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = GameCacheManager.fetchGamesWithCache(context)
                val savedUrls = getKey<Set<String>>(SAVED_GAMES_KEY) ?: emptySet()
                
                val games = response.hits.map { game ->
                    game.copy(isFavorite = savedUrls.contains(game.gameURL))
                }
                
                _allGames.value = games
                _savedGames.value = games.filter { it.isFavorite }
                isDataLoaded = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load games"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(game: GameModel) {
        val currentGames = _allGames.value ?: return
        val savedUrls = getKey<Set<String>>(SAVED_GAMES_KEY)?.toMutableSet() ?: mutableSetOf()
        
        val newIsFavorite = !game.isFavorite
        if (newIsFavorite) {
            savedUrls.add(game.gameURL)
        } else {
            savedUrls.remove(game.gameURL)
        }
        
        setKey(SAVED_GAMES_KEY, savedUrls)
        
        val updatedGames = currentGames.map { 
            if (it.gameURL == game.gameURL) it.copy(isFavorite = newIsFavorite) else it 
        }
        
        _allGames.value = updatedGames
        _savedGames.value = updatedGames.filter { it.isFavorite }
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
