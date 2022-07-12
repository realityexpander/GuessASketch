package com.realityexpander.guessasketch.ui.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.data.remote.common.Room
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.Announcement
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.BaseMessageType
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.ChatMessage
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.ClientId
import com.realityexpander.guessasketch.databinding.ItemAnnouncementBinding
import com.realityexpander.guessasketch.databinding.ItemChatMessageIncomingBinding
import com.realityexpander.guessasketch.databinding.ItemChatMessageOutgoingBinding
import com.realityexpander.guessasketch.util.toTimeString
import kotlinx.coroutines.*

private const val VIEW_TYPE_INCOMING_MESSAGE = 0
private const val VIEW_TYPE_OUTGOING_MESSAGE = 1
private const val VIEW_TYPE_ANNOUNCEMENT = 2

class ChatMessageAdapter constructor(
    private val playerName: String,
    private val clientId: ClientId,
):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var chatItems = listOf<BaseMessageType>()
        private set

    class IncomingChatMessageViewHolder(val binding: ItemChatMessageIncomingBinding):
        RecyclerView.ViewHolder(binding.root)

    class OutgoingChatMessageViewHolder(val binding: ItemChatMessageOutgoingBinding):
        RecyclerView.ViewHolder(binding.root)

    class AnnouncementViewHolder(val binding: ItemAnnouncementBinding):
        RecyclerView.ViewHolder(binding.root)

    private var updateChatMessagesJob: Job? = null
    fun updateChatMessageList(newData: List<BaseMessageType>, lifecycleScope: CoroutineScope) {
        updateChatMessagesJob?.cancel() // cancel the previous job if it exists
        updateChatMessagesJob = lifecycleScope.launch {
            updateDataset(newData)
        }
    }

    // This job must be cancelled when the adapter is destroyed to avoid memory leaks.
    suspend fun updateDataset(newDataset: List<BaseMessageType>) = withContext(Dispatchers.Default) {
        val diff = DiffUtil.calculateDiff(object: DiffUtil.Callback() {

            override fun getOldListSize(): Int {
                return chatItems.size
            }

            override fun getNewListSize(): Int {
                return newDataset.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return chatItems[oldItemPosition]== newDataset[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return chatItems[oldItemPosition] == newDataset[newItemPosition]
            }
        })

        // Update the recyclerView on the main thread
        withContext(Dispatchers.Main) {
            chatItems = newDataset
            diff.dispatchUpdatesTo(this@ChatMessageAdapter)  // must happen on main thread
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            VIEW_TYPE_INCOMING_MESSAGE -> {
                val binding = ItemChatMessageIncomingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false)
                IncomingChatMessageViewHolder(binding)
            }
            VIEW_TYPE_OUTGOING_MESSAGE -> {
                val binding = ItemChatMessageOutgoingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false)
                OutgoingChatMessageViewHolder(binding)
            }
            VIEW_TYPE_ANNOUNCEMENT -> {
                val binding = ItemAnnouncementBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false)
                AnnouncementViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatItem = chatItems[position]

        when(holder) {
            is IncomingChatMessageViewHolder -> {
                holder.binding.apply {
                    (chatItem as ChatMessage).let { chatMessage ->
                        tvMessage.text = chatMessage.message
                        tvPlayername.text = chatMessage.fromPlayerName
                        tvTime.text = chatMessage.timestamp.toTimeString()
                    }
                }
            }
            is OutgoingChatMessageViewHolder -> {
                holder.binding.apply {
                    (chatItem as ChatMessage).let { chatMessage ->
                        tvMessage.text = chatMessage.message
                        tvPlayername.text = playerName
                        tvTime.text = chatMessage.timestamp.toTimeString()
                    }
                }
            }
            is AnnouncementViewHolder -> {
                holder.binding.apply {
                    (chatItem as Announcement).let { announcement ->
                        tvAnnouncement.text = announcement.message
                        tvTime.text = announcement.timestamp.toTimeString()
                        tvAnnouncement.setTextColor(Color.BLACK)
                        tvTime.setTextColor(Color.BLACK)

                        when(announcement.announcementType) {
                            Announcement.ANNOUNCEMENT_EVERYBODY_GUESSED_CORRECTLY -> {
                                root.setBackgroundColor(Color.GREEN)
                                tvAnnouncement.setTextColor(Color.WHITE)
                                tvTime.setTextColor(Color.WHITE)
                            }
                            Announcement.ANNOUNCEMENT_PLAYER_GUESSED_CORRECTLY -> {
                                root.setBackgroundColor(Color.YELLOW)
                                tvAnnouncement.setTextColor(Color.BLACK)
                                tvTime.setTextColor(Color.BLACK)
                            }
                            Announcement.ANNOUNCEMENT_PLAYER_JOINED_ROOM -> {
                                root.setBackgroundColor(Color.GREEN)
                                tvAnnouncement.setTextColor(Color.WHITE)
                                tvTime.setTextColor(Color.WHITE)
                            }
                            Announcement.ANNOUNCEMENT_PLAYER_EXITED_ROOM -> {
                                root.setBackgroundColor(Color.RED)
                                tvAnnouncement.setTextColor(Color.BLACK)
                                tvTime.setTextColor(Color.BLACK)
                            }
                            Announcement.ANNOUNCEMENT_NOBODY_GUESSED_CORRECTLY -> {
                                root.setBackgroundColor(Color.BLUE)
                                tvAnnouncement.setTextColor(Color.WHITE)
                                tvTime.setTextColor(Color.WHITE)
                            }
                            Announcement.ANNOUNCEMENT_GENERAL_MESSAGE -> {
                                root.setBackgroundColor(ContextCompat.getColor(
                                    holder.binding.root.context, R.color.yellow
                                ))
                            }
                        }
                    }
                }
            }
            else -> throw IllegalArgumentException("Unknown ChatMessage view type")
        }

    }

    override fun getItemCount(): Int {
        return chatItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return when(chatItems[position]) {
            is ChatMessage -> {
                if ((chatItems[position] as ChatMessage).fromClientId == clientId) {
                // if ((chatItems[position] as ChatMessage).fromPlayerName == playerName) {  // bad if 2 players have the same name
                    VIEW_TYPE_OUTGOING_MESSAGE
                } else {
                    VIEW_TYPE_INCOMING_MESSAGE
                }
            }
            is Announcement -> VIEW_TYPE_ANNOUNCEMENT
            else -> throw IllegalArgumentException("Unknown message type")
        }
    }

}