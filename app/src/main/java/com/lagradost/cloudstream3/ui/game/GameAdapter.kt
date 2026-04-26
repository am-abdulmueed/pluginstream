package com.lagradost.cloudstream3.ui.game

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.placeholder
import coil3.size.Scale
import com.lagradost.cloudstream3.R

class GameAdapter(
    private val onGameClick: (GameModel) -> Unit
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

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
                // Normal icon (448x448) - square aspect ratio
                val size = holder.itemView.width
                holder.imageView.layoutParams.height = size
                holder.imageView.load(game.images.icon) {
                    crossfade(true)
                    placeholder(R.drawable.ic_game)
                    scale(Scale.FILL)
                }
                holder.itemView.setOnClickListener { onGameClick(game) }
            }
            is GameViewHolder.LargeViewHolder -> {
                // Large poster (448x252) - wide aspect ratio (16:9)
                holder.imageView.layoutParams.height = (holder.itemView.width * 0.56).toInt()
                holder.imageView.load(game.images.poster) {
                    crossfade(true)
                    placeholder(R.drawable.ic_game)
                    scale(Scale.FILL)
                }
                holder.itemView.setOnClickListener { onGameClick(game) }
            }
        }
    }

    override fun getItemCount(): Int = games.size

    fun updateList(newGames: List<GameModel>) {
        games = newGames
        notifyDataSetChanged()
    }

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_LARGE = 1
    }
}
