package com.example.videoapp.network

import com.google.gson.annotations.SerializedName

data class VideosResponse(
    val results: MutableList<Video>,
    @SerializedName("seed_value")
    val seedValue: Float,
    @SerializedName("page_number")
    val pageNumber:Int
)
