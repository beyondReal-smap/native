package com.dmonster.smap.utils.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.dmonster.smap.utils.room.data.StepData

@Dao
interface StepDataDao {
    @Insert
    fun insert(stepData: StepData)

    @Delete
    fun delete(stepData: StepData)

    @Query("UPDATE StepData SET step_cnt = :step_cnt, step_time_stamp = :step_time_stamp, step_during_time = :step_during_time WHERE id = :id")
    fun update(id: Int, step_cnt: Int, step_time_stamp: String, step_during_time: String)

    @Query("SELECT * FROM StepData WHERE step_date = :step_date")
    fun getDate(step_date: String): StepData?

    @Query("SELECT * FROM StepData")
    fun getAll(): List<StepData>
}