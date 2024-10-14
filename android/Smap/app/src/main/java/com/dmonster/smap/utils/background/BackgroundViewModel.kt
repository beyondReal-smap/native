package com.dmonster.smap.utils.background

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dmonster.smap.api.ApiUrl
import com.dmonster.smap.api.BaseResult
import com.dmonster.smap.base.BaseViewModel
import com.dmonster.smap.data.AuthData
import com.dmonster.smap.data.MemberLocationData
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class BackgroundViewModel @Inject constructor(
    private val application: Application
) : BaseViewModel() {
    val locationList = MutableStateFlow(listOf<Location>())
    val lat = MutableStateFlow(0.0)
    val lng = MutableStateFlow(0.0)

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BackgroundViewModel::class.java)) {
                return BackgroundViewModel(
                    application,
                ) as T
            }

            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private val _loginReceiveChannel = Channel<Unit>(Channel.CONFLATED)
    val loginReceiveChannel = _loginReceiveChannel.receiveAsFlow()

    fun loginReceive() = viewModelScope.launch {
        _loginReceiveChannel.send(Unit)
    }

    private val _locationCheckChannel = Channel<Unit>(Channel.CONFLATED)
    val locationCheckChannel = _locationCheckChannel.receiveAsFlow()

    fun locationCheck() = viewModelScope.launch {
        _locationCheckChannel.send(Unit)
    }

    private val _authSuccessChannel = Channel<AuthData?>(Channel.CONFLATED)
    val authSuccessChannel = _authSuccessChannel.receiveAsFlow()

    fun authSuccess(authData: AuthData?) = viewModelScope.launch {
        _authSuccessChannel.send(authData)
    }

    private val _authFailChannel = Channel<String>(Channel.CONFLATED)
    val authFailChannel = _authFailChannel.receiveAsFlow()

    fun authFail(errorMsg: String) = viewModelScope.launch {
        _authFailChannel.send(errorMsg)
    }

    private val _locationRestartChannel = Channel<Unit>(Channel.CONFLATED)
    val locationRestartChannel = _locationRestartChannel.receiveAsFlow()

    fun locationRestart() = viewModelScope.launch {
        _locationRestartChannel.send(Unit)
    }

    fun auth(data: HashMap<String, Any>) {
        val url = ApiUrl.authUrl
        repository.requestPost(url, data)
            .onStart {
                //로딩 시작
            }
            .onEach { result ->
                try {
                    val resultString = result.string()
                    val type = object : TypeToken<BaseResult<AuthData>>() {}.type
                    val resultJson = Gson().fromJson<BaseResult<AuthData>>(JsonParser.parseString(resultString).asJsonObject, type)
                    if (resultJson.success == "true") {
                        authSuccess(resultJson.data)
                    } else {
                        authFail(resultJson.message.toString())
                    }
                } catch (e: Exception) {
                    //파싱 에러
                    authFail(e.message.toString())
                }
            }
            .catch {
                //에러 처리
                authFail(it.message.toString())
            }
            .onCompletion {
                //로딩 끝
            }
            .launchIn(viewModelScope)
    }

    fun memberLocation(data: HashMap<String, Any>){
        val url = ApiUrl.memberLocationUrl
        repository.requestPost(url, data)
            .onStart {
                //로딩 시작
            }
            .onEach { result ->
                try {
                    val resultString = result.string()
                    val type = object : TypeToken<BaseResult<MemberLocationData>>() {}.type
                    val resultJson = Gson().fromJson<BaseResult<MemberLocationData>>(JsonParser.parseString(resultString).asJsonObject, type)
                    if (resultJson.success == "true") {

                    } else {

                    }
                } catch (e: Exception) {
                    //파싱 에러
                    Log.e("memberLocation", "error - ${e.message}")
                }
            }
            .catch {
                //에러 처리
                Log.e("memberLocation", "catch - ${it.message}")
            }
            .onCompletion {
                //로딩 끝
            }
            .launchIn(viewModelScope)
    }
}