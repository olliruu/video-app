package com.example.videoapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.videoapp.network.ServerAPI
import java.util.Locale

open class BaseActivity: AppCompatActivity() {
    val serverAPI:ServerAPI
        get() = (application as MyApp).serverApi

    fun token():String? {
        return getSharedPreferences("pref", Context.MODE_PRIVATE).getString("token", null)
    }

    fun userId():Int {
        return getSharedPreferences("pref", Context.MODE_PRIVATE).getInt("id", -1)
    }

    fun changeLanguage(languageCode:String){
        getSharedPreferences("pref", Context.MODE_PRIVATE).edit().apply {
            putString("language", languageCode)
            apply()
        }
        setLanguage(languageCode)
    }

    private fun getLanguage(): String {
        return getSharedPreferences("pref",Context.MODE_PRIVATE).getString("language", "en")!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLanguage(getLanguage())
    }

    private fun setLanguage(language:String){
        applicationContext.resources.configuration.setLocale(Locale(language))
    }
}