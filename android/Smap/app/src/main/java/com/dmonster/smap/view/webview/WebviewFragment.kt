package com.dmonster.smap.view.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.dmonster.smap.BuildConfig
import com.dmonster.smap.R
import com.dmonster.smap.base.BaseFragment
import com.dmonster.smap.databinding.FragmentWebviewBinding
import com.dmonster.smap.utils.allTmpFileDelete
import com.dmonster.smap.utils.getPref
import com.dmonster.smap.utils.observeInLifecycleDestroy
import com.dmonster.smap.utils.saveImageOnAboveAndroidQ
import com.dmonster.smap.utils.saveImageOnUnderAndroidQ
import com.dmonster.smap.utils.showSnackBar
import com.dmonster.smap.utils.webview.ChromeClient
import com.dmonster.smap.utils.webview.WebClient
import com.dmonster.smap.utils.webview.WebViewSettingHelper
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder

@AndroidEntryPoint
class WebviewFragment : BaseFragment<FragmentWebviewBinding, WebviewViewModel>() {
    private val helper: WebViewSettingHelper by lazy {
        WebViewSettingHelper(
            WebClient(), ChromeClient(), mainViewModel, viewModel
        )
    }
    private val hashKey = "518cbe9ed50bf7e72913eb6b5a5e5fc6a8b99d56200ebda3a5bb365dbdccbdf6"

    private var backKeyPressedTime: Long = 0
    private var webViewPageType = ""

    private var fileUploadMtIdx = ""

