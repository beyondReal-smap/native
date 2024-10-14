package com.dmonster.smap.utils

import android.os.SystemClock
import android.view.View

class Listener {
    abstract class OnSingleClickListener(private val minIntervalMS: Long = 1000) :
        View.OnClickListener {
        // 마지막으로 클릭한 시간
        private var mLastClickTime: Long = 0

        abstract fun onSingleClick(v: View?)
        override fun onClick(v: View?) { //현재 클릭한 시간
            val currentClickTime = SystemClock.uptimeMillis()
            // 이전에 클릭한 시간과 현재시간의 차이
            val elapsedTime = currentClickTime - mLastClickTime
            // 내가 정한 중복클릭시간 차이를 안넘었으면 클릭이벤트 발생못하게 return
            if (elapsedTime <= minIntervalMS) return
            // 마지막클릭시간 업데이트
            mLastClickTime = currentClickTime
            // 중복클릭시간 아니면 이벤트 발생
            onSingleClick(v)
        }
    }
}