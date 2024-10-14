package com.dmonster.smap.data

import com.google.gson.annotations.SerializedName

data class MltGpsData(
    @SerializedName("mlt_lat")
    var mlt_lat: String,
    @SerializedName("mlt_long")
    var mlt_long: String,
    @SerializedName("mlt_speed")
    var mlt_speed: String,
    @SerializedName("mlt_accuacy")
    var mlt_accuacy: String,
    @SerializedName("mlt_gps_time")
    var mlt_gps_time: String
)