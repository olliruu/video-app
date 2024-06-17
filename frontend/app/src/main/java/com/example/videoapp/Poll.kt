package com.example.videoapp

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videoapp.Utils.Companion.dateToString
import com.example.videoapp.network.CustomCallback
import com.example.videoapp.network.PollResponse
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Poll :ConstraintLayout{
    private var name:TextView
    private var date:TextView
    private var content:TextView
    private var votes:TextView
    private var pp:ShapeableImageView
    private var pollLayout:LinearLayout
    private var pollItems = mutableListOf<PollItem>()
    lateinit var poll:PollResponse
    var hasVoted = false
    private lateinit var activity:MainActivity

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr:Int) : super(context, attrs, defStyleAttr) {
        inflate(context,R.layout.poll, this)
        name = findViewById(R.id.name)
        date = findViewById(R.id.date)
        content = findViewById(R.id.content)
        votes = findViewById(R.id.votes)
        pp = findViewById(R.id.pp)
        pollLayout = findViewById(R.id.poll_layout)
    }

    fun getPoll(pollId:Int, adapter: MediaAdapter, holder: MediaAdapter.PollViewHolder){
        activity = adapter.activity as MainActivity
        adapter.serverAPI.getPoll(pollId).enqueue(object: CustomCallback<PollResponse>() {
            override fun onSuccess(t: PollResponse) {
                setPoll(t, activity)
                setOnLongClickListener {
                    AlertDialog.Builder(context).apply {
                        setTitle(R.string.delete_poll)
                        setMessage(R.string.delete_poll_confirmation)
                        setNegativeButton(R.string.cancel) { _, _ -> }
                        setPositiveButton(R.string.delete) {_, _->
                            adapter.serverAPI.deletePoll(pollId).enqueue(object:
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
                    true
                }
            }
        })
    }

    fun setPoll(poll:PollResponse, activity: MainActivity){
        this.activity = activity
        this@Poll.poll = poll
        hasVoted = (poll.votedOrdinal == null).not()
        name.text = poll.name
        date.text = dateToString(poll.createTime, context)
        content.text = poll.content
        votes.text = context.getString(R.string.votes, poll.votes)
        Utils.setPP(poll.profilePicture, pp)
        pp.setOnClickListener {
            activity.showProfile(poll.ownerId)
        }
        for (option in poll.options){

            val item = PollItem(context, option, this@Poll, option.ordinal == poll.votedOrdinal).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0,0,0,4)
                }
            }
            addPollItem(item)
        }
    }

    fun selectedChild(pollItemId:Int?){

        if(pollItemId != null){
            hasVoted = true
            votes.text = context.getString(R.string.votes, if(poll.votedOrdinal == null) poll.votes +1 else poll.votes)
            activity.serverAPI.addPollVote(poll.id, pollItemId).enqueue(object: CustomCallback<ResponseBody>() {})
        } else {
            votes.text = context.getString(R.string.votes, if(poll.votedOrdinal == null) poll.votes else poll.votes -1)
            hasVoted = false
            activity.serverAPI.deletePollVote(poll.id).enqueue(object:CustomCallback<ResponseBody>(){})
        }
        Log.d("pollSelected", pollItemId.toString())
        pollItems.forEach {

            it.childChanged(pollItemId)
        }
    }

    fun getPollItems(): MutableList<PollItem> {
        return pollItems
    }

    private fun addPollItem(item:PollItem){
        pollItems.add(item)
        //pollLayout.children.forEachIndexed { index, _ -> }
        pollLayout.addView(item)
    }

    fun addPollItem(context: Context, content:String, image: Uri?): PollItem {
        val item = PollItem(context, content, image).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        addPollItem(item)
        return item
    }

    fun removePollItem(item: PollItem){
        pollItems.remove(item)
        pollLayout.removeView(item)
    }

    fun pollOptionsToJsonString():String {
        var array = JSONArray()
        getPollItems().forEachIndexed { index, pollItem ->
            array.put(index, JSONObject().apply {
                put("ordinal", index)
                put("name",pollItem.content)
            })
        }
        return array.toString()
    }
}