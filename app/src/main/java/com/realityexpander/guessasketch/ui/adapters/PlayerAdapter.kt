package com.realityexpander.guessasketch.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.BaseMessageType
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.PlayerData
import com.realityexpander.guessasketch.databinding.ItemPlayerBinding
import kotlinx.coroutines.*
import javax.inject.Inject

// @AndroidEntryPoint is not needed here... interesting! Just the @Inject annotation is needed to use this as a provier
class PlayerAdapter @Inject constructor():
    RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    ////////////////////////////////////////////////////////
    /// PUBLIC METHODS & VARS TO BE USED BY THE ACTIVITY ///

    var players = listOf<PlayerData>()
        private set

    var updatePlayersJob: Job? = null
        private set
    fun updatePlayers(newData: List<PlayerData>, lifecycleScope: CoroutineScope) {
        updatePlayersJob?.cancel() // cancel the previous job if it exists
        updatePlayersJob = lifecycleScope.launch {
            updateDataset(newData)
        }
    }

    suspend fun waitForPlayersToUpdate() {
        updatePlayersJob?.join()
    }


    //////////////////////////////////////////////////
    /// PRIVATE STUFF TO BE USED INTERNALLY        ///

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
            ivPencil.isVisible = false
            tvRank.text = ""
            tvScore.text = ""

            if (position != 0) {  // first item is the room name (hacky)
                "${player.rank}.".also { tvRank.text = it }
                tvScore.text = player.score.toString()
                ivPencil.isVisible = player.isDrawingPlayer
            }

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