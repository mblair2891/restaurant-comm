package com.restaurantcomm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.restaurantcomm.data.RoleRepository
import com.restaurantcomm.data.model.DeviceRole
import com.restaurantcomm.data.model.Message
import com.restaurantcomm.discovery.DiscoveredDevice
import com.restaurantcomm.discovery.DiscoveryManager
import com.restaurantcomm.discovery.DiscoveryStatus
import com.restaurantcomm.messaging.MessagingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface AppUiState {
    data object Loading : AppUiState
    data class RoleRequired(val role: DeviceRole? = null) : AppUiState
    data class Ready(val role: DeviceRole) : AppUiState
}

data class DiscoveryUiState(
    val deviceId: String? = null,
    val status: DiscoveryStatus = DiscoveryStatus.Idle,
    val peers: List<DiscoveredDevice> = emptyList()
)

data class MessagingUiState(
    val selectedPeerId: String? = null,
    val messageDraft: String = "",
    val messages: List<Message> = emptyList()
)

class AppViewModel(
    private val repository: RoleRepository,
    private val discoveryManager: DiscoveryManager,
    private val messagingRepository: MessagingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _discoveryUiState = MutableStateFlow(DiscoveryUiState())
    val discoveryUiState: StateFlow<DiscoveryUiState> = _discoveryUiState.asStateFlow()

    private val _messagingUiState = MutableStateFlow(MessagingUiState())
    val messagingUiState: StateFlow<MessagingUiState> = _messagingUiState.asStateFlow()

    init {
        repository.observeRole()
            .onEach { role ->
                _uiState.value = if (role == null) {
                    discoveryManager.stop()
                    messagingRepository.stopListener()
                    _messagingUiState.value = MessagingUiState()
                    AppUiState.RoleRequired()
                } else {
                    discoveryManager.start(role, MESSAGE_PORT)
                    messagingRepository.startListener(MESSAGE_PORT)
                    AppUiState.Ready(role)
                }
            }
            .launchIn(viewModelScope)

        discoveryManager.status
            .onEach { status ->
                _discoveryUiState.value = _discoveryUiState.value.copy(status = status)
            }
            .launchIn(viewModelScope)

        discoveryManager.peers
            .onEach { peers ->
                val selected = _messagingUiState.value.selectedPeerId
                val selectedStillExists = peers.any { it.deviceId == selected }
                _messagingUiState.value = _messagingUiState.value.copy(
                    selectedPeerId = if (selectedStillExists) selected else peers.firstOrNull()?.deviceId
                )
                _discoveryUiState.value = _discoveryUiState.value.copy(peers = peers)
            }
            .launchIn(viewModelScope)

        discoveryManager.deviceId
            .onEach { deviceId ->
                _discoveryUiState.value = _discoveryUiState.value.copy(deviceId = deviceId)
                val role = (_uiState.value as? AppUiState.Ready)?.role
                if (deviceId != null && role != null) {
                    messagingRepository.setSelf(deviceId, role)
                }
            }
            .launchIn(viewModelScope)

        messagingRepository.messages
            .onEach { messages ->
                _messagingUiState.value = _messagingUiState.value.copy(messages = messages)
            }
            .launchIn(viewModelScope)
    }

    fun saveRole(role: DeviceRole) {
        viewModelScope.launch {
            repository.saveRole(role)
        }
    }

    fun resetRole() {
        viewModelScope.launch {
            repository.resetRole()
        }
    }

    fun updateMessageDraft(value: String) {
        _messagingUiState.value = _messagingUiState.value.copy(messageDraft = value)
    }

    fun selectPeer(deviceId: String?) {
        _messagingUiState.value = _messagingUiState.value.copy(selectedPeerId = deviceId)
    }

    fun sendDirectMessage() {
        val draft = _messagingUiState.value.messageDraft.trim()
        if (draft.isBlank()) return

        val selectedId = _messagingUiState.value.selectedPeerId ?: return
        val peer = _discoveryUiState.value.peers.firstOrNull { it.deviceId == selectedId } ?: return
        messagingRepository.sendDirect(peer, draft)
        _messagingUiState.value = _messagingUiState.value.copy(messageDraft = "")
    }

    fun sendBroadcastMessage() {
        val draft = _messagingUiState.value.messageDraft.trim()
        if (draft.isBlank()) return

        messagingRepository.sendBroadcast(_discoveryUiState.value.peers, draft)
        _messagingUiState.value = _messagingUiState.value.copy(messageDraft = "")
    }

    override fun onCleared() {
        discoveryManager.stop()
        messagingRepository.stopListener()
        super.onCleared()
    }

    companion object {
        private const val MESSAGE_PORT = 49152
    }
}

class AppViewModelFactory(
    private val repository: RoleRepository,
    private val discoveryManager: DiscoveryManager,
    private val messagingRepository: MessagingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(repository, discoveryManager, messagingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
