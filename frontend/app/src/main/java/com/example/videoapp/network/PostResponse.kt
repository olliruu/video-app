package com.example.videoapp.network

import com.google.gson.annotations.SerializedName

data class PostResponse(
    val id:Int,
    val content:String,
    @SerializedName("create_time")
    val createTime:String,
    @SerializedName("user_id")
    val ownerId:Int,
    val name:String,
    @SerializedName("profile_picture")
    val profilePicture:String,
    @SerializedName("is_liked")
    val isLiked:Boolean,
    @SerializedName("is_disliked")
    val isDisliked:Boolean,
    val likes:Int,
    val dislikes:Int,
    val comments:Int,
    val images:MutableList<String>
)
