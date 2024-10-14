package com.dmonster.smap.view.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.android.billingclient.api.Purchase
import com.dmonster.smap.MyApplication
import com.dmonster.smap.R
import com.dmonster.smap.bill.BillingConstant
import com.dmonster.smap.bill.BillingManager
import com.dmonster.smap.bill.BillingManagerCallback
import com.dmonster.smap.databinding.ActivityMainBinding
import com.dmonster.smap.utils.background.BackgroundService
import com.dmonster.smap.utils.background.BackgroundViewModel
import com.dmonster.smap.utils.delPref
import com.dmonster.smap.utils.observeInLifecycleDestroy
import com.dmonster.smap.utils.observeInLifecycleStop
import com.dmonster.smap.utils.observeOnLifecycleDestroy
import com.dmonster.smap.utils.permission.PermissionViewModel
import com.dmonster.smap.view.intro.IntroFragment
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import android.os.Handler
import android.os.Looper

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), BillingManagerCallback {
    private val TAG = "MainActivity"
    private val binding: ActivityMainBinding by lazy(LazyThreadSafetyMode.NONE) {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }
    private val viewModel: MainViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()
    private var backgroundViewModel: BackgroundViewModel? = null

    private val navHostFragment: NavHostFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.navHostFragmentContainer) as NavHostFragment
    }

    private val navController: NavController by lazy {
        navHostFragment.navController
    }

    private val currentNavigationFragment: Fragment?
        get() = navHostFragment.childFragmentManager.fragments.first()

    private val settingDetails: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            permissionViewModel.onActivityResult()
        }

    private var event_url = ""
    private var invitation_code = ""

    init {
        addOnContextAvailableListener { binding.notifyChange() }
    }

    private lateinit var billingManager: BillingManager

    //광고
    private var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.lifecycleOwner = this

        //광고 SDK 초기화
        MobileAds.initialize(this@MainActivity) {
            Log.e("MobileAds.initialize", "MobileAds.initialize")
            loadAds(false, 0)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                currentNavigationFragment?.let {
                    if (it is IntroFragment) {
                        finishAffinity()

                        return
                    }
                }

                viewModel.backPressed()
            }
        })

        init()
        initViewModelCallback()
        initPermissionViewModelCallback()
    }

    override fun onDestroy() {
        billingManager.destroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isLocationPermissionCheck) {
            permissionViewModel.checkPermission(PermissionViewModel.TYPE_LOCATION)
        }
    }

    private fun init() {
        pushCheck(intent)

        billingManager = BillingManager(this@MainActivity, this@MainActivity)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        pushCheck(intent)
    }

    private fun pushCheck(intent: Intent?){

        if (intent != null) {
            val bundle = intent.extras
            val eventUrl = bundle?.getString("event_url")
            if (!TextUtils.isEmpty(eventUrl) && eventUrl != "null" && eventUrl != null) {
                event_url = eventUrl
                viewModel.pushUrl(event_url)

                return
            }
            
            //스키마도 같이 처리
            val uri = intent.data
            if (uri != null) {
                if (uri.scheme == "smap_app") {
                    val invitationCode = uri.getQueryParameter("invitation_code")
                    invitation_code = invitationCode ?: ""
                    viewModel.deepLink(invitation_code)
                }
            }
        }
    }

    private fun initViewModelCallback() = viewModel.run {
        navigateToChannel.observeOnLifecycleDestroy(this@MainActivity) { item ->
            item?.let {
                if (!TextUtils.isEmpty(event_url) && event_url != "null") {
                    it.arguments.putString("event_url", event_url)
                }
                if (!TextUtils.isEmpty(invitation_code) && invitation_code != "null") {
                    it.arguments.putString("invitation_code", invitation_code)
                }
                navController.navigate(it)
            }
        }

        checkPermissionChannel.onEach {
            permissionViewModel.checkPermissions()
        }.observeInLifecycleStop(this@MainActivity)

        startForegroundServiceChannel.onEach {
            startBackgroundService()
        }.observeInLifecycleDestroy(this@MainActivity)

        stopForegroundServiceChannel.onEach {
            stopBackgroundService()
        }.observeInLifecycleDestroy(this@MainActivity)

        loginReceiveChannel.onEach {
            backgroundViewModel?.loginReceive()
        }.observeInLifecycleDestroy(this@MainActivity)

        logoutReceiveChannel.onEach {
            delPref(this@MainActivity, "mt_idx")
        }.observeInLifecycleDestroy(this@MainActivity)

        removePushUrlChannel.onEach {
            event_url = ""
        }.observeInLifecycleDestroy(this@MainActivity)

        removeDeepLinkChannel.onEach {
            invitation_code = ""
        }.observeInLifecycleDestroy(this@MainActivity)

        purchaseChannel.onEach { type ->

            val productDetails = billingManager.getProductDetail(BillingConstant.productId)
            var productOfferTags: String? = null
            if (productDetails != null) {
                if (type == "month") {
                    productOfferTags = billingManager.getProductDetailOfferToken(productDetails, BillingConstant.monthPlanId)
                } else if (type == "year") {
                    productOfferTags = billingManager.getProductDetailOfferToken(productDetails, BillingConstant.yearPlanId)
                }

                if (productOfferTags != null) {
                    billingManager.purchase(productDetails, productOfferTags)
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        showSnackBar("상품정보를 가져오는데 실패했습니다.")
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    showSnackBar("상품정보를 가져오는데 실패했습니다.")
                }
            }
        }.observeInLifecycleDestroy(this@MainActivity)

        purchaseCheckChannel.onEach {
            billingManager.checkSubscribed { purchase ->
                CoroutineScope(Dispatchers.Main).launch {
                    viewModel.purchaseCheckCheck(purchase)
                }
            }
        }.observeInLifecycleDestroy(this@MainActivity)

        showAdChannel.onEach {
            // 광고 띄우기
            showAds()
        }.observeInLifecycleDestroy(this@MainActivity)
    }

    private fun initPermissionViewModelCallback() = permissionViewModel.run {
        checkPermissionChannel.onEach { type ->
            if (type == PermissionViewModel.TYPE_LOCATION) {
                checkLocationPermissions()
            } else {
                checkPermissions()
            }
        }.observeInLifecycleStop(this@MainActivity)

        isGrantedPermission.onEach {
            if (it.isGranted) {
                viewModel.permissionsComplete()

                return@onEach
            }

            if (!it.isShowPopup) {
                requestPermission(this@MainActivity)

                return@onEach
            }

            AlertDialog.Builder(this@MainActivity).apply {
                setTitle(getString(R.string.app_name))
                setMessage(getString(R.string.require_permission_contents))
                setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                    viewModel.permissionsComplete()
                }
                setPositiveButton(getString(R.string.require_permission_button)) { dialog, _ ->
                    dialog.dismiss()
                    requirePermissionsSetting()
                }
                setCancelable(false)
                show()
            }
        }.observeInLifecycleStop(this@MainActivity)

        checkLocationPermissionsChannel.onEach {
            if (it.isGranted) {
                viewModel.permissionsComplete()
                backgroundViewModel?.locationRestart()
                return@onEach
            }

            if (!it.isShowPopup) {
                requestPermission(this@MainActivity)

                return@onEach
            }

            AlertDialog.Builder(this@MainActivity).apply {
                setTitle(getString(R.string.app_name))
                setMessage(getString(R.string.require_permission_contents))
                setNegativeButton(getString(R.string.later)) { dialog, _ ->
                    dialog.dismiss()
                    viewModel.isLocationPermissionCheck = false
                }
                setPositiveButton(getString(R.string.go_setting)) { dialog, _ ->
                    dialog.dismiss()
                    requirePermissionsSetting()
                }
                setCancelable(false)
                show()
            }
        }.observeInLifecycleStop(this@MainActivity)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionViewModel.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun requirePermissionsSetting() {
        try {
            val intent =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${packageName}"))
            settingDetails.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("REQ_DETAILS_SETTINGS", "권한 error -> ${e.printStackTrace()}")
            val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            settingDetails.launch(intent)
        }
    }

    private fun startBackgroundService() {
        //백그라운드 시작할때 연결 시작
        if (!BackgroundService.isRunning(this@MainActivity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this@MainActivity, BackgroundService::class.java))
            } else {
                startService(Intent(this@MainActivity, BackgroundService::class.java))
            }
        }

        backgroundViewModel = (application as MyApplication).attachBackgroundViewModel().apply {
//            locationList.observeOnLifecycleDestroy(this@MainActivity) {
//
//            }
//
//            lat.observeOnLifecycleDestroy(this@MainActivity) {
//
//            }
//
//            lng.observeOnLifecycleDestroy(this@MainActivity) {
//
//            }
//
//            stepCount.observeOnLifecycleDestroy(this@MainActivity) {
//
//            }
        }

        backgroundViewModel?.run {
            locationCheckChannel.onEach {
                AlertDialog.Builder(this@MainActivity).apply {
                    setTitle(getString(R.string.app_name))
                    setMessage(getString(R.string.location_enable_contents))
                    setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    setCancelable(false)
                    show()
                }
            }.observeInLifecycleDestroy(this@MainActivity)
        }
    }

    private fun stopBackgroundService() {
        if (BackgroundService.isRunning(this@MainActivity)) {
            stopService(Intent(this, BackgroundService::class.java))
        }

        (application as MyApplication).detachBackgroundViewModel()
        backgroundViewModel = null
    }
    //광고가져오기
    private var lastAdRequestTime = 0L
    private val MIN_AD_REQUEST_INTERVAL = 60000L // 1분
    private val MAX_RETRY_ATTEMPTS = 3
    private val INITIAL_RETRY_DELAY = 5000L // 5초
    private val adIds = listOf(R.string.admob_ad_id_pri, R.string.admob_ad_id_bak)
    private var currentAdIndex = 0

    private fun loadAds(isShow: Boolean, retryAttempt: Int = 0) {
        if (!canRequestAd()) {
            Log.d("ADLoadFail", "Ad request too frequent. Waiting...")
            viewModel.failAd("frequency")
            return
        }

        if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
            Log.d("ADLoadFail", "Max retry attempts reached")
            viewModel.failAd("maxRetry")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            viewModel.showLoadingDialog()
        }

        val adRequest = AdRequest.Builder().build()
        val adId = getString(adIds[currentAdIndex])

        InterstitialAd.load(this, adId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                CoroutineScope(Dispatchers.Main).launch {
                    viewModel.hideLoadingDialog()
                }

                Log.d("ADLoadFail", "Failed to load ad: ${adError.message}")
                mInterstitialAd = null

                // 다음 광고 ID로 전환
                currentAdIndex = (currentAdIndex + 1) % adIds.size

                val nextRetryDelay = INITIAL_RETRY_DELAY * (1 shl retryAttempt)
                Handler(Looper.getMainLooper()).postDelayed({
                    loadAds(isShow, retryAttempt + 1)
                }, nextRetryDelay)
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d("ADLoaded", "Ad loaded successfully")
                CoroutineScope(Dispatchers.Main).launch {
                    viewModel.hideLoadingDialog()
                }

                mInterstitialAd = interstitialAd

                mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("AdDismissed", "Ad dismissed fullscreen content.")
                        mInterstitialAd = null
                        loadAds(false, 0) // 새 광고 미리 로드
                        viewModel.endAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d("AdShowFailed", "Ad failed to show: ${adError.message}")
                        mInterstitialAd = null
                        viewModel.failAd("show")
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("AdShowed", "Ad showed fullscreen content.")
                    }
                }

                if (isShow) {
                    showAds()
                }
            }
        })
    }

