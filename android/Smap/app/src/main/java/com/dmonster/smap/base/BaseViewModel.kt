package com.dmonster.smap.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmonster.smap.api.BaseRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    val repository: BaseRepository by lazy {
        BaseRepository()
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun showLoadingDialog() = viewModelScope.launch {
        _isLoading.emit(true)
    }

    fun hideLoadingDialog() = viewModelScope.launch {
        _isLoading.emit(false)
    }

    private val _showSnackBarChannel = Channel<String>(Channel.CONFLATED)
    val showSnackBarChannel = _showSnackBarChannel.receiveAsFlow()

    fun showSnackBar(
        message: String
    ) = viewModelScope.launch {
        _showSnackBarChannel.send(message)
    }
}