package com.lagradost.cloudstream3.ui.game

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
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
        val welcomeOverlay = binding.welcomeOverlay
        val welcomeIcon = binding.welcomeIcon
        val btnOffers = binding.btnGoToOffers

        // 1. Welcome NexGama Overlay Logic (Shows only once per app session)
        if (!hasShownSplashThisSession) {
            hasShownSplashThisSession = true
            welcomeOverlay.visibility = View.VISIBLE
            welcomeOverlay.alpha = 1f
            // Animate Icon pop-up
            welcomeIcon.scaleX = 0.5f
            welcomeIcon.scaleY = 0.5f
            welcomeIcon.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(400)
                .withEndAction {
                    welcomeIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                }
                .start()

            // Auto-hide after 1 second
            Handler(Looper.getMainLooper()).postDelayed({
                welcomeOverlay.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        welcomeOverlay.visibility = View.GONE
                    }
                    .start()
            }, 1000)
        } else {
            welcomeOverlay.visibility = View.GONE
        }

        // 2. Offers Icon Click
        btnOffers.setOnClickListener {
            findNavController().navigate(R.id.navigation_offers)
        }

        // Setup RecyclerView with GridLayoutManager (2 columns for mobile)
        val spanCount = 2
        
        // Use existing adapter if available to maintain state
        if (gameAdapter == null) {
            gameAdapter = GameAdapter { game ->
                val bundle = Bundle().apply {
                    putString("game_url", game.gameURL)
                    putString("game_title", game.title)
                }
                findNavController().navigate(R.id.navigation_game_player, bundle)
            }
            gameAdapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        gamesRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val games = viewModel.allGames.value ?: emptyList()
                    return if (games.getOrNull(position)?.isFeatured == true) 2 else 1
                }
            }
        }
        gamesRecyclerView.adapter = gameAdapter

        // Observe ViewModel data
        viewModel.allGames.observe(viewLifecycleOwner) { games ->
            if (games.isNotEmpty()) {
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                gamesRecyclerView.visibility = View.VISIBLE
                gameAdapter?.updateList(games)
                emptyTextView.visibility = View.GONE
            } else if (viewModel.isLoading.value == false) {
                emptyTextView.visibility = View.VISIBLE
                emptyTextView.text = "No games available"
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                shimmerLayout.visibility = View.VISIBLE
                shimmerLayout.startShimmer()
                gamesRecyclerView.visibility = View.GONE
                emptyTextView.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                emptyTextView.visibility = View.VISIBLE
                emptyTextView.text = "Failed to load games: $error"
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

    companion object {
        private var hasShownSplashThisSession = false
    }
}
