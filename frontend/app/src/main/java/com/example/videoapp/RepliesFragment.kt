package com.example.videoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.videoapp.network.Comment
import com.example.videoapp.network.CommentRequest
import com.example.videoapp.network.CustomCallback
import com.example.videoapp.network.ServerAPI
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


private const val PARAM = "parent_id"

class RepliesFragment : BaseFragment() {
    private var parentId = -1
    private lateinit var back:ImageView
    private lateinit var pp:ShapeableImageView
    private lateinit var name:TextView
    private lateinit var date:TextView
    private lateinit var content:TextView
    private lateinit var likeCount:TextView
    private lateinit var dislikeCount:TextView
    private lateinit var like:MaterialButton
    private lateinit var dislike:MaterialButton
    private lateinit var repliesList:RecyclerView
    private lateinit var reply:Button
    private lateinit var comment:TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            parentId = it.getInt(PARAM)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var layout = inflater.inflate(R.layout.fragment_replies, container, false)
        prepareViews(layout)
        serverAPI.replies(parentId).enqueue(object:Callback<MutableList<Comment>>{
            override fun onResponse(
                p0: Call<MutableList<Comment>>,
                p1: Response<MutableList<Comment>>
            ) {
                var data = p1.body()!!
                var parent = data.first { it.parentId == null }
                var replies = data.subList(1, data.size)
                Utils.setPP(parent.profilePicture, pp)
                name.text = parent.username
                date.text = Utils.dateToString(parent.createTime, requireContext())
                content.text = parent.content
                likeCount.text = "${parent.likes}"
                dislikeCount.text = "${parent.dislikes}"
                var adapter = RepliesAdapter(replies,serverAPI, requireActivity())
                repliesList.adapter = adapter

                var likeStatus = Like.EMPTY
                if(parent.isLiked){
                    likeStatus = Like.LIKE
                    like.icon = ContextCompat.getDrawable(requireContext(), R.drawable.thumb_up_filled)
                } else if(parent.isDisliked){
                    likeStatus = Like.DISLIKE
                    dislike.icon = ContextCompat.getDrawable(requireContext(), R.drawable.thumb_down_filled)
                }
                val originalLikeStatus = likeStatus

                like.setOnClickListener {
                    likeStatus = Utils.commentLikeAction(likeStatus, serverAPI, like, dislike, likeCount, dislikeCount,
                        originalLikeStatus, requireContext(), parent.id, parent.likes, parent.dislikes, Like.LIKE)
                }

                dislike.setOnClickListener {
                    likeStatus = Utils.commentLikeAction(likeStatus, serverAPI, like, dislike,
                        likeCount, dislikeCount, originalLikeStatus, requireContext(), parent.id,
                        parent.likes, parent.dislikes, Like.DISLIKE)
                }


                reply.setOnClickListener {
                    serverAPI.comment(CommentRequest(parent.id, "video",parent.videoId, comment.text.toString().trim())).enqueue(object:
                        CustomCallback<Comment>(){
                        override fun onSuccess(t: Comment) {
                            adapter.addComment(t)
                        }
                    })
                    comment.text?.clear()
                }
                pp.setOnClickListener {
                    (activity as MainActivity).showProfile(parent.userId)
                }
                back.setOnClickListener {
                    parentFragmentManager.popBackStack()
                }

            }
            override fun onFailure(p0: Call<MutableList<Comment>>, p1: Throwable) {}
        })
        return layout
    }

    private fun prepareViews(l:View){
        pp = l.findViewById(R.id.pp)
        back = l.findViewById(R.id.back)
        name = l.findViewById(R.id.name)
        date = l.findViewById(R.id.date)
        content = l.findViewById(R.id.content)
        likeCount = l.findViewById(R.id.like_count)
        dislikeCount = l.findViewById(R.id.dislike_count)
        like = l.findViewById(R.id.like)
        dislike = l.findViewById(R.id.dislike)
        repliesList = l.findViewById(R.id.replies)
        reply = l.findViewById(R.id.reply)
        comment = l.findViewById(R.id.text)
    }

    companion object {
        @JvmStatic
        fun newInstance(parentId: Int) =
            RepliesFragment().apply {
                arguments = Bundle().apply {
                    putInt(PARAM, parentId)
                }
            }
    }
}

class RepliesAdapter(private var dataset:MutableList<Comment>, var serverAPI: ServerAPI, private val activity: FragmentActivity): RecyclerView.Adapter<RepliesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.comment, parent, false)
        return ViewHolder(layout)
    }

    fun addComment(comment: Comment){
        dataset.add(comment)
        notifyItemInserted(itemCount-1)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var data = dataset[position]
        holder.apply {
            Utils.setPP(data.profilePicture, pp)
            name.text = data.username
            date.text = Utils.dateToString(data.createTime, itemView.context)
            message.text = data.content
            likeCount.text = "${data.likes}"
            dislikeCount.text = "${data.dislikes}"
            var likeStatus = Like.EMPTY
            if(data.isLiked){
                likeStatus = Like.LIKE
                like.icon = ContextCompat.getDrawable(holder.itemView.context, R.drawable.thumb_up_filled)
            } else if(data.isDisliked){
                likeStatus = Like.DISLIKE
                dislike.icon = ContextCompat.getDrawable(holder.itemView.context, R.drawable.thumb_down_filled)
            }
            var originalLikeStatus = likeStatus

            like.setOnClickListener {
                likeStatus = Utils.commentLikeAction(likeStatus, serverAPI, like, dislike,likeCount,
                    dislikeCount, originalLikeStatus, itemView.context, data.id, data.likes, data.dislikes, Like.LIKE)
            }
            dislike.setOnClickListener {
                likeStatus = Utils.commentLikeAction(likeStatus, serverAPI, like, dislike,likeCount,
                    dislikeCount, originalLikeStatus, itemView.context, data.id, data.likes, data.dislikes, Like.DISLIKE)
            }
            pp.setOnClickListener {
                (activity as MainActivity).showProfile(data.userId)
            }
        }
    }

    override fun getItemCount() = dataset.size

    class ViewHolder(v:View):RecyclerView.ViewHolder(v){
       var pp :ShapeableImageView = v.findViewById(R.id.pp)
        var name:TextView = v.findViewById(R.id.name)
        var date :TextView = v.findViewById(R.id.date)
        var message:TextView = v.findViewById(R.id.message)
        var likeCount:TextView = v.findViewById(R.id.like_count)
        var dislikeCount:TextView = v.findViewById(R.id.dislike_count)
        var like:MaterialButton = v.findViewById(R.id.like_button)
        var dislike:MaterialButton = v.findViewById(R.id.dislike_button)
    }
}