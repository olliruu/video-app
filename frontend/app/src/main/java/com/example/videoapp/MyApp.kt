package com.example.videoapp

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.videoapp.network.ServerAPI
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.logging.Logger

class MyApp: Application() {
    lateinit var serverApi: ServerAPI


    companion object {
        const val BASE_URL = "http://172.20.10.8:3000"
        var auth:String = ""
    }

    override fun onCreate() {
        super.onCreate()
        auth = getSharedPreferences("pref", Context.MODE_PRIVATE).getString("token", "")!!
        var interceptor = Interceptor { chain ->
            chain.proceed(chain.request().newBuilder().addHeader("Authorization",auth).build())
        }

        val client = OkHttpClient.Builder().addInterceptor(interceptor).retryOnConnectionFailure(true).build()
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .client(client)
            .build()
        serverApi = retrofit.create(ServerAPI::class.java)
    }
}