package com.dmonster.smap.api

import android.net.Uri
import android.text.TextUtils
import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.File

class BaseRepository {
    val apiService: ApiService by lazy {
        ApiProtocol.service()
    }

    val fileApiService: ApiService by lazy {
        ApiProtocol.fileService()
    }

    @WorkerThread
    fun requestPost(url: String, data: HashMap<String, Any>): Flow<ResponseBody> = flow {
        val response = apiService.setPostData(url, data)
        if (response.isSuccessful) {
            response.body()?.let { body ->
                emit(body)
            } ?: run {
                response.errorBody()?.let { errorBody ->
                    throw RuntimeException(errorBody.string())
                } ?: run {
                    throw RuntimeException("Api Error No Response")
                }
            }
        } else {
            throw RuntimeException("Api Error -> ${response.code()} - ${response.errorBody()}")
        }
    }

    @WorkerThread
    fun requestPostFileData(url: String, data: HashMap<String, Any>, file: HashMap<String, Uri>) = flow {
        val response = fileApiService.setPostFileData(url, setParam(data), createParamFile(file))
        if (response.isSuccessful) {
            response.body()?.let { body ->
                emit(body)
            } ?: run {
                response.errorBody()?.let { errorBody ->
                    throw RuntimeException(errorBody.string())
                } ?: run {
                    throw RuntimeException("Api Error No Response")
                }
            }
        } else {
            throw RuntimeException("Api Error -> ${response.code()} - ${response.errorBody()}")
        }
    }

    private fun setParam(data: HashMap<String, Any>): HashMap<String, RequestBody> {
        val requestBodyHashMap = HashMap<String, RequestBody>()
        val set: Set<*> = data.entries
        val iterator = set.iterator()
        while (iterator.hasNext()) {
            val (key1, value1) = iterator.next() as Map.Entry<*, *>
            val key = key1 as String
            val value = "" + value1
            val requestBody = value.toRequestBody(MultipartBody.FORM)
            requestBodyHashMap[key] = requestBody
        }
        return requestBodyHashMap
    }

    private fun createParamFile(file: HashMap<String, Uri>): MultipartBody.Part? {
        var img: Uri = Uri.parse("")
        var name = ""
        val set: Set<*> = file.entries
        val iterator = set.iterator()
        while (iterator.hasNext()) {
            val (key, value) = iterator.next() as Map.Entry<*, *>
            name = key as String
            img = value as Uri
        }
        return if (img.path != null && !TextUtils.isEmpty(img.path)) {
            val f = File(img.path!!)
            val fileReqBody = f.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData(name, f.name, fileReqBody)
        } else {
            null
        }
    }

//    @WorkerThread
//    fun requestPostDataJson(url: String, data: HashMap<String, Any>): Flow<ResponseBody> = flow {
//        val response = apiService.setPostDataJson(url, data)
//        if (response.isSuccessful) {
//            response.body()?.let { body ->
//                emit(body)
//            } ?: run {
//                response.errorBody()?.let { errorBody ->
//                    throw RuntimeException(errorBody.string())
//                } ?: run {
//                    throw RuntimeException("Api Error No Response")
//                }
//            }
//        } else {
//            throw RuntimeException("Api Error -> ${response.code()} - ${response.errorBody()}")
//        }
//    }

//    fun requestPostJson(url: String, data: HashMap<String, Any>): Flow<ResponseBody> = flow {
//        val response = apiService.setPostJson(url, data)
//        if (response.isSuccessful) {
//            response.body()?.let { body ->
//                emit(body)
//            } ?: run {
//                response.errorBody()?.let { errorBody ->
//                    throw RuntimeException(errorBody.string())
//                } ?: run {
//                    throw RuntimeException("Api Error No Response")
//                }
//            }
//        } else {
//            throw RuntimeException("Api Error -> ${response.code()} - ${response.errorBody()}")
//        }
//    }
}