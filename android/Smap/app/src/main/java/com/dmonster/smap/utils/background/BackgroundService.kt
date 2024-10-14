package com.dmonster.smap.utils.background

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.*
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.Debug.getLocation
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.dmonster.smap.MyApplication
import com.dmonster.smap.R
import com.dmonster.smap.data.AuthData
import com.dmonster.smap.data.MltGpsData
import com.dmonster.smap.utils.getBattery
import com.dmonster.smap.utils.getPref
import com.dmonster.smap.utils.observeInLifecycleDestroy
import com.dmonster.smap.utils.room.data.StepData
import com.dmonster.smap.utils.room.database.SmapDatabase
import com.dmonster.smap.utils.setPref
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class BackgroundService : LifecycleService(), SensorEventListener {
    // 위치 사용 관련 - https://tomas-repcik.medium.com/locationrequest-create-got-deprecated-how-to-fix-it-e4f814138764
    private var database: SmapDatabase? = null

    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null

    var locHandler: Handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            backgroundViewModel.locationCheck()
        }
    }

    private val backgroundViewModel: BackgroundViewModel by lazy {
        (application as MyApplication).attachBackgroundViewModel()
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

//    private val fusedLocationClient: FusedLocationProviderClient by lazy {
//        LocationServices.getFusedLocationProviderClient(this)
//    }
//    private val locationRequest: LocationRequest by lazy {
//        LocationRequest.Builder(
//            Priority.PRIORITY_HIGH_ACCURACY, //정확도
//            GPS_REFRESH_INTERVAL //위치 업데이트 갱신 주기
//        ).apply {
//            setMinUpdateIntervalMillis(GPS_MIN_REFRESH_INTERVAL) //위치 업데이트 최소 갱신 시간 - 설정을 하지 않을시 원하는 갱신 주기 시간대 보다 빨리 들어올 수 있음
//            setMaxUpdateDelayMillis(GPS_MAX_REFRESH_INTERVAL) //위치 업데이트 최대 갱신 시간 - 설정을 하지 않을시 원하는 갱신 주기 시간대 보다 늦게 들어올 수 있음
//            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
//            setWaitForAccurateLocation(true) // 정확한 위치 받기
//        }.build()
//    }

    private fun createLocationRequest(): LocationRequest =
        LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, //정확도
            GPS_REFRESH_INTERVAL //위치 업데이트 갱신 주기
        ).apply {
            setMinUpdateIntervalMillis(GPS_MIN_REFRESH_INTERVAL) //위치 업데이트 최소 갱신 시간 - 설정을 하지 않을시 원하는 갱신 주기 시간대 보다 빨리 들어올 수 있음
            setMaxUpdateDelayMillis(GPS_MAX_REFRESH_INTERVAL) //위치 업데이트 최대 갱신 시간 - 설정을 하지 않을시 원하는 갱신 주기 시간대 보다 늦게 들어올 수 있음
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true) // 정확한 위치 받기
        }.build()

    //var savedLocation: Location? = null

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            Log.d("LOCATION REQUEST", "onLocationResult")

            backgroundViewModel.locationList.value = result.locations
            backgroundViewModel.lng.value = result.locations.last().longitude
            backgroundViewModel.lat.value = result.locations.last().latitude

            // API
            val mltGpsDataList = ArrayList<MltGpsData>()
