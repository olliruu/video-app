package com.example.videoapp

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.videoapp.network.CustomCallback
import com.example.videoapp.network.ProgressResponse
import com.example.videoapp.network.Video
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import okhttp3.internal.userAgent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VideoPreview :ConstraintLayout{

    private var thumbnail: ImageView
    private var pp: ShapeableImageView
    private var videoName: TextView
    private var likes: TextView
    private var dislikes: TextView
    private var channelName: TextView
    private var views: TextView
    private var duration: TextView
    private var progressBar:ProgressBar

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr:Int) : super(context, attrs, defStyleAttr) {
        inflate(context, R.layout.video_preview, this)
       thumbnail = findViewById(R.id.thumbnail)
        pp = findViewById(R.id.pp)
        videoName = findViewById(R.id.video_name)
        likes = findViewById(R.id.likes)
        dislikes = findViewById(R.id.dislikes)
        channelName = findViewById(R.id.channel_name)
        views = findViewById(R.id.views)
        duration = findViewById(R.id.duration)
        progressBar = findViewById(R.id.upload_video_progress)
    }

    fun getVideoPreview(videoId: Int, adapter: MediaAdapter, holder: MediaAdapter.VideoViewHolder){
        val activity = adapter.activity as MainActivity
        adapter.serverAPI.getVideoPreview(videoId).enqueue(object : CustomCallback<Video>() {
            override fun onSuccess(t: Video) {
                if(t.status == "done"){
                    setVideoPreview(t, activity)
                    setOnLongClickListener {
                        if(activity.userId() == t.ownerId){
                            AlertDialog.Builder(context).apply {
                                setTitle(R.string.delete_video)
                                setMessage(R.string.delete_video_confirmation)
                                setNegativeButton(R.string.cancel) { _, _ -> }
                                setPositiveButton(R.string.delete) {_, _->
                                    adapter.serverAPI.deleteVideo(videoId).enqueue(object:
                                        CustomCallback<ResponseBody>() {
                                        override fun onSuccess(t: ResponseBody) {
                                            adapter.notifyItemRemoved(holder.bindingAdapterPosition)
                                        }
                                    })
                                }
                                show()
                            }
                            return@setOnLongClickListener true
                        }
                        false
                    }
                } else {
                    if(t.ownerId == activity.userId())
                        setVideoUploading(t, activity)
                }
            }
        })
    }

    fun setVideoPreview(video:Video, activity:MainActivity){
        views.text = activity.getString(R.string.views, video.views)
        videoName.text = video.title
        likes.text = activity.getString(R.string.likes_count, video.likes)
        dislikes.text = activity.getString(R.string.dislikes_count, video.dislikes)
        channelName.text = activity.getString(R.string.channel_and_date, video.ownerName,
            Utils.dateToString(video.createTime, context)
        )
        duration.text = Utils.durationToString(video.duration)
        Utils.setPP(video.profilePicture, pp)
        Picasso.get().load("${MyApp.BASE_URL}/files/${video.thumbnail}.jpg").into(thumbnail)
        setOnClickListener {
            activity.startVideo(video.id)
        }

        pp.setOnClickListener {
            activity.showProfile(video.ownerId)
        }
    }

    fun setVideoUploading(v:Video, activity: MainActivity){
        views.text = activity.getString(R.string.views, 0)
        videoName.text = v.title
        likes.text = context.resources.getString(R.string.likes_count, 0)
        dislikes.text = context.resources.getString(R.string.dislikes_count, 0)
        channelName.text = context.resources.getString(R.string.channel_and_date, v.ownerName,
            Utils.dateToString(v.createTime, context)
        )
        duration.text = Utils.durationToString(v.duration)
        Utils.setPP(v.profilePicture, pp)
        Picasso.get().load("${MyApp.BASE_URL}/files/${v.thumbnail}.jpg").into(thumbnail)
        pp.setOnClickListener {
            activity.showProfile(v.ownerId)
        }
        progressBar.visibility = View.VISIBLE

        Thread {
            var keepGoing = true
            while(keepGoing){
                activity.serverAPI.checkProgress(v.videoResource).enqueue(object:CustomCallback<ProgressResponse>(){
                    override fun onSuccess(t: ProgressResponse) {
                        if(t.status == "done"){
                            setOnClickListener {
                                activity.startVideo(v.id)
                            }
                            progressBar.visibility = View.GONE
                            keepGoing = false
                        } else {
                            progressBar.setProgress(t.progress, true)
                            Thread.sleep(3000)
                        }
                    }

                    override fun onFail() {
                        keepGoing = false
                    }
                })
            }
        }
    }
}

