package com.realityexpander.guessasketch.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realityexpander.guessasketch.data.remote.common.Room
import com.realityexpander.guessasketch.repository.SetupRepository
import com.realityexpander.guessasketch.util.Constants.MAX_PLAYER_NAME_LENGTH
import com.realityexpander.guessasketch.util.Constants.MAX_ROOM_NAME_LENGTH
import com.realityexpander.guessasketch.util.Constants.MIN_PLAYER_NAME_LENGTH
import com.realityexpander.guessasketch.util.Constants.MIN_ROOM_NAME_LENGTH
import com.realityexpander.guessasketch.util.DispatcherProvider
import com.realityexpander.guessasketch.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class SetupViewModel @Inject constructor(
    private val setupRepository: SetupRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    sealed class SetupEvent {

        /// Validation ///
        object InputEmptyError: SetupEvent()
        object InputTooShortError: SetupEvent()
        object InputTooLongError: SetupEvent()

        /// UI Events ///
        object ShowLoadingEvent: SetupEvent() // show the progress indicator
        object HideLoadingEvent: SetupEvent()

        /// Navigation ///
        data class NavigateToSelectRoomEvent(val playerName: String): SetupEvent()


        /// REPOSITORY CALLS ///
        data class CreateRoomEvent(val room: Room): SetupEvent()
        data class CreateRoomErrorEvent(val error: String): SetupEvent()

        data class GetRoomEvent(val rooms: List<Room>): SetupEvent()
        data class GetRoomErrorEvent(val error: String): SetupEvent()
        object GetRoomEmptyEvent: SetupEvent()

        data class JoinRoomEvent(val roomName: String): SetupEvent()
        data class JoinRoomErrorEvent(val error: String): SetupEvent()
    }

    // SharedFlow ==> Emits the values only once (like showing a snackbar)
    private val _setupEvent = MutableSharedFlow<SetupEvent>()
    val setupEvent: SharedFlow<SetupEvent> = _setupEvent

    // StateFlow ==> Emits value when set and also Re-Emits value after config change (used to reload the data into UI list)
    private val _rooms = MutableStateFlow<SetupEvent>(SetupEvent.GetRoomEmptyEvent)
    val rooms: StateFlow<SetupEvent> = _rooms


    fun validatePlayerNameAndNavigateToSelectRoom(playerName: String) {

        viewModelScope.launch(dispatchers.main) {
            val trimmedPlayerName = playerName.trim()

            when {
                trimmedPlayerName.isEmpty() ->
                    _setupEvent.emit(SetupEvent.InputEmptyError)
                trimmedPlayerName.length < MIN_PLAYER_NAME_LENGTH ->
                    _setupEvent.emit(SetupEvent.InputTooShortError)
                trimmedPlayerName.length > MAX_PLAYER_NAME_LENGTH ->
                    _setupEvent.emit(SetupEvent.InputTooLongError)
                else -> _setupEvent.emit(SetupEvent.NavigateToSelectRoomEvent(playerName))
            }
        }
    }

    fun createRoom(room: Room) {

        viewModelScope.launch(dispatchers.main) {
            val trimmedRoomName = room.roomName.trim()

            when {
                trimmedRoomName.isEmpty() ->
                    _setupEvent.emit(SetupEvent.InputEmptyError)
                trimmedRoomName.length < MIN_ROOM_NAME_LENGTH ->
                    _setupEvent.emit(SetupEvent.InputTooShortError)
                trimmedRoomName.length > MAX_ROOM_NAME_LENGTH ->
                    _setupEvent.emit(SetupEvent.InputTooLongError)
                else -> {
                    when (val result = setupRepository.createRoom(room)) {
                        is Resource.Success -> {
                            _setupEvent.emit(SetupEvent.HideLoadingEvent)
                            _setupEvent.emit(SetupEvent.NavigateToSelectRoomEvent(room.roomName))
                        }
                        is Resource.Error   ->
                            _setupEvent.emit(SetupEvent.CreateRoomErrorEvent(result.message ?: "Unknown error"))
                        is Resource.Loading ->
                            _setupEvent.emit(SetupEvent.ShowLoadingEvent)
                    }
                }
            }
        }
    }

    fun getRooms(searchQuery: String) {

        viewModelScope.launch(dispatchers.main) {
            val trimmedQuery = searchQuery.trim()

            when (val result = setupRepository.getRooms(trimmedQuery)) {
                is Resource.Success -> {
                    when (val rooms = result.data) {
                        null -> _rooms.value = SetupEvent.GetRoomEmptyEvent
                        else -> _rooms.value = SetupEvent.GetRoomEvent(rooms)
                    }
                    _setupEvent.emit(SetupEvent.HideLoadingEvent)
                }
                is Resource.Error   ->
                    _setupEvent.emit(SetupEvent.GetRoomErrorEvent(result.message ?: "Unknown error"))
                is Resource.Loading ->
                    _setupEvent.emit(SetupEvent.ShowLoadingEvent)
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
                    _setupEvent.emit(SetupEvent.HideLoadingEvent)
                    // _setupEvent.emit(SetupEvent.JoinRoomEvent(trimmedRoomName)) // needed?
                    _setupEvent.emit(SetupEvent.NavigateToSelectRoomEvent(trimmedRoomName))
                }
                is Resource.Error   ->
                    _setupEvent.emit(SetupEvent.JoinRoomErrorEvent(result.message ?: "Unknown error"))
                is Resource.Loading ->
                    _setupEvent.emit(SetupEvent.ShowLoadingEvent)
            }
        }
    }
}

























