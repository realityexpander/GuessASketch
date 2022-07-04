package com.realityexpander.guessasketch.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.realityexpander.guessasketch.data.remote.common.Room
import com.realityexpander.guessasketch.databinding.ItemRoomBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomAdapter @Inject constructor():
    RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    class RoomViewHolder(val binding: ItemRoomBinding) : RecyclerView.ViewHolder(binding.root)

    suspend fun updateDataset(newDataset: List<Room>) = withContext(Dispatchers.Default) {
        val diff = DiffUtil.calculateDiff(object: DiffUtil.Callback() {

            override fun getOldListSize(): Int {
                return rooms.size
            }

            override fun getNewListSize(): Int {
                return newDataset.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return rooms[oldItemPosition]== newDataset[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return rooms[oldItemPosition] == newDataset[newItemPosition]
            }
        })

        // Update the recyclerView on the main thread
        withContext(Dispatchers.Main) {
            rooms = newDataset
            diff.dispatchUpdatesTo(this@RoomAdapter)  // must happen on main thread
        }
    }

    var rooms = listOf<Room>()
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        return RoomViewHolder(
            ItemRoomBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.binding.apply {
            tvRoomName.text = room.roomName

            val playerCountText = "${room.playerCount}/${room.maxPlayers}"
            tvRoomPersonCount.text = playerCountText

            // Respond to click on this room item
            root.setOnClickListener {
                onRoomItemClickListener?.let { click ->
                    click(room)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return rooms.size
    }

    // Function that is called when the room item is clicked.
    private var onRoomItemClickListener: ((Room) -> Unit)? = null

    fun setOnRoomItemClickListener(listener: (Room) -> Unit) {
        onRoomItemClickListener = listener
    }
}