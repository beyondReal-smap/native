package com.dmonster.smap.view.main

import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import com.android.billingclient.api.Purchase
import com.dmonster.smap.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(

) : BaseViewModel() {
    var isLocationPermissionCheck = false

    private val _navigateToChannel = MutableStateFlow<NavDirections?>(null)
    val navigateToChannel = _navigateToChannel.asStateFlow()

    fun fragmentNavigateTo(item: NavDirections?) = viewModelScope.launch {
        item?.let {
            _navigateToChannel.value = it
        }
    }

    private val _checkPermissionChannel = Channel<Unit>(Channel.CONFLATED)
    val checkPermissionChannel = _checkPermissionChannel.receiveAsFlow()

    fun checkPermission() = viewModelScope.launch {
        _checkPermissionChannel.send(Unit)
    }

    private val _permissionsCompleteChannel = Channel<Unit>(Channel.CONFLATED)
    val permissionsCompleteChannel = _permissionsCompleteChannel.receiveAsFlow()

    fun permissionsComplete() = viewModelScope.launch {
        _permissionsCompleteChannel.send(Unit)
    }

    private val _backPressedChannel = Channel<Unit>(Channel.CONFLATED)
    val backPressedChannel = _backPressedChannel.receiveAsFlow()

    fun backPressed() = viewModelScope.launch {
        _backPressedChannel.send(Unit)
    }

    private val _startForegroundServiceChannel = Channel<Unit>(Channel.CONFLATED)
    val startForegroundServiceChannel = _startForegroundServiceChannel.receiveAsFlow()

    fun startForegroundService() = viewModelScope.launch {
        _startForegroundServiceChannel.send(Unit)
    }

    private val _stopForegroundServiceChannel = Channel<Unit>(Channel.CONFLATED)
    val stopForegroundServiceChannel = _stopForegroundServiceChannel.receiveAsFlow()

    fun stopForegroundService() = viewModelScope.launch {
        _stopForegroundServiceChannel.send(Unit)
    }

    private val _pushUrlChannel = Channel<String>(Channel.CONFLATED)
    val pushUrlChannel = _pushUrlChannel.receiveAsFlow()

    fun pushUrl(url: String) = viewModelScope.launch {
        _pushUrlChannel.send(url)
    }

    private val _removePushUrlChannel = Channel<Unit>(Channel.CONFLATED)
    val removePushUrlChannel = _removePushUrlChannel.receiveAsFlow()

    fun removePushUrl() = viewModelScope.launch {
        _removePushUrlChannel.send(Unit)
    }

    private val _deepLinkChannel = Channel<String>(Channel.CONFLATED)
    val deepLinkChannel = _deepLinkChannel.receiveAsFlow()

    fun deepLink(invitation_code: String) = viewModelScope.launch {
        _deepLinkChannel.send(invitation_code)
    }

    private val _removeDeepLinkChannel = Channel<Unit>(Channel.CONFLATED)
    val removeDeepLinkChannel = _removeDeepLinkChannel.receiveAsFlow()

    fun removeDeepLink() = viewModelScope.launch {
        _removeDeepLinkChannel.send(Unit)
    }

    private val _loginReceiveChannel = Channel<Unit>(Channel.CONFLATED)
    val loginReceiveChannel = _loginReceiveChannel.receiveAsFlow()

    fun loginReceive() = viewModelScope.launch {
        _loginReceiveChannel.send(Unit)
    }

    private val _logoutReceiveChannel = Channel<Unit>(Channel.CONFLATED)
    val logoutReceiveChannel = _logoutReceiveChannel.receiveAsFlow()

    fun logoutReceive() = viewModelScope.launch {
        _logoutReceiveChannel.send(Unit)
    }

    //결제
    private val _purchaseChannel = Channel<String>(Channel.CONFLATED)
    val purchaseChannel = _purchaseChannel.receiveAsFlow()

    fun purchase(type: String) = viewModelScope.launch {
        _purchaseChannel.send(type)
    }

    private val _purchaseDoneChannel = Channel<Purchase>(Channel.CONFLATED)
    val purchaseDoneChannel = _purchaseDoneChannel.receiveAsFlow()

    fun purchaseDone(purchase: Purchase) = viewModelScope.launch {
        _purchaseDoneChannel.send(purchase)
    }

    //결제 체크
    private val _purchaseCheckChannel = Channel<Unit>(Channel.CONFLATED)
    val purchaseCheckChannel = _purchaseCheckChannel.receiveAsFlow()

    fun purchaseCheck() = viewModelScope.launch {
        _purchaseCheckChannel.send(Unit)
    }

    private val _purchaseCheckCheckChannel = Channel<Purchase?>(Channel.CONFLATED)
    val purchaseCheckCheckChannel = _purchaseCheckCheckChannel.receiveAsFlow()

    fun purchaseCheckCheck(purchase: Purchase?) = viewModelScope.launch {
        _purchaseCheckCheckChannel.send(purchase)
    }

    private val _showAdChannel = Channel<Unit>(Channel.CONFLATED)
    val showAdChannel = _showAdChannel.receiveAsFlow()

    fun showAd() = viewModelScope.launch {
        _showAdChannel.send(Unit)
    }

    private val _endAdChannel = Channel<Unit>(Channel.CONFLATED)
    val endAdChannel = _endAdChannel.receiveAsFlow()

    fun endAd() = viewModelScope.launch {
        _endAdChannel.send(Unit)
    }

    private val _failAdChannel = Channel<String>(Channel.CONFLATED)
    val failAdChannel = _failAdChannel.receiveAsFlow()

    fun failAd(errorType: String) = viewModelScope.launch {
        _failAdChannel.send(errorType)
    }
}