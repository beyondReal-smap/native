package com.dmonster.smap.view.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.dmonster.smap.R
import com.dmonster.smap.databinding.DialogLoadingBinding

class LoadingDialog(
    private val context: Context, root: ViewGroup? = null
) {
    private val builder = AlertDialog.Builder(context, R.style.AlertDialogTheme)

    private val binding: DialogLoadingBinding by lazy {
        DialogLoadingBinding.inflate(LayoutInflater.from(context), root, false)
    }

    private var dialog: AlertDialog? = null

    val isShowing: Boolean
        get() {
            dialog?.let {
                return it.isShowing
            }

            return false
        }

    fun create(): AlertDialog {
        builder.setView(binding.root)
        dialog = builder.create()

        return dialog!!
    }

    fun show() {
        create().show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}