//            result.locations.forEach { loc ->
//                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//                val date = Date(loc.time)
//                val dateTime = formatter.format(date)
//
//                val mltGpsData = MltGpsData(
//                    loc.latitude.toString(),
//                    loc.longitude.toString(),
//                    loc.speed.toString(),
//                    loc.accuracy.toString(),
//                    dateTime
//                )
//                mltGpsDataList.add(mltGpsData)
//            }

            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val loc = result.locations.last()
            val date = Date(loc.time)
            val dateTime = formatter.format(date)

            val mltGpsData = MltGpsData(
                loc.latitude.toString(),
                loc.longitude.toString(),
                loc.speed.toString(),
                loc.accuracy.toString(),
                dateTime
            )
            mltGpsDataList.add(mltGpsData)

            memberLocation(mltGpsDataList)
        }

        override fun onLocationAvailability(p0: LocationAvailability) {
            super.onLocationAvailability(p0)
            Log.d("LOCATION REQUEST", "onLocationAvailability ${p0.isLocationAvailable}")
            if (!p0.isLocationAvailable) {
                locHandler.sendEmptyMessageDelayed(10001, 30000)
            } else {
                locHandler.removeMessages(10001)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@BackgroundService)
        locationRequest = createLocationRequest()

        database = SmapDatabase.getInstance(applicationContext)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let { sensor ->
            stepCounterSensor = sensor
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }

        getLocation()
        initViewModelCallback()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, TAG, NotificationManager.IMPORTANCE_NONE
        )
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        chan.setShowBadge(false)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).setContentTitle(
            String.format(
                getString(R.string.background_notification_content_title),
                getString(R.string.app_name)
            )
        ).setContentText(
            String.format(
                getString(R.string.background_notification_content_text),
                getString(R.string.app_name)
            )
        ).setSmallIcon(R.mipmap.ic_launcher).setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)

        val notification = builder.build()

        // Foreground Service 시작!
        startForeground(NOTIFICATION_ID, notification)

        getLocationStart()

        return START_REDELIVER_INTENT
    }

    private fun initViewModelCallback() = backgroundViewModel.run {
        loginReceiveChannel.onEach {
            val data = HashMap<String, Any>()
            data["mt_token_id"] = getPref(this@BackgroundService, "androidId")
            auth(data)
        }.observeInLifecycleDestroy(this@BackgroundService)

        authSuccessChannel.onEach { authResult ->
            if (authResult != null) {
                Log.e("authSuccessChannel", "mt_idx -> ${authResult.mt_idx}")
                receiveAuth(authResult)
            }
        }.observeInLifecycleDestroy(this@BackgroundService)

        authFailChannel.onEach { msg ->
            Log.e("authFailChannel", "msg -> $msg")
        }.observeInLifecycleDestroy(this@BackgroundService)

        locationRestartChannel.onEach {
            startLocationTracking()
        }.observeInLifecycleDestroy(this@BackgroundService)
    }

    private fun getLocationStart() {
        val settingsClient = LocationServices.getSettingsClient(this@BackgroundService)
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            .setAlwaysShow(true)
        settingsClient.checkLocationSettings(builder.build()).addOnSuccessListener {
            // 위치 설정 켜짐
            Log.d(TAG, "LocationRequest > onSuccess")
        }.addOnFailureListener { e: Exception ->
            // 위치 설정 꺼짐
            e.printStackTrace()
        }

        changeLocationRequest()
    }

    //정확성 변경
    private fun changeLocationRequest(){
        if (ActivityCompat.checkSelfPermission(
                this@BackgroundService,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@BackgroundService,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationRequest = createLocationRequest()
        stopLocationTracking()
        startLocationTracking()
    }

    private fun startLocationTracking(){
        if (ActivityCompat.checkSelfPermission(
                this@BackgroundService,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@BackgroundService,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 마지막 위치 요청
        fusedLocationClient.lastLocation
            .addOnSuccessListener {
                it?.let { location ->
                    backgroundViewModel.lat.value = location.latitude
                    backgroundViewModel.lng.value = location.longitude
                }
            }

        //위치 정보 업데이트
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper(),
        )
    }

    private fun stopLocationTracking(){
        fusedLocationClient.flushLocations()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        LocationServices.getFusedLocationProviderClient(application.applicationContext)
            .removeLocationUpdates(locationCallback)
        (application as MyApplication).detachBackgroundViewModel()
        stopSelf()

        sensorManager.unregisterListener(this)
        stepCounterSensor = null

        super.onDestroy()
    }

    companion object {
        const val TAG = "BackgroundService"
        const val NOTIFICATION_CHANNEL_ID = "BackgroundServiceChannel"
        const val NOTIFICATION_ID = 9999
        const val GPS_CURRENT_DURATION = 2000L
        const val GPS_CURRENT_MAX_UPDATE = 1000L
        var GPS_REFRESH_INTERVAL = 20000L
        var GPS_MIN_REFRESH_INTERVAL = 20000L
        var GPS_MAX_REFRESH_INTERVAL = 20000L

        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager

            @Suppress("DEPRECATION") return manager.getRunningServices(Integer.MAX_VALUE)
                .any { it.service.className == BackgroundService::class.java.name }
        }
    }

    private fun receiveAuth(authResult: AuthData){
        setPref(this@BackgroundService, "mt_idx", authResult.mt_idx ?: "")
        // TODO
//        GPS_REFRESH_INTERVAL = 10000L
//        GPS_MIN_REFRESH_INTERVAL = 10000L
//        GPS_MAX_REFRESH_INTERVAL = 10000L
//        changeLocationRequest()

        val mltGpsDataList = ArrayList<MltGpsData>()
        if (backgroundViewModel.locationList.value.isNotEmpty()) {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = Date(backgroundViewModel.locationList.value.last().time)
            val dateTime = formatter.format(date)

            val mltGpsData = MltGpsData(
                backgroundViewModel.locationList.value.last().latitude.toString(),
                backgroundViewModel.locationList.value.last().longitude.toString(),
                backgroundViewModel.locationList.value.last().speed.toString(),
                backgroundViewModel.locationList.value.last().accuracy.toString(),
                dateTime
            )
            mltGpsDataList.add(mltGpsData)

            memberLocation(mltGpsDataList)
        }
    }

    private fun memberLocation(mltGpsDataList: ArrayList<MltGpsData>){
        CoroutineScope(Dispatchers.IO).launch {
            val mt_idx = getPref(this@BackgroundService, "mt_idx")
            if (!TextUtils.isEmpty(mt_idx) && mt_idx != "null") {
                val jsonData = HashMap<String, ArrayList<MltGpsData>>()
                jsonData["mlt_gps_data"] = mltGpsDataList

                val jsonStr = Gson().toJsonTree(jsonData, object : TypeToken<HashMap<String, ArrayList<MltGpsData>>>(){}.type)

                val formatter = SimpleDateFormat("yyyy-MM-dd")
                val nowLong = System.currentTimeMillis()
                val dateStr = formatter.format(nowLong)
                CoroutineScope(Dispatchers.IO).launch {
                    val stepData = database?.stepDataDao()?.getDate(dateStr)
                    val cnt = stepData?.step_cnt ?: 0

                    val data = HashMap<String, Any>()
                    data["mt_idx"] = mt_idx
                    data["mt_gps_data"] = jsonStr.toString()
                    data["mlt_battery"] = getBattery(this@BackgroundService)
                    data["mlt_fine_location"] = "N"
                    data["mlt_location_chk"] = "N"
                    data["mt_health_work"] = cnt.toString()
                    backgroundViewModel.memberLocation(data)
                }
            }
        }
    }

//    private fun loadCurrentLocation(listener: CurrentLocationListener?) {
//        if (ActivityCompat.checkSelfPermission(
//                this@BackgroundService,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                this@BackgroundService,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            listener?.onFail("Permission Denied")
//            return
//        }
//
//        fusedLocationClient.getCurrentLocation(createCurrentLocationRequest(), createCancellationToken())
//            .addOnSuccessListener { location ->
//                if (location != null) {
//                    listener?.onSuccess(location)
//                } else {
//                    listener?.onFail("Location is not available.")
//                }
//            }
//            .addOnFailureListener { e ->
//                listener?.onFail(e.message.toString())
//            }
//            .addOnCanceledListener {
//                listener?.onFail("Location is not available.")
//            }
//    }

//    private fun createCancellationToken(): CancellationToken =
//        object : CancellationToken() {
//            override fun onCanceledRequested(p0: OnTokenCanceledListener): CancellationToken  = CancellationTokenSource().token
//
//            override fun isCancellationRequested(): Boolean = false
//        }
//
//    private fun createCurrentLocationRequest() : CurrentLocationRequest =
//        CurrentLocationRequest.Builder()
//            .setDurationMillis(GPS_CURRENT_DURATION)
//            .setMaxUpdateAgeMillis(GPS_CURRENT_MAX_UPDATE)
//            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
//            .build()

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        CoroutineScope(Dispatchers.IO).launch {
            if (sensorEvent != null) {
                if (sensorEvent.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    val formatter = SimpleDateFormat("yyyy-MM-dd")
                    val nowLong = System.currentTimeMillis()
                    val dateStr = formatter.format(nowLong)

                    val tmpData: StepData? = database?.stepDataDao()?.getDate(dateStr)
                    if (tmpData == null) {
                        val data = StepData(
                            step_cnt = 1,
                            step_date = dateStr,
                            step_time_stamp = nowLong.toString(),
                            step_during_time = "0"
                        )
                        database?.stepDataDao()?.insert(data)
                    } else {
                        val lastLong: Long = tmpData.step_time_stamp.toLong()
                        var step_during_time: Long = tmpData.step_during_time.toLong()
                        var cnt: Int = tmpData.step_cnt
                        val resultLong = nowLong - lastLong
                        if (resultLong in 1..2000L) {
                            step_during_time += resultLong
                            cnt += 1
                        }
                        database?.stepDataDao()?.update(tmpData.id, cnt, nowLong.toString(), step_during_time.toString())
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }
}