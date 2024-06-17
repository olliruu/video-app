package com.example.videoapp.network

import com.google.gson.annotations.SerializedName

data class Media(
    val type:String,
    val id:Int,
    @SerializedName("create_time")
    val createTime:String,
    val likes:Int,
    val dislikes:Int,
    val comments:Int
)
