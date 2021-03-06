package com.realityexpander.guessasketch.ui.setup.create_room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realityexpander.guessasketch.data.remote.common.Room
import com.realityexpander.guessasketch.repository.SetupRepository
import com.realityexpander.guessasketch.ui.common.Constants.MAX_PLAYER_NAME_LENGTH
import com.realityexpander.guessasketch.ui.common.Constants.MAX_ROOM_NAME_LENGTH
import com.realityexpander.guessasketch.ui.common.Constants.MIN_PLAYER_NAME_LENGTH
import com.realityexpander.guessasketch.ui.common.Constants.MIN_ROOM_NAME_LENGTH
import com.realityexpander.guessasketch.util.DispatcherProvider
import com.realityexpander.guessasketch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val setupRepository: SetupRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    // UI Events for the setup screens
    sealed class SetupEvent {

        /// Validation ///
        object InputEmptyError: SetupEvent()
        object InputTooShortError: SetupEvent()
        object InputTooLongError: SetupEvent()

        /// UI Events ///
        object HideLoadingEvent: SetupEvent() // show the progress indicator

        /// Navigation ///
        data class NavigateToSelectRoomEvent(val playerName: String): SetupEvent()

        /// REPOSITORY CALLS ///
        object CreateRoomEvent: SetupEvent()
        data class CreateRoomErrorEvent(val errorMessage: String): SetupEvent()

        data class JoinRoomEvent(val roomName: String): SetupEvent()
        data class JoinRoomErrorEvent(val errorMessage: String): SetupEvent()
    }

    // SharedFlow ==> Emits the values only once, to possibly multiple listeners. (for showing a snackbar)
    private val _setupEvent = MutableSharedFlow<SetupEvent>()
    val setupEvent: SharedFlow<SetupEvent> = _setupEvent

    fun emitSetupEvent(event: SetupEvent) {
        viewModelScope.launch {
            _setupEvent.emit(event)
        }
    }

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

    fun createRoom(roomName: String, maxPlayers: Int) {

        viewModelScope.launch(dispatchers.main) {
            val trimmedRoomName = roomName.trim()

            when {
                trimmedRoomName.isEmpty() ->
                    _setupEvent.emit(SetupEvent.InputEmptyError)
                trimmedRoomName.length < MIN_ROOM_NAME_LENGTH ->
                    _setupEvent.emit(SetupEvent.InputTooShortError)
                trimmedRoomName.length > MAX_ROOM_NAME_LENGTH ->
                    _setupEvent.emit(SetupEvent.InputTooLongError)
                else -> {
                    val newRoom = Room(roomName, maxPlayers)

                    when (val result = setupRepository.createRoom(newRoom)) {
                        is Resource.Success -> {
                            _setupEvent.emit(SetupEvent.JoinRoomEvent(newRoom.roomName))
                        }
                        is Resource.Error   -> {
                            _setupEvent.emit(
                                SetupEvent.CreateRoomErrorEvent(
                                    result.message ?: "Unknown error"
                                )
                            )
                        }
                        else -> {
                            // Should never get here
                            _setupEvent.emit(SetupEvent.HideLoadingEvent)
                        }
                    }
                }
            }
        }
    }

}

























