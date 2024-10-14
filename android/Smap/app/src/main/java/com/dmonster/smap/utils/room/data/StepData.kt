package com.dmonster.smap.utils.room.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class StepData(
    var step_cnt: Int,
    var step_date: String,
    var step_time_stamp: String,
    var step_during_time: String
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}