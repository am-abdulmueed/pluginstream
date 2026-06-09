package com.lagradost.cloudstream3.ui.game

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentSavedGamesBinding
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding

class SavedGamesFragment : BaseFragment<FragmentSavedGamesBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentSavedGamesBinding::inflate)
) {
    private lateinit var viewModel: GameViewModel
    private var gameAdapter: GameAdapter? = null

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onBindingCreated(binding: FragmentSavedGamesBinding) {
        viewModel = ViewModelProvider(requireActivity())[GameViewModel::class.java]

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val spanCount = if (isLayout(TV or EMULATOR)) 4 else 2
        
        gameAdapter = GameAdapter({ game ->
            val bundle = Bundle().apply {
                putString("game_url", game.gameURL)
                putString("game_title", game.title)
            }
            findNavController().navigate(R.id.navigation_game_player, bundle)
        }, { game ->
            viewModel.toggleFavorite(game)
        }, forceNormal = true)

        binding.savedGamesRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), spanCount)
            adapter = gameAdapter
        }

        viewModel.savedGames.observe(viewLifecycleOwner) { games ->
            if (games.isEmpty()) {
                binding.emptySavedLayout.visibility = View.VISIBLE
                binding.savedGamesRecyclerView.visibility = View.GONE
            } else {
                binding.emptySavedLayout.visibility = View.GONE
                binding.savedGamesRecyclerView.visibility = View.VISIBLE
                gameAdapter?.updateList(games)
            }
        }
    }
}
