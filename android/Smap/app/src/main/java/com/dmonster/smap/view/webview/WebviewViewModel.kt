package com.dmonster.smap.view.webview

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.dmonster.smap.api.ApiUrl
import com.dmonster.smap.api.BaseResult
import com.dmonster.smap.base.BaseViewModel
import com.dmonster.smap.data.FileUploadData
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class WebviewViewModel @Inject constructor(

): BaseViewModel() {
    private val _pageTypeChannel = Channel<String>(Channel.CONFLATED)
    val pageTypeChannel = _pageTypeChannel.receiveAsFlow()

    fun pageType(page: String) = viewModelScope.launch {
        _pageTypeChannel.send(page)
    }

    private val _apiLoadingChannel = Channel<Boolean>(Channel.CONFLATED)
    val apiLoadingChannel = _apiLoadingChannel.receiveAsFlow()

    fun apiLoading(isState: Boolean) = viewModelScope.launch {
        _apiLoadingChannel.send(isState)
    }

    private val _openPhotoChannel = Channel<String>(Channel.CONFLATED)
    val openPhotoChannel = _openPhotoChannel.receiveAsFlow()

    fun openPhoto(mt_idx: String) = viewModelScope.launch {
        _openPhotoChannel.send(mt_idx)
    }

    private val _openAlbumChannel = Channel<String>(Channel.CONFLATED)
    val openAlbumChannel = _openAlbumChannel.receiveAsFlow()

    fun openAlbum(mt_idx: String) = viewModelScope.launch {
        _openAlbumChannel.send(mt_idx)
    }

    private val _urlClipBoardChannel = Channel<String>(Channel.CONFLATED)
    val urlClipBoardChannel = _urlClipBoardChannel.receiveAsFlow()

    fun urlClipBoard(url: String) = viewModelScope.launch {
        _urlClipBoardChannel.send(url)
    }

    private val _urlOpenSmsChannel = Channel<String>(Channel.CONFLATED)
    val urlOpenSmsChannel = _urlOpenSmsChannel.receiveAsFlow()

    fun urlOpenSms(url: String) = viewModelScope.launch {
        _urlOpenSmsChannel.send(url)
    }

    private val _openShareChannel = Channel<String>(Channel.CONFLATED)
    val openShareChannel = _openShareChannel.receiveAsFlow()

    fun openShare(url: String) = viewModelScope.launch {
        _openShareChannel.send(url)
    }

    private val _openUrlBlankChannel = Channel<String>(Channel.CONFLATED)
    val openUrlBlankChannel = _openUrlBlankChannel.receiveAsFlow()

    fun openUrlBlank(url: String) = viewModelScope.launch {
        _openUrlBlankChannel.send(url)
    }

    private val _sessionRefreshChannel = Channel<String>(Channel.CONFLATED)
    val sessionRefreshChannel = _sessionRefreshChannel.receiveAsFlow()

    fun sessionRefreshChannel(url: String) = viewModelScope.launch {
        _sessionRefreshChannel.send(url)
    }

    private val _fileUploadSuccessChannel = Channel<Unit>(Channel.CONFLATED)
    val fileUploadSuccessChannel = _fileUploadSuccessChannel.receiveAsFlow()

    fun fileUploadSuccess() = viewModelScope.launch {
        _fileUploadSuccessChannel.send(Unit)
    }

    private val _fileUploadFailChannel = Channel<String>(Channel.CONFLATED)
    val fileUploadFailChannel = _fileUploadFailChannel.receiveAsFlow()

    fun fileUploadFail(errorMsg: String) = viewModelScope.launch {
        _fileUploadFailChannel.send(errorMsg)
    }

    fun fileUpload(data: HashMap<String, Any>, file: HashMap<String, Uri>){
        val url = ApiUrl.fileUploadUrl
        repository.requestPostFileData(url, data, file)
            .onStart {
                //로딩 시작
                apiLoading(true)
            }
            .onEach { result ->
                try {
                    val resultString = result.string()
                    val type = object : TypeToken<BaseResult<FileUploadData>>() {}.type
                    val resultJson = Gson().fromJson<BaseResult<FileUploadData>>(JsonParser.parseString(resultString).asJsonObject, type)
                    if (resultJson.success == "true") {
                        fileUploadSuccess()
                    } else {
                        fileUploadFail(resultJson.message.toString())
                    }
                } catch (e: Exception) {
                    //파싱 에러
                    Log.e("memberLocation", "error - ${e.message}")
                    fileUploadFail(e.message.toString())
                }
            }
            .catch {
                //에러 처리
                Log.e("memberLocation", "catch - ${it.message}")
                fileUploadFail(it.message.toString())
            }
            .onCompletion {
                //로딩 끝
                apiLoading(false)
            }
            .launchIn(viewModelScope)
    }
}