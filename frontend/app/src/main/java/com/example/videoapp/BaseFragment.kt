package com.example.videoapp

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import com.example.videoapp.network.ServerAPI

open class BaseFragment: Fragment() {
    val serverAPI :ServerAPI
        get() = (activity as BaseActivity).serverAPI

    fun userId():Int {
        return requireContext().getSharedPreferences("pref", Context.MODE_PRIVATE).getInt("id", -1)
    }

    fun setUserId(id:Int){
        with(requireContext().getSharedPreferences("pref", Context.MODE_PRIVATE).edit()){
            putInt("id", id)
            apply()
        }
    }

    fun token():String? {
        return requireContext().getSharedPreferences("pref",Context.MODE_PRIVATE).getString("token", null)
    }

    fun setToken(token:String?){
        with(requireContext().getSharedPreferences("pref", Context.MODE_PRIVATE).edit()){
            MyApp.auth = if(token.isNullOrBlank()) "" else token
            putString("token", token)
            apply()
        }
    }

    fun getLanguage(){

    }

    fun setLanguage(){

    }
}