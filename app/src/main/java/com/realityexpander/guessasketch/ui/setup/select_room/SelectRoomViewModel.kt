package com.realityexpander.guessasketch.ui.setup.select_room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realityexpander.guessasketch.data.remote.common.Room
import com.realityexpander.guessasketch.repository.SetupRepository
import com.realityexpander.guessasketch.util.DispatcherProvider
import com.realityexpander.guessasketch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectRoomViewModel @Inject constructor(
    private val setupRepository: SetupRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    // UI Events for the setup screens
    sealed class SetupEvent {

        /// UI Events ///
        object HideLoadingEvent: SetupEvent() // hide the progress indicator

        /// Navigation ///
        data class NavigateToCreateRoomEvent(val playerName: String): SetupEvent()

        /// REPOSITORY CALLS ///
        data class JoinRoomEvent(val roomName: String): SetupEvent()
        data class JoinRoomErrorEvent(val errorMessage: String): SetupEvent()
    }

    // Room List events
    sealed class RoomsEvent {
        object InitialState: RoomsEvent()
        object ShowLoadingEvent: RoomsEvent()
        data class GetRoomsEvent(val rooms: List<Room>): RoomsEvent()
        object GetRoomsEmptyEvent: RoomsEvent()
        data class GetRoomsErrorEvent(val errorMessage: String): RoomsEvent()
    }

    // SharedFlow ==> Emits the values only once, to possibly multiple listeners. (for showing a snackbar)
    private val _setupEvent = MutableSharedFlow<SetupEvent>()
    val setupEvent: SharedFlow<SetupEvent> = _setupEvent

    // StateFlow ==> Emits value when set and also Re-Emits value after config change (used to reload the data into UI list)
    private val _rooms = MutableStateFlow<RoomsEvent>(RoomsEvent.InitialState)
    val rooms: StateFlow<RoomsEvent> = _rooms

    fun emitSetupEvent(event: SetupEvent) {
        viewModelScope.launch {
            _setupEvent.emit(event)
        }
    }

    fun getRooms(searchQuery: String) {

        viewModelScope.launch(dispatchers.main) {
            val trimmedQuery = searchQuery.trim()

            _rooms.value = RoomsEvent.ShowLoadingEvent
            when (val result = setupRepository.getRooms(trimmedQuery)) {
                is Resource.Success -> {
                    when (val rooms = result.data) {
                        null -> _rooms.value = RoomsEvent.GetRoomsEmptyEvent
                        else -> _rooms.value = RoomsEvent.GetRoomsEvent(rooms)
                    }
                }
                is Resource.Error   -> {
                    _rooms.emit(
                        RoomsEvent.GetRoomsErrorEvent(
                            result.message ?: "Unknown error"
                        )
                    )
                }
                else -> {
                    // Should never get here
                    _rooms.value = RoomsEvent.InitialState
                }
            }
        }
    }

    fun joinRoom(playerName: String, roomName: String) {

        viewModelScope.launch(dispatchers.main) {
            val trimmedRoomName = roomName.trim()
            val trimmedPlayerName = playerName.trim()

            when (val result
                    = setupRepository.joinRoom(trimmedPlayerName, trimmedRoomName)) {
                is Resource.Success -> {
                    _setupEvent.emit(SetupEvent.JoinRoomEvent(trimmedRoomName))
                }
                is Resource.Error   ->
                    _setupEvent.emit(
                        SetupEvent.JoinRoomErrorEvent(
                            result.message ?: "Unknown error"
                        )
                    )

                else -> {
                    // Should never get here
                    _setupEvent.emit(SetupEvent.HideLoadingEvent)
                }
            }
        }
    }
}

























