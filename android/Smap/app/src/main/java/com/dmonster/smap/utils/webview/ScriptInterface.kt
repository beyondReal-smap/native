package com.dmonster.smap.utils.webview

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.dmonster.smap.utils.showSnackBar
import com.dmonster.smap.view.main.MainViewModel
import com.dmonster.smap.view.webview.WebviewViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScriptInterface(
    private val view: WebView?,
    private val context: Context,
    private val mainViewModel: MainViewModel,
    private val webviewViewModel: WebviewViewModel
) {
    @JavascriptInterface
    fun showToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            view?.let {
                showSnackBar(it, message)
            }
        }
    }

    @JavascriptInterface
    fun startForegroundService() {
        CoroutineScope(Dispatchers.Main).launch {
            mainViewModel.startForegroundService()
        }
    }

    @JavascriptInterface
    fun stopForegroundService() {
        CoroutineScope(Dispatchers.Main).launch {
            mainViewModel.stopForegroundService()
        }
    }

    @JavascriptInterface
    fun pageType(page: String){
        CoroutineScope(Dispatchers.Main).launch {
            webviewViewModel.pageType(page)
        }
    }

    @JavascriptInterface
    fun memberLogin(){
        CoroutineScope(Dispatchers.Main).launch {
            mainViewModel.loginReceive()
        }
    }

    @JavascriptInterface
    fun memberLogout(){
        CoroutineScope(Dispatchers.Main).launch {
            mainViewModel.logoutReceive()
        }
    }

    @JavascriptInterface
    fun openCamera(mt_idx: String){
        CoroutineScope(Dispatchers.Main).launch {
            webviewViewModel.openPhoto(mt_idx)
        }
    }

    @JavascriptInterface
    fun openAlbum(mt_idx: String){
        CoroutineScope(Dispatchers.Main).launch {
            webviewViewModel.openAlbum(mt_idx)
        }
    }

    @JavascriptInterface
    fun urlClipBoard(url: String){
        CoroutineScope(Dispatchers.Main).launch {
            webviewViewModel.urlClipBoard(url)
        }
    }

    @JavascriptInterface
    fun urlOpenSms(url: String){
        CoroutineScope(Dispatchers.Main).launch {
            webviewViewModel.urlOpenSms(url)
        }
    }

    @JavascriptInterface
    fun openShare(content: String) {
        CoroutineScope(Dispatchers.Main).launch {
            webviewViewModel.openShare(content)
        }
    }

    @JavascriptInterface
    fun openUrlBlank(url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            webviewViewModel.openUrlBlank(url)
        }
    }

    @JavascriptInterface
    fun purchase(type: String) {
        CoroutineScope(Dispatchers.Main).launch {
            mainViewModel.purchase(type)
        }
    }

    @JavascriptInterface
    fun purchaseCheck() {
        CoroutineScope(Dispatchers.Main).launch {
            mainViewModel.purchaseCheck()
        }
    }

    @JavascriptInterface
    fun session_refresh(url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            webviewViewModel.sessionRefreshChannel(url)
        }
    }

    @JavascriptInterface
    fun showAd() {
        CoroutineScope(Dispatchers.Main).launch {
            mainViewModel.showAd()
        }
    }
}