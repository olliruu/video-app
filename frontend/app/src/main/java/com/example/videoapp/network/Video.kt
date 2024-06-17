package com.example.videoapp.network

import com.google.gson.annotations.SerializedName

data class Video(
    val id: Int,
    @SerializedName("user_id")
    val ownerId: Int,
    @SerializedName("name")
    val ownerName:String,
    @SerializedName("thumbnail_resource")
    val thumbnail: String,
    @SerializedName("profile_picture")
    val profilePicture: String,
    val title: String,
    val duration: Int,
    val likes: Int,
    val dislikes: Int,
    val views: Int,
    @SerializedName("create_time")
    val createTime:String,
    @SerializedName("visibility")
    val visibility:String,
    val status:String,
    @SerializedName("video_resource")
    val videoResource:String,
    )
