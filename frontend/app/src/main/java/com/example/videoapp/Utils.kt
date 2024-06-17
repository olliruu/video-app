package com.example.videoapp

import android.content.Context
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.example.videoapp.network.ServerAPI
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date

class Utils {
    companion object {
        fun durationToString(duration: Int):String {
            return if(duration > 3600){
                "${duration/3600}:${(duration % 3600)/60}:${duration % 60}"
            } else {
                "${duration / 60}:${duration % 60}"
            }
        }

        fun setPP(resource:String, img:ShapeableImageView){
            Picasso.get().load("${MyApp.BASE_URL}/files/${resource}.jpg")
                .resize(230,230).placeholder(R.drawable.person)
                .error(R.drawable.person).into(img)
        }

        fun dateToString(d:String?, context: Context):String {
            try{
                val fullDate = d!!.split('T')
                val date = fullDate[0].split('-')
                val time = fullDate[1].split(':')
                val otherDate = LocalDateTime.of(date[0].toInt(), date[1].toInt(), date[2].toInt(), time[0].toInt(), time[1].toInt(), time[2].toInt())
                val currentDate = LocalDateTime.now()
                if(currentDate.year > otherDate.year){
                    val count = currentDate.year - otherDate.year
                    return context.resources.getQuantityString(R.plurals.year_ago, count, count)
                } else if(currentDate.monthValue > otherDate.monthValue) {
                    val count = currentDate.monthValue - otherDate.monthValue
                    return context.resources.getQuantityString(R.plurals.month_ago, count, count)
                } else if(currentDate.dayOfMonth > otherDate.dayOfMonth) {
                    val count = currentDate.dayOfMonth - otherDate.dayOfMonth
                    return context.resources.getQuantityString(R.plurals.day_ago, count, count)
                } else if(currentDate.hour > otherDate.hour) {
                    val count = currentDate.hour - otherDate.hour
                    return context.resources.getQuantityString(R.plurals.hour_ago, count, count)
                } else if(currentDate.minute > otherDate.minute) {
                    val count = currentDate.minute - otherDate.minute
                    return context.resources.getQuantityString(R.plurals.minute_ago, count, count)
                }  else if (currentDate.second > otherDate.second) {
                    val count = currentDate.second - otherDate.second
                    return context.resources.getQuantityString(R.plurals.second_ago, count, count)
                } else
                    return ""

            } catch (e:Exception){
                Log.d("dateToString", e.stackTraceToString())
                return ""
            }
        }

        fun imageReverseRotation(img: File):Float {
            return (360 - ExifInterface(img).rotationDegrees).toFloat()
        }
        fun imageReverseRotation(img: InputStream):Float {
            return (360 - ExifInterface(img).rotationDegrees).toFloat()
        }

        fun commentLikeAction(status:Like, serverAPI: ServerAPI, like:MaterialButton, dislike:MaterialButton,
                  likeCount:TextView, dislikeCount:TextView, originalLikeStatus:Like, context: Context,
                  id:Int, likes:Int, dislikes:Int, type:Like):Like{
            var likeStatus = status
            var call: Call<ResponseBody>
            if(type == Like.LIKE){
                when(likeStatus){
                    Like.LIKE ->{
                        call = serverAPI.deleteLike("comment", id)
                        likeStatus = Like.EMPTY
                        like.icon = ContextCompat.getDrawable(context, R.drawable.thumb_up)
                        likeCount.text = if(originalLikeStatus == Like.LIKE)"${likes-1}" else "$likes"
                    }
                    Like.EMPTY -> {
                        call = serverAPI.like("comment", true, id)
                        likeStatus = Like.LIKE
                        like.icon = ContextCompat.getDrawable(context, R.drawable.thumb_up_filled)
                        likeCount.text = if(originalLikeStatus == Like.LIKE)"$likes" else "${likes + 1}"
                    }
                    Like.DISLIKE -> {
                        call = serverAPI.like("comment", true, id)
                        likeStatus = Like.LIKE
                        like.icon = ContextCompat.getDrawable(context, R.drawable.thumb_up_filled)
                        likeCount.text = if(originalLikeStatus == Like.LIKE)"$likes" else "${likes + 1}"
                        dislike.icon = ContextCompat.getDrawable(context, R.drawable.thumb_down)
                        dislikeCount.text = if(originalLikeStatus == Like.DISLIKE)"${dislikes -1}" else "$dislikes"
                    }
                }
            } else {
                when(likeStatus){
                    Like.LIKE ->{
                        call = serverAPI.like("comment", false, id)
                        likeStatus = Like.DISLIKE
                        like.icon = ContextCompat.getDrawable(context, R.drawable.thumb_up)
                        likeCount.text = if(originalLikeStatus == Like.LIKE)"${likes-1}" else "$likes"
                        dislike.icon = ContextCompat.getDrawable(context, R.drawable.thumb_down_filled)
                        dislikeCount.text = if(originalLikeStatus == Like.DISLIKE)"$dislikes" else "${dislikes +1}"
                    }
                    Like.EMPTY -> {
                        call = serverAPI.like("comment", false, id)
                        likeStatus = Like.DISLIKE
                        dislike.icon = ContextCompat.getDrawable(context, R.drawable.thumb_down_filled)
                        dislikeCount.text = if(originalLikeStatus == Like.LIKE)"$dislikes" else "${dislikes + 1}"
                    }
                    Like.DISLIKE -> {
                        call = serverAPI.deleteLike("comment", id)
                        likeStatus = Like.EMPTY
                        dislike.icon = ContextCompat.getDrawable(context, R.drawable.thumb_down)
                        dislikeCount.text = if(originalLikeStatus == Like.DISLIKE)"${dislikes -1}" else "$likes"
                    }
                }
            }
            call.enqueue(object:Callback<ResponseBody>{
                override fun onResponse(p0: Call<ResponseBody>, p1: Response<ResponseBody>) {}
                override fun onFailure(p0: Call<ResponseBody>, p1: Throwable) {}
            })
            return likeStatus
        }
    }
}