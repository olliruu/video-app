package com.example.videoapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.videoapp.Utils.Companion.setPP
import com.example.videoapp.network.Comment
import com.example.videoapp.network.CommentRequest
import com.example.videoapp.network.CustomCallback
import com.example.videoapp.network.VideoResponse
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Stack
import kotlin.math.roundToInt

class BottomSheet : BaseFragment(), BottomSheetCallbacks {

    private var videoId = -1
    private lateinit var player:ExoPlayer

    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var title: TextView
    private lateinit var date: TextView
    private lateinit var views: TextView
    private lateinit var username: TextView
    private lateinit var subscribers: TextView
    private lateinit var subscribe: MaterialButton
    private lateinit var pp: ShapeableImageView
    private lateinit var videos: RecyclerView
    private lateinit var comments: RecyclerView
    private lateinit var chipGroup: ChipGroup
    private lateinit var comment : TextInputEditText
    private lateinit var repliesContainer: FragmentContainerView
    private lateinit var titleCollapsed: TextView
    private lateinit var videoLayout: ConstraintLayout
    private lateinit var videoScroll: ScrollView
    private lateinit var behaviour:BottomSheetBehavior<ConstraintLayout>
    private lateinit var send:ImageView


    companion object {
        fun newInstance(videoId:Int):BottomSheet{
            val fragment  = BottomSheet()
            fragment.arguments = Bundle().apply {
                putInt("video_id", videoId)
            }
            return fragment
        }
        var videoStack = Stack<Int>()
        const val TAG = "bottom_sheet"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            videoId = getInt("video_id")
            videoStack.push(videoId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)
        var layout = inflater.inflate(R.layout.bottom_sheet, container, false) as CoordinatorLayout
        prepareViews(layout)

        serverAPI.getVideo(videoId).enqueue(object:CustomCallback<VideoResponse>(){
            override fun onSuccess(it: VideoResponse) {
                videos.adapter = MediaAdapter(it.recommendations, requireActivity())
                videos.layoutManager = LinearLayoutManager(context)
                videos.addItemDecoration(CustomDecoration(15))
                title.text = it.title
                titleCollapsed.text = it.title
                views.text  ="${it.views}"
                date.text = Utils.dateToString(it.createTime, requireContext())
                username.text = it.ownerName
                subscribers.text = getString(R.string.subscribers, it.subscriptions)
                if(it.ownerId != userId()){
                    subscribe.text = if(it.isSubscribed) getString(R.string.unsubscribe) else getString(R.string.subscribe)
                    var isSubbed = it.isSubscribed
                    subscribe.setOnClickListener { _ ->
                        var call : Call<ResponseBody>
                        if(isSubbed){
                            isSubbed = false
                            subscribe.text = getString(R.string.subscribe)
                            subscribers.text = if(it.isSubscribed) "${it.subscriptions-1}" else "${it.subscriptions}"
                            call = serverAPI.deleteSubscription(it.ownerId)
                        } else {
                            isSubbed = true
                            subscribe.text = getString(R.string.unsubscribe)
                            subscribers.text = if(it.isSubscribed) "${it.subscriptions}" else "${it.subscriptions+1}"
                            call = serverAPI.subscribe(it.ownerId)
                        }
                        call.enqueue(object:CustomCallback<ResponseBody>(){})
                    }
                }

                setPP(it.profilePicture, pp)
                pp.setOnClickListener {_->
                    (activity as MainActivity).showProfile(it.ownerId)
                }

                val mediaItem = MediaItem.fromUri("${MyApp.BASE_URL}/files/${it.video}/master.m3u8")
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        })

        serverAPI.comments("video", videoId).enqueue(object:CustomCallback<MutableList<Comment>>(){
            override fun onSuccess(t: MutableList<Comment>) {
                comments.adapter = CommentsAdapter(t, this@BottomSheet, requireActivity())
                comments.layoutManager = LinearLayoutManager(context)
                comments.addItemDecoration(CustomDecoration(15))
                send.setOnClickListener {_->
                    var c = comment.text.toString().trim()
                    if(c.isNotBlank()){
                        serverAPI.comment(CommentRequest(null, "video", videoId, c)).enqueue(object:CustomCallback<Comment>(){
                            override fun onSuccess(t: Comment) {
                                (comments.adapter as CommentsAdapter).addComment(t)
                            }
                        })
                        comment.text?.clear()
                    }
                }

                chipGroup.setOnCheckedStateChangeListener { group, _ ->
                    when(group.checkedChipId){
                        R.id.chip_newest -> comments.adapter = CommentsAdapter(t, this@BottomSheet, requireActivity())
                        R.id.chip_oldest -> comments.adapter = CommentsAdapter(t.asReversed(), this@BottomSheet, requireActivity())
                        R.id.chip_most_liked-> comments.adapter = CommentsAdapter(t.sortedBy { it.likes }.toMutableList(), this@BottomSheet, requireActivity())
                        R.id.chip_least_liked-> comments.adapter = CommentsAdapter(t.sortedBy {it.dislikes}.toMutableList(), this@BottomSheet, requireActivity())
                    }
                }
            }
        })
        return layout
    }

    private fun prepareViews(l:CoordinatorLayout){
        playerView = l.findViewById(R.id.player_view)
        player = ExoPlayer.Builder(requireContext()).build()
        playerView.player = player

        progressBar = l.findViewById(R.id.video_progress)
        title = l.findViewById(R.id.title)
        date = l.findViewById(R.id.date)
        views = l.findViewById(R.id.views)
        username = l.findViewById(R.id.username)
        subscribers = l.findViewById(R.id.subscribers)
        subscribe = l.findViewById(R.id.subscribe)
        pp = l.findViewById(R.id.pp)
        videos = l.findViewById(R.id.videos)
        comments = l.findViewById(R.id.comments)
        chipGroup = l.findViewById(R.id.chip_group)
        comment = l.findViewById(R.id.comment)
        repliesContainer = l.findViewById(R.id.replies_container)
        titleCollapsed = l.findViewById(R.id.title_small)
        videoLayout = l.findViewById(R.id.video_layout)
        videoScroll = l.findViewById(R.id.video_scroll)
        send = l.findViewById(R.id.send)

        videoLayout.setOnClickListener {
            if(behaviour.state == BottomSheetBehavior.STATE_COLLAPSED){
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        behaviour = BottomSheetBehavior.from(l.getChildAt(0) as ConstraintLayout)
        behaviour.state = BottomSheetBehavior.STATE_EXPANDED

        //update video player size when bottom sheet state changes
        behaviour.addBottomSheetCallback(object:BottomSheetBehavior.BottomSheetCallback(){
            @OptIn(UnstableApi::class) override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        videoLayout.layoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                        playerView.layoutParams.apply {
                            width = (170 * requireContext().resources.displayMetrics.density).roundToInt()
                            height = (100 * requireContext().resources.displayMetrics.density).roundToInt()
                        }
                        titleCollapsed.visibility = View.VISIBLE
                        videoScroll.visibility = View.GONE
                        repliesContainer.visibility = View.GONE
                        playerView.hideController()
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        parentFragmentManager.beginTransaction().remove(this@BottomSheet).commit()
                    }
                    else -> {
                        videoLayout.layoutParams.height = (230 * requireContext().resources.displayMetrics.density).roundToInt()

                        playerView.layoutParams.apply {
                            width = LinearLayout.LayoutParams.MATCH_PARENT
                            height = LinearLayout.LayoutParams.MATCH_PARENT
                        }
                        titleCollapsed.visibility = View.GONE
                        videoScroll.visibility = View.VISIBLE
                        repliesContainer.visibility = View.VISIBLE
                        playerView.showController()
                    }
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
    }

    override fun onCommentClicked(commentId: Int) {
        var fragment = RepliesFragment()
        fragment.arguments?.apply {
            putInt("parent_id",commentId)
        }
        parentFragmentManager.beginTransaction().replace(R.id.replies_container, fragment).commit()
    }

    override fun onVideoClicked(videoId: Int) {
        (activity as MainActivity).startVideo(videoId)
    }

    override fun onLikeClicked(likeId: Int, isLike: Boolean, action: Action) {
        val call = when(action){
            Action.ADD -> serverAPI.like("comment", isLike, likeId)
            Action.DELETE -> serverAPI.deleteLike("comment", likeId)
        }
        val callback = object:CustomCallback<ResponseBody>(){}
        call.enqueue(callback)
    }

    override fun isMyId(userId: Int): Boolean {
        return userId == (activity as BaseActivity).userId()
    }

    override fun deleteComment(commentId: Int) {
        serverAPI.deleteComment(commentId).enqueue(object:Callback<ResponseBody>{
            override fun onResponse(p0: Call<ResponseBody>, p1: Response<ResponseBody>) {}
            override fun onFailure(p0: Call<ResponseBody>, p1: Throwable) {}
        })
    }
}

interface BottomSheetCallbacks{
    fun onCommentClicked(commentId:Int){}
    fun onVideoClicked(videoId:Int){}
    fun onLikeClicked(likeId:Int, isLike:Boolean, action:Action){}
    fun isMyId(userId:Int):Boolean{return false}
    fun deleteComment(commentId: Int){}
}
enum class Action{ADD,DELETE}

class CommentsAdapter(private val dataset:MutableList<Comment>, private val callbacks: BottomSheetCallbacks, private val activity: FragmentActivity): RecyclerView.Adapter<CommentsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var pp :ShapeableImageView = itemView.findViewById(R.id.pp)
        var name :TextView = itemView.findViewById(R.id.name)
        var message :TextView = itemView.findViewById(R.id.message)
        var date :TextView = itemView.findViewById(R.id.date)

        var likeButton: MaterialButton =itemView.findViewById(R.id.like_button)
        var likeCount: TextView =itemView.findViewById(R.id.like_count)
        var dislikeButton: MaterialButton =itemView.findViewById(R.id.dislike_button)
        var dislikeCount: TextView =itemView.findViewById(R.id.dislike_count)
        var repliesButton:MaterialButton = itemView.findViewById(R.id.replies_button)
        var repliesText:TextView = itemView.findViewById(R.id.replies_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout =LayoutInflater.from(parent.context).inflate(R.layout.comment, parent, false)
        return ViewHolder(layout)
    }

    override fun getItemCount() = dataset.size

    fun addComment(comment: Comment){
        dataset.add(comment)
        notifyItemInserted(dataset.size)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        dataset[position].let {
            holder.apply {
                setPP(it.profilePicture, pp)
                name.text = it.username
                message.text = it.content
                date.text = Utils.dateToString(it.createTime, itemView.context)
                likeCount.text = "${it.likes}"
                dislikeCount.text = "${it.dislikes}"
                if(it.replies >0){
                    repliesButton.visibility = View.VISIBLE
                    repliesText.visibility = View.VISIBLE
                    repliesText.text = itemView.context.resources.getQuantityString(R.plurals.replies,it.replies,it.replies)
                }

                var likeStatus = Like.EMPTY
                if(it.isLiked) {
                    likeStatus = Like.LIKE
                    likeButton.icon =
                        ContextCompat.getDrawable(itemView.context, R.drawable.thumb_up_filled)
                }else if(it.isDisliked) {
                    likeStatus = Like.DISLIKE
                    dislikeButton.icon =
                        ContextCompat.getDrawable(itemView.context, R.drawable.thumb_down_filled)
                }
                var originalLikeStatus = likeStatus
                likeButton.setOnClickListener {
                    when(likeStatus){
                        Like.LIKE->{
                            likeStatus = Like.EMPTY
                            likeButton.icon = ContextCompat.getDrawable(itemView.context, R.drawable.thumb_up)
                            likeCount.text = if(originalLikeStatus == Like.LIKE)"${dataset[position].likes-1}" else "${dataset[position].likes}"
                            //serverAPI delete like
                            callbacks.onLikeClicked(dataset[position].id, true, Action.DELETE)
                        }
                        Like.EMPTY->{
                            likeStatus = Like.LIKE
                            likeButton.icon = ContextCompat.getDrawable(itemView.context, R.drawable.thumb_up_filled)
                            likeCount.text = if(originalLikeStatus == Like.LIKE)"${dataset[position].likes}" else "${dataset[position].likes+1}"
                            //serverAPI add like
                            callbacks.onLikeClicked(dataset[position].id, true, Action.ADD)
                        }
                        Like.DISLIKE->{
                            likeStatus = Like.LIKE
                            likeButton.icon = ContextCompat.getDrawable(itemView.context, R.drawable.thumb_up_filled)
                            dislikeButton.icon = ContextCompat.getDrawable(itemView.context, R.drawable.thumb_down)
                            likeCount.text = if(originalLikeStatus == Like.LIKE)"${dataset[position].likes}" else "${dataset[position].likes+1}"
                            dislikeCount.text = if(originalLikeStatus == Like.DISLIKE)"${dataset[position].dislikes -1}" else "${dataset[position].dislikes}"
                            //serverAPI add like
                            callbacks.onLikeClicked(dataset[position].id, true, Action.ADD)
                        }
                    }
                }

                dislikeButton.setOnClickListener {
                    when(likeStatus){
                        Like.LIKE->{
                            likeStatus = Like.DISLIKE
                            likeButton.icon = ContextCompat.getDrawable(itemView.context, R.drawable.thumb_up)
                            dislikeButton.icon = ContextCompat.getDrawable(itemView.context, R.drawable.thumb_down_filled)
                            likeCount.text = if(originalLikeStatus == Like.LIKE)"${dataset[position].likes-1}" else "${dataset[position].likes}"
                            dislikeCount.text = if(originalLikeStatus == Like.DISLIKE)"${dataset[position].dislikes}" else "${dataset[position].dislikes+1}"
                            //serverAPI add dislike
                            callbacks.onLikeClicked(dataset[position].id, false, Action.ADD)
                        }
                        Like.EMPTY->{
                            likeStatus = Like.DISLIKE
                            dislikeButton.icon = ContextCompat.getDrawable(itemView.context, R.drawable.thumb_down_filled)
                            dislikeCount.text = if(originalLikeStatus == Like.DISLIKE)"${dataset[position].likes}" else "${dataset[position].likes+1}"
                            //serverAPI add dislike
                            callbacks.onLikeClicked(dataset[position].id, false, Action.ADD)
                        }
                        Like.DISLIKE->{
                            likeStatus = Like.EMPTY
                            dislikeButton.icon = ContextCompat.getDrawable(itemView.context, R.drawable.thumb_down)
                            dislikeCount.text = if(originalLikeStatus == Like.DISLIKE)"${dataset[position].dislikes -1}" else "${dataset[position].dislikes}"
                            //serverAPI remove dislike
                            callbacks.onLikeClicked(dataset[position].id, false, Action.DELETE)
                        }
                    }
                }

                itemView.setOnClickListener {
                    callbacks.onCommentClicked(dataset[position].id)
                }
                itemView.setOnLongClickListener {
                    //delete comment if owned
                    if(callbacks.isMyId(dataset[position].userId)){
                        AlertDialog.Builder(itemView.context).setMessage(R.string.delete_comment)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                callbacks.deleteComment(dataset[position].id)
                                notifyItemRemoved(position)
                                Toast.makeText(
                                    itemView.context,
                                    R.string.comment_deleted,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.setNegativeButton(R.string.no) { _, _ -> }.show()
                    }
                     true
                }

                pp.setOnClickListener {_->
                    (activity as MainActivity).showProfile(it.userId)
                }
            }
        }
    }
}

enum class Like{LIKE, DISLIKE, EMPTY}