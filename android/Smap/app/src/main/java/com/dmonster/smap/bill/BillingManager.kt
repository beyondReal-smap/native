package com.dmonster.smap.bill

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BillingManager(
    private val activity: Activity,
    private var billingManagerCallback: BillingManagerCallback?
) {
    private val TAG = "IN-APP-BILLING"
    private val LIST_OF_PRODUCTS = listOf(
        BillingConstant.productId
    )

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when {
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                // 구매 완료, 구매 확인 처리 진행
                for (purchase in purchases) {
                    Log.d(TAG, "결제에 대해 응답 받은 데이터 :" + purchase.originalJson)
                    confirmPurchase(purchase)
                }
            }
            else -> {
                // 구매 실패
                Log.d(TAG, "실패코드 = ${billingResult.responseCode}")
                billingManagerCallback?.onFailure(billingResult.responseCode)
            }
        }
    }

    var productDetailsList: List<ProductDetails>? = null
    private var billingClient: BillingClient

    init {
        Log.d(TAG, "구글 결제 매니저를 초기화 하고 있습니다.")

        billingClient = BillingClient.newBuilder(activity)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "구글 결제 서버와 접속이 끊어졌습니다.")
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "구글 결제 서버에 접속을 성공하였습니다.")
                    billingManagerCallback?.onBillingManagerReady()
                } else {
                    Log.d(TAG, "구글 결제 서버 접속을 실패하였습니다.\n오류코드 : ${billingResult.responseCode}")
                    billingManagerCallback?.onFailure(billingResult.responseCode)
                }
            }

        })
    }

    fun getProductDetailList(){
        val params = QueryProductDetailsParams.newBuilder()
        val productList: MutableList<QueryProductDetailsParams.Product> = arrayListOf()
        for (product in LIST_OF_PRODUCTS) {
            productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(product)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        }

        billingClient.queryProductDetailsAsync(params.setProductList(productList).build(), object :
            ProductDetailsResponseListener {
            override fun onProductDetailsResponse(
                billingResult: BillingResult,
                productDetailsList: MutableList<ProductDetails>,
            ) {
                // 상품 정보를 가지고 오지 못한 경우, 오류를 반환하고 종료합니다.
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK){
                    Log.d(TAG, "상품 정보를 가지고 오던 중 오류를 만났습니다. 오류코드 : ${billingResult.responseCode}")
                    billingManagerCallback?.onFailure(billingResult.responseCode)
                    return
                }

                // 응답 받은 데이터들의 숫자를 출력 합니다.
                Log.d(TAG, "응답 받은 데이터 숫자:" + productDetailsList.size)
//                for (pro in productDetailsList) {
//                    Log.d(TAG, "상품확인 productId - ${pro.productId}")
//                    Log.d(TAG, "상품확인 productType - ${pro.productType}")
//                    Log.d(TAG, "상품확인 name - ${pro.name}")
//                    Log.d(TAG, "상품확인 title - ${pro.title}")
//                    Log.d(TAG, "상품확인 subscriptionOfferDetails - ${pro.subscriptionOfferDetails}")
//                    Log.d(TAG, "상품확인 oneTimePurchaseOfferDetails - ${pro.oneTimePurchaseOfferDetails}")
//                    if (pro.subscriptionOfferDetails != null) {
//                        for (subs in pro.subscriptionOfferDetails!!) {
//                            Log.e(TAG, "====================================================================================")
//                            Log.e(TAG, "subs basePlanId = ${subs.basePlanId}")
//                            Log.e(TAG, "subs offerId = ${subs.offerId}")
//                            Log.e(TAG, "subs offerTags = ${subs.offerTags}")
//                            Log.e(TAG, "subs offerToken = ${subs.offerToken}")
//                            Log.e(TAG, "subs pricingPhases = ${subs.pricingPhases}")
//                        }
//                    }
//                }

                // 받은 값을 멤버 변수로 저장합니다.
                this@BillingManager.productDetailsList = productDetailsList
                billingManagerCallback?.onBillingManagerProductReady()
            }
        })
    }

    fun getProductDetail(productId: String) : ProductDetails? {
        if (!productDetailsList.isNullOrEmpty()) {
            for (productDetails in productDetailsList!!) {
                if (productDetails.productId == productId) {
                    return productDetails
                }
            }
        }
        return null
    }

    //태그로 offer 토큰을 찾는다
    fun getProductDetailOfferToken(productDetails: ProductDetails, basePlanId: String): String? {
        val subscriptionOfferDetailsList = productDetails.subscriptionOfferDetails

        if (!subscriptionOfferDetailsList.isNullOrEmpty()) {
            if (basePlanId == BillingConstant.monthPlanId) {
                for (subscriptionOfferDetails in subscriptionOfferDetailsList) {
                    if (subscriptionOfferDetails.basePlanId == basePlanId) {
                        return subscriptionOfferDetails.offerToken
                    }
                }
            } else if (basePlanId == BillingConstant.yearPlanId) {

                var firstOfferToken: String? = null
                var defaultOfferToken = ""
                for (subscriptionOfferDetails in subscriptionOfferDetailsList) {
                    if (subscriptionOfferDetails.basePlanId == basePlanId) {
                        if (subscriptionOfferDetails.offerId == null) {
                            defaultOfferToken = subscriptionOfferDetails.offerToken
                        } else {
                            firstOfferToken = subscriptionOfferDetails.offerToken
                        }
                    }
                }

                return firstOfferToken ?: defaultOfferToken
            }
        }
        return null
    }

    // 실제 구입 처리를 하는 메소드 입니다.
    fun purchase(productDetails: ProductDetails, offerToken: String) {
        val productDetailsParamList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamList)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            billingManagerCallback?.onFailure(responseCode)
        }
    }

    /**
     * @param resultBlock 구매 확인 상품에 대한 처리 return Purchase
     */
    fun checkSubscribed(resultBlock: (Purchase?) -> Unit) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()) {
                _, purchases ->
            CoroutineScope(Dispatchers.Main).launch {
                for (purchase in purchases) {
                    if (purchase.isAcknowledged && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        return@launch resultBlock(purchase)
                    } else if (!purchase.isAcknowledged && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        CoroutineScope(Dispatchers.IO).launch {
                            confirmPurchase(purchase)
                        }
                        return@launch resultBlock(purchase)
                    }
                }
                return@launch resultBlock(null)
            }
        }
    }

    fun destroy(){
        billingClient.endConnection()
    }

    private fun confirmPurchase(purchase: Purchase?){
        purchase?.let {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged) {
                //Log.e(TAG, "purchase -> $purchase")

                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(it.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(params) { billingResult ->
                    //Log.e(TAG, "billingResult -> $billingResult")
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        billingManagerCallback?.onSuccess(purchase)
                    } else {
                        billingManagerCallback?.onFailure(billingResult.responseCode)
                    }
                }
            }
        }
    }
}