//    private fun loadAds(isShow: Boolean, retryAttempt: Int = 0) {
//        if (!canRequestAd()) {
//            Log.d("ADLoadFail", "Ad request too frequent. Waiting...")
//            viewModel.failAd("frequency")
//            return
//        }
//
//        if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
//            Log.d("ADLoadFail", "Max retry attempts reached")
//            viewModel.failAd("maxRetry")
//            return
//        }
//
//        CoroutineScope(Dispatchers.Main).launch {
//            viewModel.showLoadingDialog()
//        }
//
//        val adRequest = AdRequest.Builder().build()
//        val adId = getString(R.string.admob_ad_id) // 실 서비스용
//        // val adId = getString(R.string.admob_ad_id_test) // 테스트용
//
//        InterstitialAd.load(this, adId, adRequest, object : InterstitialAdLoadCallback() {
//            override fun onAdFailedToLoad(adError: LoadAdError) {
//                CoroutineScope(Dispatchers.Main).launch {
//                    viewModel.hideLoadingDialog()
//                }
//
//                Log.d("ADLoadFail", "Failed to load ad: ${adError.message}")
//                mInterstitialAd = null
//
//                val nextRetryDelay = INITIAL_RETRY_DELAY * (1 shl retryAttempt)
//                Handler(Looper.getMainLooper()).postDelayed({
//                    loadAds(isShow, retryAttempt + 1)
//                }, nextRetryDelay)
//            }
//
//            override fun onAdLoaded(interstitialAd: InterstitialAd) {
//                Log.d("ADLoaded", "Ad loaded successfully")
//                CoroutineScope(Dispatchers.Main).launch {
//                    viewModel.hideLoadingDialog()
//                }
//
//                mInterstitialAd = interstitialAd
//
//                mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
//                    override fun onAdDismissedFullScreenContent() {
//                        Log.d("AdDismissed", "Ad dismissed fullscreen content.")
//                        mInterstitialAd = null
//                        loadAds(false, 0) // 새 광고 미리 로드
//                        viewModel.endAd()
//                    }
//
//                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                        Log.d("AdShowFailed", "Ad failed to show: ${adError.message}")
//                        mInterstitialAd = null
//                        viewModel.failAd("show")
//                    }
//
//                    override fun onAdShowedFullScreenContent() {
//                        Log.d("AdShowed", "Ad showed fullscreen content.")
//                    }
//                }
//
//                if (isShow) {
//                    showAds()
//                }
//            }
//        })
//    }

    private fun canRequestAd(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAdRequestTime < MIN_AD_REQUEST_INTERVAL) {
            return false
        }
        lastAdRequestTime = currentTime
        return true
    }

    private fun showAds() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            Log.d("AdShow", "The interstitial ad wasn't ready yet.")
            viewModel.failAd("notReady")
        }
    }

    override fun onBillingManagerReady() {
        billingManager.getProductDetailList()
    }

    override fun onBillingManagerProductReady() {

    }

    override fun onSuccess(purchase: Purchase) {
        //Log.e(TAG, "onSuccess - ${purchase.originalJson}")
        viewModel.purchaseDone(purchase)
    }

    override fun onFailure(errorCode: Int) {
        Log.e(TAG, "onFailure - $errorCode")
    }
}