package com.dmonster.smap

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.dmonster.smap.utils.background.BackgroundViewModel
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application(), ViewModelStoreOwner {

    private val mViewModelStore = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = mViewModelStore

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {

            }

            override fun onActivityStarted(p0: Activity) {

            }

            override fun onActivityResumed(p0: Activity) {
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.cancelAll()
            }

            override fun onActivityPaused(p0: Activity) {

            }

            override fun onActivityStopped(p0: Activity) {

            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {

            }

            override fun onActivityDestroyed(p0: Activity) {

            }

        })
    }

    internal fun attachBackgroundViewModel(): BackgroundViewModel {
        return ViewModelProvider(
            this, BackgroundViewModel.Factory(
                this,
            )
        )[BackgroundViewModel::class.java]
    }

    fun detachBackgroundViewModel() {
        mViewModelStore.clear()
    }
}