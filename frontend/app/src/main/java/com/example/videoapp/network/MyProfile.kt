package com.example.videoapp.network

import com.google.gson.annotations.SerializedName

data class MyProfile(
    @SerializedName("user_id")
    val id:Int,
    val name:String,
    @SerializedName("create_time")
    val createTime: String,
    @SerializedName("profile_picture")
    val profilePicture: String,
    val views: Int,
    val likes:Int,
    val dislikes: Int
)
