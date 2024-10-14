package com.dmonster.smap.api

import com.dmonster.smap.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiProtocol {
    fun service(): ApiService {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_API_URL)
            .client(provideOkHttpClient(provideLoggingInterceptor()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun fileService(): ApiService {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_FILE_API_URL)
            .client(provideOkHttpClient(provideLoggingInterceptor()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun provideOkHttpClient(interceptor: HttpLoggingInterceptor): OkHttpClient {
        val b = OkHttpClient.Builder()
        b.addInterceptor(interceptor)
        b.connectTimeout(120, TimeUnit.SECONDS)
        b.readTimeout(120, TimeUnit.SECONDS)
        b.writeTimeout(120, TimeUnit.SECONDS)
        return b.build()
    }

    private fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return interceptor
    }
}