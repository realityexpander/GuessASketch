package com.realityexpander.guessasketch.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.realityexpander.guessasketch.data.remote.common.PlayerData
import com.realityexpander.guessasketch.databinding.ItemPlayerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// @AndroidEntryPoint is not needed here... interesting!
class PlayerAdapter @Inject constructor():
    RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    class PlayerViewHolder(val binding: ItemPlayerBinding) : RecyclerView.ViewHolder(binding.root)

    // This job must be cancelled when the adapter is destroyed to avoid memory leaks.
    suspend fun updateDataset(newDataset: List<PlayerData>) = withContext(Dispatchers.Default) {
        val diff = DiffUtil.calculateDiff(object: DiffUtil.Callback() {

            override fun getOldListSize(): Int {
                return players.size
            }

            override fun getNewListSize(): Int {
                return newDataset.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return players[oldItemPosition]== newDataset[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return players[oldItemPosition] == newDataset[newItemPosition]
            }
        })

        // Update the recyclerView on the main thread
        withContext(Dispatchers.Main) {
            players = newDataset
            diff.dispatchUpdatesTo(this@PlayerAdapter)  // must happen on main thread
        }
    }

    var players = listOf<PlayerData>()
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        return PlayerViewHolder(
            ItemPlayerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = players[position]

        holder.binding.apply {
            tvPlayername.text = player.playerName
            "${player.rank.toString()}.".also { tvRank.text = it }
            tvScore.text = player.score.toString()
            ivPencil.isVisible = player.isDrawingPlayer

            // Respond to click on this player item
            root.setOnClickListener {
                onPlayerItemClickListener?.let { click ->
                    click(player)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return players.size
    }

    // Function that is called when the room item is clicked.
    private var onPlayerItemClickListener: ((PlayerData) -> Unit)? = null

    fun setOnPlayerItemClickListener(listener: (PlayerData) -> Unit) {
        onPlayerItemClickListener = listener
    }
}