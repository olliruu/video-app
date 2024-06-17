package com.example.videoapp.network

import com.google.gson.annotations.SerializedName

data class Profile(
    val id:Int,
    val name :String,
    @SerializedName("profile_picture")
    val profilePicture :String,
    @SerializedName("bio")
    val description :String,
    @SerializedName("view_count")
    val viewCount :Int,
    @SerializedName("subscriber_count")
    val subscriberCount :Int,
    @SerializedName("video_count")
    val videosCount :Int,
    @SerializedName("poll_count")
    val pollCount :Int,
    @SerializedName("post_count")
    val postCount :Int,
    @SerializedName("is_subscribed")
    val isSubscribed :Boolean,
    @SerializedName("media")
    val medias :MutableList<Media>
)
