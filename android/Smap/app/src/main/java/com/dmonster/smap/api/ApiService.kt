package com.dmonster.smap.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Url

interface ApiService {
    @POST
    @FormUrlEncoded
    suspend fun setPostData(@Url url: String, @FieldMap data: HashMap<String, Any>): Response<ResponseBody>

    @Multipart
    @POST
    suspend fun setPostFileData(@Url url: String, @PartMap data: HashMap<String, RequestBody>, @Part file: MultipartBody.Part?): Response<ResponseBody>

//    @Headers("accept: application/json")
//    @POST
//    @FormUrlEncoded
//    suspend fun setPostDataJson(@Url url: String, @FieldMap data: HashMap<String, Any>): Response<ResponseBody>

//    @Headers("accept: application/json", "content-type: application/json")
//    @POST
//    fun setPostJson(@Url url: String, @Body data: HashMap<String, Any>): Response<ResponseBody>
}