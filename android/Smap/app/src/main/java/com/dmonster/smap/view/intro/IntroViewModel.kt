package com.dmonster.smap.view.intro

import com.dmonster.smap.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.zip
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(

) : BaseViewModel() {
    val timerFinish = MutableStateFlow(false)
    val permissionFinish = MutableStateFlow(false)
    val allFinish = timerFinish.zip(permissionFinish) { t, p ->
        t && p
    }
}