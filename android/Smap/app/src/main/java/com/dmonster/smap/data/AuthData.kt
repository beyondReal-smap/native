package com.dmonster.smap.data

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class AuthData {
    @SerializedName("mt_idx")
    @Expose
    var mt_idx: String? = null

    // TODO
//    @SerializedName("mt_idx")
//    @Expose
//    var mt_idx: String? = null
}