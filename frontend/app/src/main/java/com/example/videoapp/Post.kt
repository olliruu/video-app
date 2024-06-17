package com.example.videoapp

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.videoapp.Utils.Companion.dateToString
import com.example.videoapp.network.CustomCallback
import com.example.videoapp.network.PostResponse
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Post : ConstraintLayout{
    private var name:TextView
    private var date:TextView
    private var content:TextView
    private var pp:ShapeableImageView
    private var viewPager:ViewPager2

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr:Int) : super(context, attrs, defStyleAttr){
        inflate(context, R.layout.post, this)
        name = findViewById(R.id.name)
        date = findViewById(R.id.date)
        content = findViewById(R.id.content)
        pp = findViewById(R.id.pp)
        viewPager = findViewById(R.id.viewPager)
    }

    fun getPost(postId:Int, adapter: MediaAdapter, holder: MediaAdapter.PostViewHolder){
        val activity = (adapter.activity as MainActivity)
        adapter.serverAPI.getPost(postId).enqueue(object: CustomCallback<PostResponse>() {
            override fun onSuccess(t: PostResponse) {
                setPost(t, activity)
                setOnLongClickListener {
                    if(activity.userId() == t.ownerId){
                        AlertDialog.Builder(context).apply {
                            setTitle(R.string.delete_post)
                            setMessage(R.string.delete_post_confirmation)
                            setNegativeButton(R.string.cancel) { _, _ -> }
                            setPositiveButton(R.string.delete) {_, _->
                                adapter.serverAPI.deletePost(postId).enqueue(object:
                                    Callback<ResponseBody> {
                                    override fun onResponse(
                                        p0: Call<ResponseBody>,
                                        p1: Response<ResponseBody>
                                    ) {
                                        adapter.notifyItemRemoved(holder.bindingAdapterPosition)
                                    }
                                    override fun onFailure(p0: Call<ResponseBody>, p1: Throwable) {}
                                })
                            }
                            show()
                        }
                        return@setOnLongClickListener true
                    }
                    false
                }
            }
        })
    }

    fun setPost(post:PostResponse, activity:MainActivity){
        name.text = post.name
        date.text = dateToString(post.createTime, context)
        content.text = post.content
        Utils.setPP(post.profilePicture, pp)
        viewPager.adapter = ImageAdapter(post.images)
        pp.setOnClickListener {
            activity.showProfile(post.ownerId)
        }
    }
}

class ImageAdapter(private var imageResourceList: MutableList<String> = mutableListOf(), private var imageUriList:MutableList<Uri> = mutableListOf()): RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var image = itemView as ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ImageView(parent.context).apply { layoutParams = ViewGroup.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)})
    }

    override fun getItemCount(): Int {
        return maxOf(imageResourceList.size, imageUriList.size)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(imageResourceList.size >0) {
            Picasso.get().load("${MyApp.BASE_URL}/files/${imageResourceList[position]}.jpg").into(holder.image)
        } else {
            Picasso.get().load(imageUriList[position]).into(holder.image)
        }
    }
}

