package com.lagradost.cloudstream3.ui.game

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentGameBinding
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import kotlinx.coroutines.launch

class GameFragment : BaseFragment<FragmentGameBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentGameBinding::inflate)
) {
    private lateinit var gameAdapter: GameAdapter
    private var allGames: List<GameModel> = emptyList()
    private var isDataLoaded = false // Track if data is already loaded

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onBindingCreated(binding: FragmentGameBinding) {
        val searchEditText = binding.root.findViewById<EditText>(R.id.searchEditText)
        val progressBar = binding.root.findViewById<ProgressBar>(R.id.progressBar)
        val emptyTextView = binding.root.findViewById<TextView>(R.id.emptyTextView)
        val gamesRecyclerView = binding.root.findViewById<RecyclerView>(R.id.gamesRecyclerView)
        val shimmerLayout = binding.root.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerLayout)

        // Setup RecyclerView with GridLayoutManager (2 columns for mobile)
        val spanCount = 2
        gameAdapter = GameAdapter { game ->
            // Navigate to GamePlayerFragment using Bundle
            val bundle = android.os.Bundle().apply {
                putString("game_url", game.gameURL)
                putString("game_title", game.title)
            }
            findNavController().navigate(R.id.navigation_game_player, bundle)
        }

        gamesRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    // Featured games span 2 columns (full width), normal games span 1 column
                    return if (allGames.getOrNull(position)?.isFeatured == true) 2 else 1
                }
            }
        }
        gamesRecyclerView.adapter = gameAdapter

        // Search functionality
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterGames(s.toString())
            }
        })

        // Fetch games from cache or GitHub (only once per app session)
        if (!isDataLoaded) {
            fetchGames(progressBar, emptyTextView, gamesRecyclerView, shimmerLayout)
        } else {
            // Data already loaded, just show it
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            gamesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun fetchGames(
        progressBar: ProgressBar,
        emptyTextView: TextView,
        gamesRecyclerView: RecyclerView,
        shimmerLayout: com.facebook.shimmer.ShimmerFrameLayout
    ) {
        // Show shimmer loading
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.startShimmer()
        progressBar.visibility = View.GONE
        emptyTextView.visibility = View.GONE
        gamesRecyclerView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Use smart caching - only downloads if GitHub file is updated
                val response = GameCacheManager.fetchGamesWithCache(requireContext())
                allGames = response.hits
                gameAdapter.updateList(allGames)
                isDataLoaded = true
                
                // Hide shimmer, show games
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                gamesRecyclerView.visibility = View.VISIBLE
                
                if (allGames.isEmpty()) {
                    emptyTextView.visibility = View.VISIBLE
                    emptyTextView.text = "No games available"
                }
            } catch (e: Exception) {
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                emptyTextView.visibility = View.VISIBLE
                emptyTextView.text = "Failed to load games: ${e.message}"
            }
        }
    }

    private fun filterGames(query: String) {
        if (query.isBlank()) {
            gameAdapter.updateList(allGames)
        } else {
            val filtered = allGames.filter { game ->
                game.title.contains(query, ignoreCase = true) ||
                game.genres.any { it.contains(query, ignoreCase = true) }
            }
            // Maintain featured status in filtered results
            gameAdapter.updateList(filtered)
        }
    }
}