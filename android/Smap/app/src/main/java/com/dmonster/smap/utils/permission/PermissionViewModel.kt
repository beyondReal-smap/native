package com.dmonster.smap.utils.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.dmonster.smap.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : BaseViewModel() {

    private var firstRequestPermissions = true

    private val permissions = mutableMapOf<String, Boolean>()

    private var permissionType = TYPE_INTRO

    private val _checkPermissionChannel = Channel<Int>(Channel.CONFLATED)
    val checkPermissionChannel = _checkPermissionChannel.receiveAsFlow()

    private val _isGrantedPermission = Channel<CheckPermissionData>(Channel.CONFLATED)
    val isGrantedPermission = _isGrantedPermission.receiveAsFlow()

    private val _finishCheckPermissionsChannel = Channel<Unit>(Channel.CONFLATED)
    val finishCheckPermissionsChannel = _finishCheckPermissionsChannel.receiveAsFlow()

    fun checkPermission(type: Int) = viewModelScope.launch {
        permissionType = type
        _checkPermissionChannel.send(type)
    }

    fun finishCheckPermissions() = viewModelScope.launch {
        _finishCheckPermissionsChannel.send(Unit)
    }

    private val _checkLocationPermissionsChannel = Channel<CheckPermissionData>(Channel.CONFLATED)
    val checkLocationPermissionsChannel = _checkLocationPermissionsChannel.receiveAsFlow()

    fun checkLocationPermissions() = viewModelScope.launch {
        _checkLocationPermissionsChannel.send(
            CheckPermissionData(
                checkGrantedPermission(),
                true
            )
        )
    }

    fun checkPermissions() = viewModelScope.launch {
        _isGrantedPermission.send(
            CheckPermissionData(
                checkGrantedPermission(),
                false
            )
        )
    }

    fun onActivityResult() = viewModelScope.launch {
        _isGrantedPermission.send(
            CheckPermissionData(
                checkGrantedPermission(),
                true
            )
        )
    }

    fun onRequestPermissionsResult(
        requestCode: Int, requestPermissions: Array<out String>, grantResults: IntArray,
    ) = viewModelScope.launch {
        if (requestCode != REQ_PERMISSION) {
            return@launch
        }

        var grantedCnt = 0

        if (grantResults.isNotEmpty()) {
            grantedCnt = permissions.entries.filterIndexed { i, entry ->
                requestPermissions.contains(entry.key) && entry.value && grantResults[i] == PackageManager.PERMISSION_GRANTED
            }.size
        }

        _isGrantedPermission.send(
            CheckPermissionData(
                (grantedCnt == requestPermissions.size - permissions.filterValues { !it }.size),
                true
            )
        )
    }

    private fun checkGrantedPermission(): Boolean {
        permissions.clear()

        addPermissions()

        val permissionsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mapOf(
                Manifest.permission.POST_NOTIFICATIONS to false,
            )
        } else {
            mapOf(

            )
        }

        permissionsList.iterator().forEach {
            if (ContextCompat.checkSelfPermission(
                    context, it.key
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions[it.key] = it.value
            }
        }

        // 앱 실행 후 처음 권한 요청 시
        return if (firstRequestPermissions) {
            firstRequestPermissions = false
            permissions.isEmpty()
        } else {
            permissions.filterValues { it }.isEmpty()
        }
    }

    private fun addPermissions() {
        val permissionsList = when (permissionType) {
            TYPE_INTRO ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    mapOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION to true,
                        Manifest.permission.ACCESS_FINE_LOCATION to true,
                        Manifest.permission.POST_NOTIFICATIONS to false,
                        Manifest.permission.ACTIVITY_RECOGNITION to true,
                        Manifest.permission.CAMERA to true,
                        Manifest.permission.ACCESS_MEDIA_LOCATION to true,
                        Manifest.permission.READ_MEDIA_IMAGES to true,
//                        Manifest.permission.SEND_SMS to true,
                        Manifest.permission.FOREGROUND_SERVICE_LOCATION to true
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mapOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION to true,
                    Manifest.permission.ACCESS_FINE_LOCATION to true,
                    Manifest.permission.POST_NOTIFICATIONS to false,
                    Manifest.permission.ACTIVITY_RECOGNITION to true,
                    Manifest.permission.CAMERA to true,
                    Manifest.permission.ACCESS_MEDIA_LOCATION to true,
                    Manifest.permission.READ_MEDIA_IMAGES to true,
//                    Manifest.permission.SEND_SMS to true
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mapOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION to true,
                    Manifest.permission.ACCESS_FINE_LOCATION to true,
                    Manifest.permission.ACTIVITY_RECOGNITION to true,
                    Manifest.permission.CAMERA to true,
                    Manifest.permission.ACCESS_MEDIA_LOCATION to true,
                    Manifest.permission.READ_EXTERNAL_STORAGE to true,
//                    Manifest.permission.SEND_SMS to true
                )
            } else {
                mapOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION to true,
                    Manifest.permission.ACCESS_FINE_LOCATION to true,
                    Manifest.permission.CAMERA to true,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE to true,
                    Manifest.permission.READ_EXTERNAL_STORAGE to true,
//                    Manifest.permission.SEND_SMS to true
                )
            }

            TYPE_LOCATION -> mapOf(
                Manifest.permission.ACCESS_COARSE_LOCATION to true,
                Manifest.permission.ACCESS_FINE_LOCATION to true,
            )

//            TYPE_BACKGROUND_LOCATION ->  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                mapOf(
//                    Manifest.permission.ACCESS_BACKGROUND_LOCATION to true,
//                )
//            } else {
//                mapOf()
//            }

            TYPE_NOTIFICATION ->  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mapOf(
                    Manifest.permission.POST_NOTIFICATIONS to false
                )
            } else {
                mapOf()
            }

            else -> mapOf()
        }

        permissionsList.iterator().forEach {
            if (ContextCompat.checkSelfPermission(
                    context, it.key
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions[it.key] = it.value
            }
        }
    }

    fun requestPermission(activity: Activity) {
        if (permissions.isNotEmpty()) {
            requestPermissions(
                activity, permissions.keys.toTypedArray(), REQ_PERMISSION
            )
        }
    }

    companion object {
        const val REQ_PERMISSION = 100
        const val TYPE_INTRO = 0x00
        const val TYPE_LOCATION = 0x00
        const val TYPE_BACKGROUND = 0x01
        //const val TYPE_BACKGROUND_LOCATION = 0x02
        const val TYPE_NOTIFICATION = 0x03
    }
}