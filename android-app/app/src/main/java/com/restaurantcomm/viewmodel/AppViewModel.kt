package com.restaurantcomm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.restaurantcomm.data.RoleRepository
import com.restaurantcomm.data.model.DeviceRole
import com.restaurantcomm.discovery.DiscoveredDevice
import com.restaurantcomm.discovery.DiscoveryManager
import com.restaurantcomm.discovery.DiscoveryStatus
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

class AppViewModel(
    private val repository: RoleRepository,
    private val discoveryManager: DiscoveryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _discoveryUiState = MutableStateFlow(DiscoveryUiState())
    val discoveryUiState: StateFlow<DiscoveryUiState> = _discoveryUiState.asStateFlow()

    init {
        repository.observeRole()
            .onEach { role ->
                _uiState.value = if (role == null) {
                    discoveryManager.stop()
                    AppUiState.RoleRequired()
                } else {
                    discoveryManager.start(role)
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
                _discoveryUiState.value = _discoveryUiState.value.copy(peers = peers)
            }
            .launchIn(viewModelScope)

        discoveryManager.deviceId
            .onEach { deviceId ->
                _discoveryUiState.value = _discoveryUiState.value.copy(deviceId = deviceId)
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

    override fun onCleared() {
        discoveryManager.stop()
        super.onCleared()
    }
}

class AppViewModelFactory(
    private val repository: RoleRepository,
    private val discoveryManager: DiscoveryManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(repository, discoveryManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
