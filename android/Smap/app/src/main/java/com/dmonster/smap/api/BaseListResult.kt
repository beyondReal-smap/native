package com.dmonster.smap.api

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class BaseListResult<T> {
    @SerializedName("tot_cnt")
    @Expose
    var tot_cnt: Int? = null
    @SerializedName("list")
    @Expose
    var list: ArrayList<T>? = null
}