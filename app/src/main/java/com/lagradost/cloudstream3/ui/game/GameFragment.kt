package com.lagradost.cloudstream3.ui.game

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
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
    private var gameAdapter: GameAdapter? = null
    private lateinit var viewModel: GameViewModel

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onBindingCreated(binding: FragmentGameBinding) {
        viewModel = ViewModelProvider(requireActivity())[GameViewModel::class.java]
        
        val searchEditText = binding.searchEditText
        val progressBar = binding.progressBar
        val emptyTextView = binding.emptyTextView
        val gamesRecyclerView = binding.gamesRecyclerView
        val shimmerLayout = binding.shimmerLayout
        val btnOffers = binding.btnGoToOffers
        val offlineScreen = binding.offlineScreen
        val offlineShimmer = binding.offlineShimmer
        val retryButton = binding.retryButton

        // Setup RecyclerView with GridLayoutManager (Dynamic columns for responsiveness)
        val spanCount = if (isLayout(TV or EMULATOR)) 4 else 2
        
        // Use existing adapter if available to maintain state
        if (gameAdapter == null) {
            gameAdapter = GameAdapter({ game ->
                val bundle = Bundle().apply {
                    putString("game_url", game.gameURL)
                    putString("game_title", game.title)
                }
                findNavController().navigate(R.id.navigation_game_player, bundle)
            }, { game ->
                viewModel.toggleFavorite(game)
            })
            gameAdapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        gamesRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val games = viewModel.allGames.value ?: emptyList()
                    return if (games.getOrNull(position)?.isFeatured == true) spanCount else 1
                }
            }
        }
        gamesRecyclerView.adapter = gameAdapter

        // 1. Offers Icon Click
        btnOffers.setOnClickListener {
            findNavController().navigate(R.id.navigation_offers)
        }

        // Retry button click
        retryButton.setOnClickListener {
            hideOfflineScreen(binding)
            viewModel.fetchGamesIfNeeded(requireContext())
        }

        // Saved Games Card Click
        binding.savedGamesCard.setOnClickListener {
            val savedCount = viewModel.savedGames.value?.size ?: 0
            if (savedCount > 0) {
                findNavController().navigate(R.id.action_game_to_saved_games)
            } else {
                // Show Guide Dialog
                AlertDialog.Builder(requireContext(), R.style.AlertDialogResponsive)
                    .setTitle("How to Save Games")
                    .setMessage("Tap the 🔖 bookmark icon on any game to save it to your collection. Your saved games will appear here for quick access!")
                    .setPositiveButton("Got it", null)
                    .show()
            }
        }

        // Observe Saved Games to update shortcut card
        viewModel.savedGames.observe(viewLifecycleOwner) { savedGames ->
            val count = savedGames.size
            if (count > 0) {
                binding.savedGamesCount.text = count.toString()
                binding.savedGamesCount.visibility = View.VISIBLE
                binding.savedGamesSubtitle.text = "You have $count saved games"
                binding.savedGamesSubtitle.setTextColor(requireContext().getColor(R.color.colorPrimary))
            } else {
                binding.savedGamesCount.visibility = View.GONE
                binding.savedGamesSubtitle.text = "No games saved yet"
                binding.savedGamesSubtitle.setTextColor(requireContext().getColor(R.color.grayTextColor))
            }
        }

        // Observe ViewModel data
        viewModel.allGames.observe(viewLifecycleOwner) { games ->
            if (games.isNotEmpty()) {
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                gamesRecyclerView.visibility = View.VISIBLE
                gameAdapter?.updateList(games)
                emptyTextView.visibility = View.GONE
                hideOfflineScreen(binding)
                
                // Restore scroll position after data is loaded
                if (viewModel.scrollPosition > 0) {
                    gamesRecyclerView.scrollToPosition(viewModel.scrollPosition)
                    // Optionally use post to ensure layout is complete
                    gamesRecyclerView.post {
                        (gamesRecyclerView.layoutManager as? GridLayoutManager)?.scrollToPositionWithOffset(viewModel.scrollPosition, 0)
                    }
                }
            } else if (viewModel.isLoading.value == false) {
                emptyTextView.visibility = View.VISIBLE
                emptyTextView.text = "No games available"
            }
        }

        // Save scroll position when scrolling
        gamesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? GridLayoutManager
                val position = layoutManager?.findFirstVisibleItemPosition() ?: 0
                if (position >= 0) {
                    viewModel.scrollPosition = position
                }
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                shimmerLayout.visibility = View.VISIBLE
                shimmerLayout.startShimmer()
                gamesRecyclerView.visibility = View.GONE
                emptyTextView.visibility = View.GONE
                hideOfflineScreen(binding)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                gamesRecyclerView.visibility = View.GONE
                emptyTextView.visibility = View.GONE
                showOfflineScreen(binding)
            }
        }

        // Search functionality
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                gameAdapter?.updateList(viewModel.filterGames(s.toString()))
            }
        })

        // Fetch games only if needed
        viewModel.fetchGamesIfNeeded(requireContext())
    }

    private fun showOfflineScreen(binding: FragmentGameBinding) {
        binding.offlineScreen.visibility = View.VISIBLE
        binding.offlineShimmer.startShimmer()
    }

    private fun hideOfflineScreen(binding: FragmentGameBinding) {
        binding.offlineScreen.visibility = View.GONE
        binding.offlineShimmer.stopShimmer()
    }
}
