package com.lagradost.cloudstream3.ui.game

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import coil3.size.Scale
import coil3.size.Size
import com.lagradost.cloudstream3.R

class GameAdapter(
    private val onGameClick: (GameModel) -> Unit
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private var games: List<GameModel> = emptyList()
    
    sealed class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract val imageView: ImageView
        
        class NormalViewHolder(view: View) : GameViewHolder(view) {
            override val imageView: ImageView = view.findViewById(R.id.gameImageView)
        }
        
        class LargeViewHolder(view: View) : GameViewHolder(view) {
            override val imageView: ImageView = view.findViewById(R.id.gameImageView)
        }
    }

    override fun getItemViewType(position: Int): Int {
        // Use isFeatured flag to determine if it should be a large poster
        return if (games[position].isFeatured) {
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

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]
        
        when (holder) {
            is GameViewHolder.NormalViewHolder -> {
                holder.imageView.load(game.images.icon) {
                    placeholder(R.drawable.ic_game_placeholder)
                    error(R.drawable.ic_game_placeholder)
                    scale(Scale.FILL)
                    size(Size.ORIGINAL)
                }
                holder.itemView.setOnClickListener { onGameClick(game) }
            }
            is GameViewHolder.LargeViewHolder -> {
                holder.imageView.load(game.images.poster) {
                    placeholder(R.drawable.ic_game_placeholder)
                    error(R.drawable.ic_game_placeholder)
                    scale(Scale.FILL)
                    size(Size.ORIGINAL)
                }
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
