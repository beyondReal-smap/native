package com.dmonster.smap.view.intro

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Log
import com.dmonster.smap.base.BaseFragment
import com.dmonster.smap.databinding.FragmentIntroBinding
import com.dmonster.smap.utils.observeOnLifecycleDestroy
import com.dmonster.smap.utils.observeOnLifecycleStop
import com.dmonster.smap.utils.setPref
import com.dmonster.smap.utils.showSnackBar
import com.google.firebase.messaging.FirebaseMessaging

class IntroFragment : BaseFragment<FragmentIntroBinding, IntroViewModel>() {
    private val timer: CountDownTimer = object : CountDownTimer(2000, 1000) {
        override fun onTick(p0: Long) {

        }

        override fun onFinish() {
            viewModel.timerFinish.value = true
        }
    }

    override fun init() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        timer.start()

        mainViewModel.checkPermission()
    }

    @SuppressLint("MissingPermission")
    override fun initViewModelCallback(): Unit = viewModel.run {
        allFinish.observeOnLifecycleStop(viewLifecycleOwner) {
            if (it) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e("FIREBASE", "Fetching FCM registration token failed", task.exception)
                        task.exception?.let { exception ->
                            showSnackBar(binding.splash, exception.message.toString())
                            //showsn(this@IntroActivity, exception.message.toString())
                        }

                        mainViewModel.fragmentNavigateTo(IntroFragmentDirections.actionIntroFragmentToWebviewFragment())
                        return@addOnCompleteListener
                    }

                    val token = task.result
                    Log.e("FIREBASE", "token - $token")
                    setPref(requireContext(), "androidId", token)

                    mainViewModel.fragmentNavigateTo(IntroFragmentDirections.actionIntroFragmentToWebviewFragment())

                }
            }
        }
    }

    override fun initMainViewModelCallback(): Unit = mainViewModel.run {
        super.initMainViewModelCallback()

        permissionsCompleteChannel.observeOnLifecycleDestroy(viewLifecycleOwner) {
            viewModel.permissionFinish.value = true
        }
    }
}