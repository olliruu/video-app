package com.example.videoapp.network

import com.google.gson.annotations.SerializedName

data class Comment(
    val id:Int,
    @SerializedName("parent_id")
    val parentId:Int?,
    val content:String,
    @SerializedName("video_id")
    val videoId:Int,
    @SerializedName("create_time")
    val createTime:String,
    @SerializedName("user_id")
    val userId:Int,
    @SerializedName("profile_picture")
    val profilePicture:String,
    @SerializedName("name")
    val username:String,
    @SerializedName("is_liked")
    val isLiked:Boolean,
    @SerializedName("is_disliked")
    val isDisliked:Boolean,
    val likes:Int,
    val dislikes:Int,
    var replies:Int =0,
)
