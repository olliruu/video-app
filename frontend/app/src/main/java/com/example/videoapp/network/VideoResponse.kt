package com.example.videoapp.network

import com.google.gson.annotations.SerializedName

data class VideoResponse(
    val id: Int,
    @SerializedName("user_id")
    val ownerId: Int,
    @SerializedName("name")
    val ownerName:String,
    @SerializedName("thumbnail_resource")
    val thumbnail: String,
    @SerializedName("profile_picture")
    val profilePicture: String,
    @SerializedName("video_resource")
    val video: String,
    val title: String,
    val duration: Int,
    val likes: Int,
    val dislikes: Int,
    val subscriptions:Int,
    val views: Int,
    @SerializedName("create_time")
    val createTime:String,
    @SerializedName("visibility")
    val visibility:String,
    @SerializedName("is_liked")
    val isLiked:Boolean,
    @SerializedName("is_disliked")
    val isDisliked:Boolean,
    @SerializedName("is_subscribed")
    val isSubscribed:Boolean,
    val recommendations: MutableList<Media>
)
