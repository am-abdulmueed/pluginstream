package com.lagradost.cloudstream3.ui.game

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.NavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage

object GameDetailDialogHelper {
    fun showGameDetailsDialog(
        context: Context,
        game: GameModel,
        viewModel: GameViewModel,
        navController: NavController
    ) {
        val dialog = BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_game_detail, null)
        dialog.setContentView(dialogView)

        val dialogGameIcon = dialogView.findViewById<ImageView>(R.id.dialogGameIcon)
        val dialogGameTitle = dialogView.findViewById<TextView>(R.id.dialogGameTitle)
        val dialogBtnPlayGame = dialogView.findViewById<MaterialButton>(R.id.dialogBtnPlayGame)
        val dialogFavoriteButton = dialogView.findViewById<ImageView>(R.id.dialogFavoriteButton)
        val dialogCloseButton = dialogView.findViewById<ImageView>(R.id.dialogCloseButton)

        val dialogLayoutDescription = dialogView.findViewById<View>(R.id.dialogLayoutDescription)
        val dialogGameDescription = dialogView.findViewById<TextView>(R.id.dialogGameDescription)

        val dialogLayoutHowToPlay = dialogView.findViewById<View>(R.id.dialogLayoutHowToPlay)
        val dialogGameHowToPlay = dialogView.findViewById<TextView>(R.id.dialogGameHowToPlay)

        // Load Game Icon
        dialogGameIcon?.loadImage(game.getIconUrl())

        // Set Title
        dialogGameTitle?.text = game.title

        // Favorite Toggle — track local state since ViewModel no longer mutates game in-place
        var isFavorite = game.isFavorite

        fun updateFavIcon() {
            if (isFavorite) {
                dialogFavoriteButton?.setImageResource(R.drawable.ic_saved)
                dialogFavoriteButton?.colorFilter = null
            } else {
                dialogFavoriteButton?.setImageResource(R.drawable.ic_baseline_bookmark_border_24)
                dialogFavoriteButton?.clearColorFilter()
            }
        }
        updateFavIcon()

        dialogFavoriteButton?.setOnClickListener {
            viewModel.toggleFavorite(game)
            isFavorite = !isFavorite   // flip local state
            updateFavIcon()            // instant icon refresh
        }

        dialogCloseButton?.setOnClickListener {
            dialog.dismiss()
        }

        // Description
        if (!game.description.isNullOrBlank()) {
            dialogLayoutDescription?.visibility = View.VISIBLE
            dialogGameDescription?.text = game.description
        } else {
            dialogLayoutDescription?.visibility = View.GONE
        }

        // How To Play
        if (!game.howToPlayText.isNullOrBlank()) {
            dialogLayoutHowToPlay?.visibility = View.VISIBLE
            dialogGameHowToPlay?.text = game.howToPlayText
        } else {
            dialogLayoutHowToPlay?.visibility = View.GONE
        }

        val hasInGamePurchases = game.inGamePurchases?.trim()?.equals("Yes", ignoreCase = true) == true

        // Dynamic Play Button Styling based on inGamePurchases ("Yes" vs "No")
        if (hasInGamePurchases) {
            dialogBtnPlayGame?.text = "WATCH AD & PLAY"
            dialogBtnPlayGame?.setIconResource(R.drawable.ic_baseline_lock_24)
        } else {
            dialogBtnPlayGame?.text = "PLAY GAME NOW"
            dialogBtnPlayGame?.setIconResource(R.drawable.ic_baseline_play_arrow_24)
        }

        // Play Game Click
        dialogBtnPlayGame?.setOnClickListener {
            dialog.dismiss()
            val playUrl = game.getPlayUrl()
            val bundle = Bundle().apply {
                putString("game_url", playUrl)
                putString("game_title", game.title)
            }
            navController.navigate(R.id.navigation_game_player, bundle)
        }

        dialog.show()
    }
}