    //Crop 이미지
    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result: CropImageView.CropResult ->
        if (result.isSuccessful) {
            val cropped = BitmapFactory.decodeFile(result.getUriFilePath(requireContext(), true))
            cropped.let { mBitmap ->
                CoroutineScope(Dispatchers.IO).launch {
                    CoroutineScope(Dispatchers.Main).launch {
                        viewModel.showLoadingDialog()
                    }
                    val uriString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveImageOnAboveAndroidQ(requireContext(), mBitmap)
                    } else {
                        saveImageOnUnderAndroidQ(requireContext(), mBitmap)
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        viewModel.hideLoadingDialog()
                    }

                    //이미지 저장하면 업로드 시작
                    uriString?.let {
                        fileUpload(uriString)
                    }
                }
            }
        } else {
            val exception = result.error
            Log.e("cropImageLauncher", exception?.message.toString())
        }
    }
    //카메라 캡처
    private val activityResultPhoto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
            val extras = result.data?.extras
            extras?.let {mExtra ->
                CoroutineScope(Dispatchers.IO).launch {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mExtra.getParcelable("data", Bitmap::class.java)
                    } else {
                        mExtra.get("data") as Bitmap
                    }

                    bitmap?.let {mBitmap ->
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.showLoadingDialog()
                        }

                        val uriString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            saveImageOnAboveAndroidQ(requireContext(), mBitmap)
                        } else {
                            saveImageOnUnderAndroidQ(requireContext(), mBitmap)
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.hideLoadingDialog()
                        }
                        val file = File(uriString.toString())
                        cropSingleImage(Uri.fromFile(file))
                    }
                }
            }
        }
        return@registerForActivityResult
    }
    //갤러리 접근
    private val activityResultGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
            val selectedImage: Uri? = result.data?.data
            selectedImage?.let {uri ->
                binding.apply {
                    CoroutineScope(Dispatchers.IO).launch {
                        cropSingleImage(uri)
                    }
                }
            }
        }
        return@registerForActivityResult
    }

    @SuppressLint("MissingPermission")
    override fun init() {
        binding.lifecycleOwner = viewLifecycleOwner

        binding.webview.run {
            helper.init(this)

            var baseUrl = "${BuildConfig.BASE_URL}auth?mt_token_id=${getPref(context, "androidId")}"
            val eventUrl = arguments?.getString("event_url")
            val invitationCode = arguments?.getString("invitation_code")
            if (eventUrl != null) {
                if (!TextUtils.isEmpty(eventUrl) && eventUrl != "null") {
                    val encodeUrl = URLEncoder.encode(eventUrl, "UTF-8")
                    baseUrl += "&event_url=$encodeUrl"
                }
            }

            if (invitationCode != null) {
                if (!TextUtils.isEmpty(invitationCode) && invitationCode != "null") {
                    baseUrl += "&event_url=$invitationCode"
                }
            }

            val headerMap = HashMap<String, String>()
            headerMap["AUTH_SECRETKEY"] = hashKey

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    try {
                        baseUrl += "&mt_lat=${location.latitude}&mt_long=${location.longitude}"
                        loadUrl(baseUrl, headerMap)
                    } catch (e: Exception) {
                        showSnackBar(this, e.message.toString())
                        loadUrl(baseUrl, headerMap)
                    }
                }
                .addOnFailureListener { e ->
                    showSnackBar(this, e.message.toString())
                    loadUrl(baseUrl, headerMap)
                }
        }
        mainViewModel.isLocationPermissionCheck = true

        mainViewModel.startForegroundService()
        mainViewModel.removePushUrl()
        mainViewModel.removeDeepLink()
    }

    override fun initViewModelCallback(): Unit = viewModel.run {
        mainViewModel.backPressedChannel.onEach {
//            binding.webview.apply {
//                if (canGoBack()) {
//                    goBack()
//
//                    return@onEach
//                }
//
//                if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
//                    backKeyPressedTime = System.currentTimeMillis()
//                    showSnackBar(
//                        this, getString(R.string.main_back_pressed)
//                    )
//
//                } else if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
//                    requireActivity().finishAffinity()
//                }
//            }

            binding.webview.apply {
                if (webViewPageType != "index" && webViewPageType != "intro" && webViewPageType != "login") {
                    //백버튼 이벤트
                    loadUrl("javascript:self.backPress();")
                    return@onEach
                }

                if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
                    backKeyPressedTime = System.currentTimeMillis()
                    showSnackBar(
                        this, getString(R.string.main_back_pressed)
                    )

                } else if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
                    requireActivity().finishAffinity()
                }
            }
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        mainViewModel.pushUrlChannel.onEach { url ->
            if (!TextUtils.isEmpty(url)) {
                binding.webview.loadUrl(url)
            }
            mainViewModel.removePushUrl()
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        mainViewModel.deepLinkChannel.onEach { invitation_code ->
            if (!TextUtils.isEmpty(invitation_code)) {
                binding.webview.loadUrl("javascript:invite_code_insert('$invitation_code');")
            }
            mainViewModel.removeDeepLink()
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        mainViewModel.purchaseDoneChannel.onEach { purchase ->
            binding.webview.apply {
                val productId = purchase.products[0]
                val purchaseToken = purchase.purchaseToken
                val packageName = purchase.packageName
                val originalJson = purchase.originalJson
                val mt_idx = getPref(context, "mt_idx")

                loadUrl("javascript:f_member_receipt_done('$productId', '$purchaseToken', '$packageName', '$originalJson', '$mt_idx');")
            }
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        mainViewModel.purchaseCheckCheckChannel.onEach { purchase ->
            binding.webview.apply {
                val mt_idx = getPref(context, "mt_idx")
                if (purchase != null) {
                    val productId = purchase.products[0]
                    val purchaseToken = purchase.purchaseToken
                    val packageName = purchase.packageName
                    val originalJson = purchase.originalJson

                    loadUrl("javascript:f_member_receipt_check('$productId', '$purchaseToken', '$packageName', '$originalJson', '$mt_idx');")
                } else {
                    loadUrl("javascript:f_member_receipt_check('', '', '', '', '$mt_idx');")
                }
            }
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        mainViewModel.endAdChannel.onEach {
            binding.webview.apply {
                loadUrl("javascript:endAd();")
            }
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        mainViewModel.failAdChannel.onEach { errorType ->
            CoroutineScope(Dispatchers.Main).launch {
                binding.webview.apply {
                    loadUrl("javascript:failAd('$errorType');")
                }
            }
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        pageTypeChannel.onEach { page ->
            webViewPageType = page
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        openPhotoChannel.onEach { mt_idx ->
            fileUploadMtIdx = mt_idx
            this@WebviewFragment.openPhoto()
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        openAlbumChannel.onEach { mt_idx ->
            fileUploadMtIdx = mt_idx
            this@WebviewFragment.openAlbum()
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        urlClipBoardChannel.onEach { url ->
            val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("url", url)
            clipboardManager.setPrimaryClip(clipData)

            showSnackBar(getString(R.string.copy_clipboard_contents))
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        urlOpenSmsChannel.onEach { url ->
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("smsto:") // 문자 보낼 번호
            intent.putExtra("sms_body", url) // 문자의 내용
            startActivity(intent)
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        openShareChannel.onEach { content ->
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, content)
            intent.type = "text/plain"
            startActivity(Intent.createChooser(intent, "Share"))
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        openUrlBlankChannel.onEach { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        sessionRefreshChannel.onEach { url ->
            CoroutineScope(Dispatchers.IO).launch {
                var baseUrl = "${BuildConfig.BASE_URL}auth?mt_token_id=${getPref(requireContext(), "androidId")}"
                val encodeUrl = URLEncoder.encode(url, "UTF-8")
                baseUrl += "&event_url=$encodeUrl"

                val headerMap = HashMap<String, String>()
                headerMap["AUTH_SECRETKEY"] = hashKey

                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.webview.apply {
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { location ->
                                    try {
                                        baseUrl += "&mt_lat=${location.latitude}&mt_long=${location.longitude}"
                                        loadUrl(baseUrl, headerMap)
                                        //Log.e("test", "baseurl = $baseUrl")
                                    } catch (e: Exception) {
                                        showSnackBar(this, e.message.toString())
                                        loadUrl(baseUrl, headerMap)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    showSnackBar(this, e.message.toString())
                                    loadUrl(baseUrl, headerMap)
                                }
                        }
                    }
                }
            }
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        fileUploadSuccessChannel.onEach {
            //성공 전달
            binding.webview.apply {
                loadUrl("javascript:f_member_file_upload_done();")
            }
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        fileUploadFailChannel.onEach { msg ->
            binding.webview.apply {
                showSnackBar(this, msg)
            }
        }.observeInLifecycleDestroy(viewLifecycleOwner)

        apiLoadingChannel.onEach { isState ->
            if (isState) {
                viewModel.showLoadingDialog()
            } else {
                viewModel.hideLoadingDialog()
            }
        }.observeInLifecycleDestroy(viewLifecycleOwner)
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                binding.webview.apply {
                    loadUrl("javascript:location_refresh('$webViewPageType', '${location.latitude}', '${location.longitude}');")
                }
            }
            .addOnFailureListener { e ->
                binding.webview.apply {
                    loadUrl("javascript:location_refresh('$webViewPageType', '', '');")
                }
            }
    }

    private fun openPhoto() {
        allTmpFileDelete(requireContext())
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        activityResultPhoto.launch(intent)
    }

    private fun openAlbum() {
        allTmpFileDelete(requireContext())
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        activityResultGallery.launch(intent)
    }

    private fun fileUpload(uriString: String){
        val data = HashMap<String, Any>()
        data["mt_idx"] = fileUploadMtIdx

        val fileData = HashMap<String, Uri>()
        fileData["mt_file1"] = Uri.parse(uriString)

        viewModel.fileUpload(data, fileData)
    }

    private fun cropSingleImage(photoUriPath: Uri) {
        val cropImageOption = CropImageOptions()
        cropImageOption.imageSourceIncludeGallery = true
        cropImageOption.imageSourceIncludeCamera = false
        val cropImageContractOptions = CropImageContractOptions(photoUriPath, cropImageOption)
        cropImageLauncher.launch(cropImageContractOptions)
    }
}