package com.example.videoapp.network

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class CustomCallback<T> : Callback<T> {
    override fun onResponse(p0: Call<T>, p1: Response<T>) {
        val data = p1.body()
        if(data != null){
            onSuccess(data)
        } else {
            onFail()
        }
    }

    open fun onSuccess(t:T){

    }

    override fun onFailure(p0: Call<T>, p1: Throwable) {
        throw p1
        Log.d("Ruuskanen", p1.message, p1)
        onFail()
    }

    open fun onFail(){

    }

}