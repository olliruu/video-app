package com.example.videoapp.network

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


data class CommentRequest(
    @Expose
    @SerializedName("parent_id")
    val parentId:Int?,
    val type:String,
    @Expose
    @SerializedName("type_id")
    val typeId:Int,
    val content:String)
