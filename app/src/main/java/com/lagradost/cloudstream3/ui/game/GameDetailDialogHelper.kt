package com.lagradost.cloudstream3.ui.game

import android.app.Activity
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
        // val dialogCloseButton = dialogView.findViewById<ImageView>(R.id.dialogCloseButton)

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

        // dialogCloseButton?.setOnClickListener {
        //     dialog.dismiss()
        // }

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

        val isLocked = GameRewardedAdManager.isGameLocked(game)

        // Dynamic Play Button Styling based on lock / unlock state
        if (isLocked) {
            dialogBtnPlayGame?.text = "WATCH AD & PLAY"
            dialogBtnPlayGame?.setIconResource(R.drawable.ic_baseline_lock_24)
        } else {
            dialogBtnPlayGame?.text = "PLAY GAME NOW"
            dialogBtnPlayGame?.setIconResource(R.drawable.ic_baseline_play_arrow_24)
        }

        val activity = (context as? Activity) ?: context.getActivity() ?: com.lagradost.cloudstream3.CommonActivity.activity

        fun launchGamePlayer() {
            val runAction = Runnable {
                try {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    val playUrl = game.getPlayUrl()
                    val bundle = Bundle().apply {
                        putString("game_url", playUrl)
                        putString("game_title", game.title)
                    }
                    navController.navigate(R.id.navigation_game_player, bundle)
                } catch (e: Exception) {
                    android.util.Log.e("GameDetailDialog", "Failed to navigate to game player", e)
                }
            }

            if (activity != null) {
                activity.runOnUiThread(runAction)
            } else {
                runAction.run()
            }
        }

        // Play Game Click
        dialogBtnPlayGame?.setOnClickListener {
            if (GameRewardedAdManager.isGameLocked(game)) {
                if (activity != null) {
                    dialog.dismiss()
                    GameRewardedAdManager.showRewardedAd(
                        activity,
                        game,
                        onRewardEarned = {
                            launchGamePlayer()
                        }
                    )
                } else {
                    launchGamePlayer()
                }
            } else {
                // Non-locked game: record play and show interstitial every 3 plays
                if (activity != null) {
                    GameInterstitialAdManager.recordPlayAndShowIfDue(activity)
                }
                launchGamePlayer()
            }
        }

        dialog.show()
    }

    private fun Context.getActivity(): android.app.Activity? {
        var cur: Context? = this
        while (cur is android.content.ContextWrapper) {
            if (cur is android.app.Activity) return cur
            cur = cur.baseContext
        }
        return null
    }
}
