package com.example.videoapp


import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.videoapp.network.Comment
import com.example.videoapp.network.CustomCallback
import com.example.videoapp.network.Media
import com.example.videoapp.network.PollResponse
import com.example.videoapp.network.PostResponse
import com.example.videoapp.network.Video

import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyDialogFragment:DialogFragment() {
    lateinit var type:String
    lateinit var layout:View

    companion object{
        fun newInstance(type:String): MyDialogFragment{
            val fragment = MyDialogFragment()
            val args = Bundle()
            args.putString("type", type)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = requireArguments().getString("type", "")!!

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        layout = requireActivity().layoutInflater.inflate(R.layout.my_dialog, null)

        return AlertDialog.Builder(requireContext()).setView(layout).create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View { return layout }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var toolbar = view.findViewById<MaterialToolbar>(R.id.materialToolbar10)
        var recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(CustomDecoration(15))
        var dialogText = view.findViewById<TextView>(R.id.dialog_text)
        var serverAPI = (activity as BaseActivity).serverAPI
        when (type){
            MyDialogEnum.VIEW.name-> {
                toolbar.title = getString(R.string.watched_videos)
                serverAPI.myViews().enqueue(object: CustomCallback<MutableList<Media>>() {
                    override fun onSuccess(t: MutableList<Media>) {
                        if(t.isNotEmpty()){
                            recyclerView.adapter =MediaAdapter(t, requireActivity())
                        } else {
                            dialogText.visibility = View.VISIBLE
                            dialogText.text = getString(R.string.no_videos_liked)
                        }
                    }
                })
            }
            MyDialogEnum.LIKE.name->{
                toolbar.title = getString(R.string.liked_videos)
                serverAPI.myLikes().enqueue(object:CustomCallback<MutableList<Media>>(){
                    override fun onSuccess(t: MutableList<Media>) {
                        if(t.isNotEmpty()){
                            recyclerView.adapter =LikesAdapter(t, requireActivity())

                        } else {
                            dialogText.visibility = View.VISIBLE
                            dialogText.text = getString(R.string.no_videos_liked)
                        }
                    }
                })            }
            MyDialogEnum.DISLIKE.name->{
                toolbar.title = getString(R.string.disliked_videos)
                serverAPI.myDislikes().enqueue(object:CustomCallback<MutableList<Media>>(){
                    override fun onSuccess(t: MutableList<Media>) {
                        if(t.isNotEmpty()){
                            recyclerView.adapter =LikesAdapter(t, requireActivity())
                        } else {
                            dialogText.visibility = View.VISIBLE
                            dialogText.text = getString(R.string.no_videos_liked)
                        }
                    }
                })
            }
        }
    }
}

enum class MyDialogEnum {
    VIEW,LIKE,DISLIKE
}

class LikesAdapter(val dataset:MutableList<Media>, private val activity: FragmentActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class VideoViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var video = itemView as VideoPreview
    }
    class CommentViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView){
        var pp: ShapeableImageView = itemView.findViewById(R.id.pp)
        var name: TextView = itemView.findViewById(R.id.name)
        var message: TextView = itemView.findViewById(R.id.message)
        var date: TextView = itemView.findViewById(R.id.date)
        var likeButton: MaterialButton = itemView.findViewById(R.id.like_button)
        var likeCount: TextView = itemView.findViewById(R.id.like_count)
        var dislikeButton: MaterialButton = itemView.findViewById(R.id.dislike_button)
        var dislikeCount: TextView = itemView.findViewById(R.id.dislike_count)
    }

    class PollViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var poll = itemView as Poll
    }

    class PostViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var post = itemView as Post
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            Type.VIDEO.ordinal-> VideoViewHolder(VideoPreview(parent.context).apply { layoutParams = ViewGroup.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT) })
            Type.COMMENT.ordinal-> CommentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.comment, parent, false))
            Type.POLL.ordinal-> PollViewHolder(Poll(parent.context).apply { layoutParams = ViewGroup.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)})
            else-> PostViewHolder(Post(parent.context).apply { layoutParams.apply { height = LayoutParams.WRAP_CONTENT;width = LayoutParams.MATCH_PARENT } })
        }
    }

    override fun getItemViewType(position: Int): Int {
        return Type.valueOf(dataset[position].type.uppercase()).ordinal
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = dataset[position]
        when(getItemViewType(position)){
            Type.VIDEO.ordinal->{
                (holder as VideoViewHolder).apply {
                    (activity as MainActivity).serverAPI.getVideoPreview(data.id).enqueue(object:CustomCallback<Video>(){
                        override fun onSuccess(t: Video) {
                            video.setVideoPreview(t, activity)
                        }
                    })
                }
            }
            Type.COMMENT.ordinal->{
                (holder as CommentViewHolder).apply {
                    (activity as MainActivity).serverAPI.getComment(data.id).enqueue(object: CustomCallback<Comment>(){
                        override fun onSuccess(t: Comment) {
                            name.text = t.username
                            message.text = t.content
                            date.text = Utils.dateToString(t.createTime, activity)
                            likeCount.text = "${t.likes}"
                            dislikeCount.text = "${t.dislikes}"
                            Utils.setPP(t.profilePicture, pp)
                            if(t.isLiked) {
                                likeButton.icon =
                                    ContextCompat.getDrawable(holder.itemView.context, R.drawable.thumb_up_filled)
                            }else if(t.isDisliked){
                                dislikeButton.icon =
                                    ContextCompat.getDrawable(holder.itemView.context, R.drawable.thumb_down_filled)
                            }
                            holder.itemView.setOnClickListener {
                                activity.startVideo(t.videoId)
                            }
                            pp.setOnClickListener {
                                activity.showProfile(t.userId)
                            }
                        }
                    })
                }
            }
            Type.POST.ordinal->{
                (holder as PostViewHolder).apply {
                    (activity as MainActivity).serverAPI.getPost(data.id).enqueue(object:CustomCallback<PostResponse>(){
                        override fun onSuccess(t: PostResponse) {
                            post.setPost(t, activity)
                        }
                    })
                }
            }
            Type.POLL.ordinal->{
                (holder as PollViewHolder).apply {
                    (activity as MainActivity).serverAPI.getPoll(data.id).enqueue(object:CustomCallback<PollResponse>(){
                        override fun onSuccess(t: PollResponse) {
                            poll.setPoll(t, activity)
                        }
                    })
                }
            }
        }
    }

    enum class Type{ VIDEO, COMMENT, POLL, POST }
}