package com.example.videoapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.videoapp.network.SubscriptionsResponse
import com.example.videoapp.network.User
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SubscriptionsFragment : BaseFragment() {

    lateinit var users: RecyclerView
    lateinit var videos:RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.fragment_subscriptions, container, false)
        users = view.findViewById(R.id.profilesRecyclerView)
        videos = view.findViewById(R.id.videosRecylerView)
        var noSubscriptions = view.findViewById<TextView>(R.id.no_subscriptions)
        var progressBar = view.findViewById<ProgressBar>(R.id.subscriptionProgress)
        var nestedScrollView = view.findViewById<NestedScrollView>(R.id.nestedScrollView)

        serverAPI.getSubscriptions().enqueue(object: Callback<SubscriptionsResponse>{
            override fun onResponse(
                p0: Call<SubscriptionsResponse>,
                p1: Response<SubscriptionsResponse>
            ) {
                if(p1.body()?.userList.isNullOrEmpty().not()){
                    nestedScrollView.visibility = View.VISIBLE

                    var userlist = p1.body()!!.userList
                    var videolist = p1.body()!!.mediaList
                    users.adapter = UserAdapter(userlist, requireActivity())
                    users.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL,false)
                    videos.adapter = MediaAdapter(videolist, requireActivity())
                    videos.layoutManager = LinearLayoutManager(context)
                    videos.addItemDecoration(CustomDecoration(15))
                } else {
                    noSubscriptions.visibility = View.VISIBLE
                }
                progressBar.visibility = View.GONE
            }
            override fun onFailure(p0: Call<SubscriptionsResponse>, p1: Throwable) {
                throw p1
            }
        })
        return view
    }
}

class UserAdapter(private val dataset:MutableList<User>, private val activity: FragmentActivity): RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item,parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataset[position]
        holder.apply {
            name.text = data.name
            Utils.setPP(data.profilePicture, pp)
            holder.itemView.setOnClickListener {
                (activity as MainActivity).showProfile(data.id)
            }
        }
    }

    class ViewHolder(itemView:View): RecyclerView.ViewHolder(itemView){
        var pp: ShapeableImageView = itemView.findViewById(R.id.shapableImageView)
        var name: TextView = itemView.findViewById(R.id.textView)
    }
}