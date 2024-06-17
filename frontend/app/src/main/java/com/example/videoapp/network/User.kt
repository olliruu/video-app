package com.example.videoapp.network

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("user_id")
    val id:Int,
    val name:String,
    @SerializedName("profile_picture")
    val profilePicture: String
    )
