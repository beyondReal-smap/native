package com.dmonster.smap.utils

import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import coil.load
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation

@BindingAdapter(
    value = ["onSingleClick", "interval"], requireAll = false
)
fun onSingleClick(
    view: View,
    listener: View.OnClickListener? = null,
    interval: Long? = null,
) {
    if (listener != null) {
        view.setOnClickListener(object : Listener.OnSingleClickListener(interval ?: 1000L) {
            override fun onSingleClick(v: View?) {
                listener.onClick(v)
            }
        })
    } else {
        view.setOnClickListener(null)
    }
}

@BindingAdapter(
    value = [
        "coilSrc",
        "coilCircularCrop",
    ], requireAll = false
)
fun setImageResource(
    view: ImageView, @DrawableRes drawableRes: Int?, circularCrop: Boolean = false
) {
    if (drawableRes == null) return

    view.load(drawableRes) {
        if (circularCrop) transformations(CircleCropTransformation())
    }
}

@BindingAdapter(
    value = [
        "coilSrc",
        "coilCircularCrop",
    ], requireAll = false
)
fun setImageResource(
    view: ImageView,
    drawable: Drawable?,
    circularCrop: Boolean = false,
) {
    if (drawable == null) return

    view.load(drawable) {
        if (circularCrop) transformations(CircleCropTransformation())
    }
}

@BindingAdapter(
    value = [
        "coilSrc",
        "coilCircularCrop",
        "coilRoundedCorner",
    ], requireAll = false
)
fun setImageResource(
    view: ImageView,
    image: String?,
    circularCrop: Boolean = false,
    coilRoundedCorner: Boolean = false,
) {
    if (image == null) return

    view.load(image) {
        if (circularCrop) transformations(CircleCropTransformation())
        if (coilRoundedCorner) transformations(RoundedCornersTransformation())
    }
}

@BindingAdapter(
    value = ["setEmpty"]
)
fun setEmpty(
    view: EditText,
    setEmpty: Boolean,
) {
    if (setEmpty) {
        object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {}
            override fun onTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {
                view.text.find {
                    if (it.toString() == " ") {
                        view.setText(view.text.toString().trim())
                        view.setSelection(view.length())
                        true
                    }

                    false
                }
            }
        }.apply {
            view.addTextChangedListener(this)
        }
    }
}