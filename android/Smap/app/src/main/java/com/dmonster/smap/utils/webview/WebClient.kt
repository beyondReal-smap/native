package com.dmonster.smap.utils.webview

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat.startActivity

class WebClient : WebViewClient() {

    private var helper: WebViewSettingHelper? = null

    fun init(helper: WebViewSettingHelper): WebClient {
        this.helper = helper
        return this
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val overrideUrl = request?.url.toString()
        if( !overrideUrl.startsWith("http://") && !overrideUrl.startsWith("https://") && !overrideUrl.startsWith("javascript:") ) {
            if (overrideUrl.startsWith("tel:")) {
                startActivity(
                    view?.context ?: return false,
                    Intent(
                        Intent.ACTION_DIAL,
                        request?.url
                    ),
                    null
                )

                return true
            }

            if (overrideUrl.startsWith("intent:")) {
                if (overrideUrl.startsWith("intent:kakaolink://")) {
                    val intent = Intent.parseUri(overrideUrl, Intent.URI_INTENT_SCHEME)
                    val packageName = "com.kakao.talk"

                    if (view != null) {
                        val context = view.context
                        val existPackage: Intent? =  context.packageManager.getLaunchIntentForPackage(packageName)
                        if (existPackage != null) {
                            startActivity(context, intent, null)
                        } else {
                            val marketIntent = Intent(Intent.ACTION_VIEW)
                            marketIntent.data = Uri.parse("market://details?id=$packageName")
                            startActivity(context, marketIntent, null)
                        }
                    }
                } else {
                    startActivity(
                        view?.context ?: return false,
                        Intent(
                            Intent.ACTION_VIEW,
                            request?.url
                        ),
                        null
                    )
                }

                return true
            }

            view?.loadUrl(overrideUrl)

            return false
        }

        return super.shouldOverrideUrlLoading(view, request)
    }
}