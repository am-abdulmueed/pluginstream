package com.lagradost.cloudstream3.ui.game

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentGameBinding
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.utils.AppContextUtils.isNetworkAvailable
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import kotlinx.coroutines.launch

class GameFragment : BaseFragment<FragmentGameBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentGameBinding::inflate)
) {
    private var gameAdapter: GameAdapter? = null
    private lateinit var viewModel: GameViewModel
    private var currentSearchQuery: String = ""

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

        // Smoother add/remove/change animations for the grid (default ones feel abrupt)
        gamesRecyclerView.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 260
            removeDuration = 200
            changeDuration = 220
            moveDuration = 260
        }

        // Subtle glow on the search bar's border while the user is typing
        val defaultStrokeColor = 0x1AFFFFFF
        val focusedStrokeColor = requireContext().getColor(R.color.colorPrimary)
        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            val fromColor = if (hasFocus) defaultStrokeColor else focusedStrokeColor
            val toColor = if (hasFocus) focusedStrokeColor else defaultStrokeColor
            ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
                duration = 200
                addUpdateListener { animator ->
                    binding.searchCard.strokeColor = animator.animatedValue as Int
                }
                start()
            }
        }

        // 1. Offers Icon Click
        btnOffers.setOnClickListener {
            findNavController().navigate(R.id.navigation_offers)
        }

        // Retry button click
        retryButton.setOnClickListener {
            if (requireContext().isNetworkAvailable()) {
                hideOfflineScreen(binding)
                viewModel.fetchGamesIfNeeded(requireContext())
            } else {
                // Stay on offline screen
            }
        }

        // Saved Games Card Click
        binding.savedGamesCard.setOnClickListener {
            findNavController().navigate(R.id.action_game_to_saved_games)
        }

        // Observe Saved Games to update shortcut card
        viewModel.savedGames.observe(viewLifecycleOwner) { savedGames ->
            val count = savedGames.size
            if (count > 0) {
                binding.savedGamesCount.text = count.toString()
                if (binding.savedGamesCount.visibility != View.VISIBLE) {
                    binding.savedGamesCount.visibility = View.VISIBLE
                    binding.savedGamesCount.startAnimation(
                        AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
                    )
                }
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

                val wasVisible = gamesRecyclerView.visibility == View.VISIBLE
                gamesRecyclerView.visibility = View.VISIBLE
                val oldSize = gameAdapter?.itemCount ?: 0
                // Filter games using current search query before updating adapter
                val filteredGames = viewModel.filterGames(currentSearchQuery)
                gameAdapter?.updateList(filteredGames)
                emptyTextView.visibility = View.GONE
                hideOfflineScreen(binding)

                // Play the staggered grid entrance animation only the first time the grid appears
                if (!wasVisible) {
                    gamesRecyclerView.scheduleLayoutAnimation()
                }

                // Restore scroll position only on first load (when old list was empty)
                if (oldSize == 0 && viewModel.scrollPosition > 0) {
                    gamesRecyclerView.scrollToPosition(viewModel.scrollPosition)
                    // Optionally use post to ensure layout is complete
                    gamesRecyclerView.post {
                        (gamesRecyclerView.layoutManager as? GridLayoutManager)?.scrollToPositionWithOffset(viewModel.scrollPosition, 0)
                    }
                }
            } else if (viewModel.isLoading.value == false) {
                emptyTextView.visibility = View.VISIBLE
                emptyTextView.alpha = 0f
                emptyTextView.text = "No games available"
                emptyTextView.animate().alpha(1f).setDuration(220).start()
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
                currentSearchQuery = s.toString()
                gameAdapter?.updateList(viewModel.filterGames(currentSearchQuery))
            }
        })

        // Fetch games only if needed
        viewModel.fetchGamesIfNeeded(requireContext())
    }

    private fun showOfflineScreen(binding: FragmentGameBinding) {
        binding.offlineScreen.alpha = 0f
        binding.offlineScreen.visibility = View.VISIBLE
        binding.offlineScreen.animate().alpha(1f).setDuration(260).start()
        binding.offlineShimmer.startShimmer()
    }

    private fun hideOfflineScreen(binding: FragmentGameBinding) {
        if (binding.offlineScreen.visibility != View.VISIBLE) return
        binding.offlineScreen.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.offlineScreen.visibility = View.GONE
                binding.offlineShimmer.stopShimmer()
            }
            .start()
    }
}
