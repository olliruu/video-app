package com.example.videoapp.network

data class PollOptionResponse(
    val id:Int,
    val name:String,
    val ordinal:Int,
    val resource:String,
    val votes:Int,
    val percentage:Int
)
