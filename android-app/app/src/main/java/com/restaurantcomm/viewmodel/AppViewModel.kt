package com.restaurantcomm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.restaurantcomm.data.RoleRepository
import com.restaurantcomm.data.model.DeviceRole
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

class AppViewModel(private val repository: RoleRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        repository.observeRole()
            .onEach { role ->
                _uiState.value = if (role == null) {
                    AppUiState.RoleRequired()
                } else {
                    AppUiState.Ready(role)
                }
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
}

class AppViewModelFactory(private val repository: RoleRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
