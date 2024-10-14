package com.dmonster.smap.api

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class BaseResult<T> {
    @SerializedName("message")
    @Expose
    var message: String? = null
    @SerializedName("success")
    @Expose
    var success: String? = null
    @SerializedName("title")
    @Expose
    var title: String? = null

    @SerializedName("data")
    @Expose
    var data: T? = null
}