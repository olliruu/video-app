package com.example.videoapp.network

import com.google.gson.annotations.SerializedName

data class SubscriptionsResponse(
    @SerializedName("user_list")
    val userList:MutableList<User>,
    @SerializedName("media_list")
    val mediaList:MutableList<Media>
)
