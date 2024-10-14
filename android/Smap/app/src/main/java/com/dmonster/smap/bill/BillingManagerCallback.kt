package com.dmonster.smap.bill
import com.android.billingclient.api.Purchase

interface BillingManagerCallback {
    fun onBillingManagerReady()
    fun onBillingManagerProductReady()
    fun onSuccess(purchase: Purchase)
    fun onFailure(errorCode: Int)
}