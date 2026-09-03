package com.lagradost.cloudstream3.ui.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage
import com.lagradost.cloudstream3.R

class GameAdapter(
    private val onGameClick: (GameModel) -> Unit,
    private val onFavoriteClick: (GameModel) -> Unit,
    private val forceNormal: Boolean = false,
    private val showFooter: Boolean = true
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private var games: List<GameModel> = emptyList()
    
    sealed class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class NormalViewHolder(view: View) : GameViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.gameImageView)
            val favoriteButton: ImageView = view.findViewById(R.id.favoriteButton)
            val lockBadge: ImageView? = view.findViewById(R.id.lockBadge)
            val loadingView: ShimmerFrameLayout? = view.findViewById(R.id.gameLoadingView)
            val titleView: TextView? = view.findViewById(R.id.gameTitleTextView)
        }
        
        class LargeViewHolder(view: View) : GameViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.gameImageView)
            val favoriteButton: ImageView = view.findViewById(R.id.favoriteButton)
            val lockBadge: ImageView? = view.findViewById(R.id.lockBadge)
            val loadingView: ShimmerFrameLayout? = view.findViewById(R.id.gameLoadingView)
            val titleView: TextView? = view.findViewById(R.id.gameTitleTextView)
        }

        class FooterViewHolder(view: View) : GameViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        if (showFooter && position == games.size) {
            return VIEW_TYPE_FOOTER
        }
        // Use isFeatured flag to determine if it should be a large poster
        // unless forceNormal is true
        return if (games[position].isFeatured && !forceNormal) {
            VIEW_TYPE_LARGE
        } else {
            VIEW_TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        return when (viewType) {
            VIEW_TYPE_FOOTER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_game_footer, parent, false)
                GameViewHolder.FooterViewHolder(view)
            }
            VIEW_TYPE_LARGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_game_large, parent, false)
                GameViewHolder.LargeViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_game_normal, parent, false)
                GameViewHolder.NormalViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int, payloads: MutableList<Any>) {
        if (holder is GameViewHolder.FooterViewHolder) return
        if (payloads.isNotEmpty() && payloads.contains("FAVORITE_CHANGED")) {
            // Only update the favorite button icon
            val game = games.getOrNull(position) ?: return
            val favButton = when (holder) {
                is GameViewHolder.NormalViewHolder -> holder.favoriteButton
                is GameViewHolder.LargeViewHolder -> holder.favoriteButton
                else -> return
            }
            if (game.isFavorite) {
                favButton.setImageResource(R.drawable.ic_saved)
                favButton.colorFilter = null // no tint on saved icon
            } else {
                favButton.setImageResource(R.drawable.ic_baseline_bookmark_border_24)
                favButton.setColorFilter(
                    android.graphics.Color.WHITE,
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            return
        }
        // If no payload, do full bind
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        if (holder is GameViewHolder.FooterViewHolder) return
        val game = games.getOrNull(position) ?: return
        
        val favButton = when (holder) {
            is GameViewHolder.NormalViewHolder -> holder.favoriteButton
            is GameViewHolder.LargeViewHolder -> holder.favoriteButton
            else -> return
        }

        val loadingView = when (holder) {
            is GameViewHolder.NormalViewHolder -> holder.loadingView
            is GameViewHolder.LargeViewHolder -> holder.loadingView
            else -> null
        }

        val imageView = when (holder) {
            is GameViewHolder.NormalViewHolder -> holder.imageView
            is GameViewHolder.LargeViewHolder -> holder.imageView
            else -> return
        }

        // Setup Favorite Icon — ic_saved.png (no tint) when saved, border icon (white tint) when not
        if (game.isFavorite) {
            favButton.setImageResource(R.drawable.ic_saved)
            favButton.colorFilter = null
        } else {
            favButton.setImageResource(R.drawable.ic_baseline_bookmark_border_24)
            favButton.setColorFilter(
                android.graphics.Color.WHITE,
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
        favButton.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION || pos >= games.size) return@setOnClickListener
            val nowFavorite = !games[pos].isFavorite  // flip: ViewModel won't mutate in-place
            onFavoriteClick(games[pos])
            // Immediately show new icon without waiting for DiffUtil
            if (nowFavorite) {
                favButton.setImageResource(R.drawable.ic_saved)
                favButton.colorFilter = null
            } else {
                favButton.setImageResource(R.drawable.ic_baseline_bookmark_border_24)
                favButton.setColorFilter(
                    android.graphics.Color.WHITE,
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }

        val lockBadge = when (holder) {
            is GameViewHolder.NormalViewHolder -> holder.lockBadge
            is GameViewHolder.LargeViewHolder -> holder.lockBadge
            else -> null
        }
        val isLocked = GameRewardedAdManager.isGameLocked(game)
        lockBadge?.visibility = if (isLocked) View.VISIBLE else View.GONE

        // Image & Shimmer
        loadingView?.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        imageView.loadImage(game.getIconUrl())

        when (holder) {
            is GameViewHolder.NormalViewHolder -> {
                holder.titleView?.text = game.title
                // Hide title as requested for search results style consistency
                holder.titleView?.visibility = View.GONE
                holder.itemView.setOnClickListener {
                    val pos = holder.bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION && pos < games.size) onGameClick(games[pos])
                }
                holder.itemView.setOnLongClickListener {
                    val pos = holder.bindingAdapterPosition
                    if (pos == RecyclerView.NO_POSITION || pos >= games.size) return@setOnLongClickListener false
                    vibrateDevice(it.context)
                    onGameClick(games[pos])
                    true
                }
            }
            is GameViewHolder.LargeViewHolder -> {
                holder.titleView?.text = game.title
                // Hide title as requested for search results style consistency
                holder.titleView?.visibility = View.GONE
                holder.itemView.setOnClickListener {
                    val pos = holder.bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION && pos < games.size) onGameClick(games[pos])
                }
                holder.itemView.setOnLongClickListener {
                    val pos = holder.bindingAdapterPosition
                    if (pos == RecyclerView.NO_POSITION || pos >= games.size) return@setOnLongClickListener false
                    vibrateDevice(it.context)
                    onGameClick(games[pos])
                    true
                }
            }
            is GameViewHolder.FooterViewHolder -> Unit
        }
    }

    override fun getItemCount(): Int = if (games.isEmpty()) 0 else if (showFooter) games.size + 1 else games.size

    override fun getItemId(position: Int): Long {
        if (position == games.size) return Long.MAX_VALUE - 100L
        val playUrl = games.getOrNull(position)?.getPlayUrl() ?: ""
        return if (playUrl.isNotBlank()) playUrl.hashCode().toLong() else (games.getOrNull(position)?.title?.hashCode()?.toLong() ?: position.toLong())
    }

    fun updateList(newGames: List<GameModel>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = games.size
            override fun getNewListSize(): Int = newGames.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return games[oldItemPosition].getPlayUrl() == newGames[newItemPosition].getPlayUrl() &&
                       games[oldItemPosition].title == newGames[newItemPosition].title
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return games[oldItemPosition] == newGames[newItemPosition]
            }

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                val oldGame = games[oldItemPosition]
                val newGame = newGames[newItemPosition]
                // If only isFavorite changed, send this payload
                if (oldGame.copy(isFavorite = newGame.isFavorite) == newGame) {
                    return "FAVORITE_CHANGED"
                }
                return super.getChangePayload(oldItemPosition, newItemPosition)
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        games = newGames
        diffResult.dispatchUpdatesTo(this)
    }

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_LARGE = 1
        private const val VIEW_TYPE_FOOTER = 2

        fun vibrateDevice(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(55)
                }
            }
        }
    }
}
