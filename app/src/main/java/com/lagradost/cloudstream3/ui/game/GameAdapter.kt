package com.lagradost.cloudstream3.ui.game

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.request.crossfade
import com.facebook.shimmer.ShimmerFrameLayout
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage
import com.lagradost.cloudstream3.R

class GameAdapter(
    private val onGameClick: (GameModel) -> Unit,
    private val onFavoriteClick: (GameModel) -> Unit,
    private val forceNormal: Boolean = false
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private var games: List<GameModel> = emptyList()
    
    sealed class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract val imageView: ImageView
        abstract val favoriteButton: ImageView
        abstract val loadingView: ShimmerFrameLayout?
        
        class NormalViewHolder(view: View) : GameViewHolder(view) {
            override val imageView: ImageView = view.findViewById(R.id.gameImageView)
            override val favoriteButton: ImageView = view.findViewById(R.id.favoriteButton)
            override val loadingView: ShimmerFrameLayout? = view.findViewById(R.id.gameLoadingView)
        }
        
        class LargeViewHolder(view: View) : GameViewHolder(view) {
            override val imageView: ImageView = view.findViewById(R.id.gameImageView)
            override val favoriteButton: ImageView = view.findViewById(R.id.favoriteButton)
            override val loadingView: ShimmerFrameLayout? = view.findViewById(R.id.gameLoadingView)
            val titleView: TextView = view.findViewById<TextView>(R.id.gameTitleTextView)
        }
    }

    override fun getItemViewType(position: Int): Int {
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
        if (payloads.isNotEmpty() && payloads.contains("FAVORITE_CHANGED")) {
            // Only update the favorite button
            val game = games[position]
            holder.favoriteButton.setImageResource(
                if (game.isFavorite) R.drawable.ic_baseline_bookmark_24 
                else R.drawable.ic_baseline_bookmark_border_24
            )
            return
        }
        // If no payload, do full bind
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]
        
        // Setup Favorite Icon
        holder.favoriteButton.setImageResource(
            if (game.isFavorite) R.drawable.ic_baseline_bookmark_24 
            else R.drawable.ic_baseline_bookmark_border_24
        )
        holder.favoriteButton.setOnClickListener { onFavoriteClick(game) }

        // Show loading shimmer initially
        holder.loadingView?.visibility = View.VISIBLE
        holder.loadingView?.startShimmer()
        holder.imageView.visibility = View.INVISIBLE

        when (holder) {
            is GameViewHolder.NormalViewHolder -> {
                holder.imageView.loadImage(game.images.icon) {
                    crossfade(true)
                    listener(
                        onStart = {
                            holder.loadingView?.visibility = View.VISIBLE
                            holder.loadingView?.startShimmer()
                            holder.imageView.visibility = View.INVISIBLE
                        },
                        onSuccess = { _, _ ->
                            holder.loadingView?.stopShimmer()
                            holder.loadingView?.visibility = View.GONE
                            holder.imageView.visibility = View.VISIBLE
                        },
                        onError = { _, _ ->
                            holder.loadingView?.stopShimmer()
                            holder.loadingView?.visibility = View.GONE
                            holder.imageView.visibility = View.VISIBLE
                        }
                    )
                }
                holder.itemView.setOnClickListener { onGameClick(game) }
            }
            is GameViewHolder.LargeViewHolder -> {
                holder.imageView.loadImage(game.images.poster) {
                    crossfade(true)
                    listener(
                        onStart = {
                            holder.loadingView?.visibility = View.VISIBLE
                            holder.loadingView?.startShimmer()
                            holder.imageView.visibility = View.INVISIBLE
                        },
                        onSuccess = { _, _ ->
                            holder.loadingView?.stopShimmer()
                            holder.loadingView?.visibility = View.GONE
                            holder.imageView.visibility = View.VISIBLE
                        },
                        onError = { _, _ ->
                            holder.loadingView?.stopShimmer()
                            holder.loadingView?.visibility = View.GONE
                            holder.imageView.visibility = View.VISIBLE
                        }
                    )
                }
                // Hide title as requested for search results style consistency
                holder.titleView.visibility = View.GONE
                holder.itemView.setOnClickListener { onGameClick(game) }
            }
        }
    }

    override fun getItemCount(): Int = games.size

    override fun getItemId(position: Int): Long {
        return games[position].gameURL.hashCode().toLong()
    }

    fun updateList(newGames: List<GameModel>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = games.size
            override fun getNewListSize(): Int = newGames.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return games[oldItemPosition].gameURL == newGames[newItemPosition].gameURL
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
    }
}
