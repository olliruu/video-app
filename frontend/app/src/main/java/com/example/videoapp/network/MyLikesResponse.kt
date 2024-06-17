package com.example.videoapp.network

import com.google.gson.annotations.SerializedName

data class MyLikesResponse(
    @SerializedName("video_id")
    val videoId: Int,
    val title:String?,
    @SerializedName("video_create_time")
    val videoCreateTime: String?,
    val duration:Int?,
    @SerializedName("thumbnail_resource")
    val thumbnailResource:String?,
    @SerializedName("comment_id")
    val commentId: Int?,
    val content:String?,
    @SerializedName("comment_create_time")
    val commentCreateTime:String?,
    @SerializedName("user_id")
    val userId: Int,
    val name:String,
    @SerializedName("profile_picture")
    val profilePicture:String,
    @SerializedName("video_views")
    val views:Int?,
    @SerializedName("video_likes")
    val videoLikes:Int?,
    @SerializedName("video_dislikes")
    val videoDislikes:Int?,
    @SerializedName("comment_likes")
    val commentLikes:Int?,
    @SerializedName("comment_dislikes")
    val commentDislikes:Int?,
    @SerializedName("is_like")
    val isLike:Boolean
)
