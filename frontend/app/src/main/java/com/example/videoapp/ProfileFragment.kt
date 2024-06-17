package com.example.videoapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.videoapp.network.CustomCallback
import com.example.videoapp.network.Profile
import com.example.videoapp.network.Video
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ProfileFragment : BaseFragment() {

     private var profileId = -1
    lateinit var recyclerView: RecyclerView
    lateinit var pp: ShapeableImageView
    lateinit var name:TextView
    lateinit var videos:TextView
    lateinit var subscribers:TextView
    lateinit var views:TextView
    lateinit var description:TextView
    lateinit var subscribe: Button
    lateinit var chipGroup: ChipGroup
    lateinit var progressBar: ProgressBar
    lateinit var scrollView: ScrollView
    private lateinit var materialToolbar: MaterialToolbar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            profileId = it.getInt(TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        var layout = inflater.inflate(R.layout.fragment_profile, container, false)
        prepareViews(layout)
        serverAPI.getProfile(profileId).enqueue(object:CustomCallback<Profile>(){
            override fun onSuccess(p: Profile) {
                Utils.setPP(p.profilePicture, pp)

                name.text = p.name
                subscribers.text = getString(R.string.subscribers, p.subscriberCount)
                views.text = getString(R.string.views, p.viewCount)
                videos.text = getString(R.string.videos, p.videosCount)
                description.text = p.description
                recyclerView.adapter = MediaAdapter(p.medias, requireActivity())
                recyclerView.layoutManager = LinearLayoutManager(context)

                progressBar.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
                if(p.id == userId()){
                    subscribe.visibility = View.GONE
                } else {
                    var isSubscribed = p.isSubscribed
                    subscribe.text = if(p.isSubscribed) getString(R.string.unsubscribe) else getString(R.string.subscribe)
                    subscribe.setOnClickListener {
                        val call = when(isSubscribed){
                            true-> serverAPI.deleteSubscription(p.id)
                            false-> serverAPI.subscribe(p.id)
                        }
                        call.enqueue(object:CustomCallback<ResponseBody>(){})

                        isSubscribed = isSubscribed.not()
                        subscribe.text = if(isSubscribed) getString(R.string.unsubscribe) else getString(R.string.subscribe)
                        val newSubCount=if(p.isSubscribed==isSubscribed)p.subscriberCount else if(isSubscribed)p.subscriberCount+1 else p.subscriberCount-1

                        subscribers.text = getString(R.string.subscribers,newSubCount)
                    }
                }

                chipGroup.setOnCheckedStateChangeListener { group, _ ->
                    when(group.checkedChipId){
                        R.id.chip_newest -> recyclerView.adapter = MediaAdapter(p.medias, requireActivity())
                        R.id.chip_oldest -> recyclerView.adapter = MediaAdapter(p.medias.asReversed(), requireActivity())
                        R.id.chip_most_liked-> recyclerView.adapter = MediaAdapter(p.medias.sortedBy { it.likes }.toMutableList(), requireActivity())
                        R.id.chip_least_liked->recyclerView.adapter = MediaAdapter(p.medias.sortedBy {it.dislikes}.toMutableList(), requireActivity())
                    }
                }
            }
        })
        return layout
    }

    private fun prepareViews(view: View){
        pp = view.findViewById(R.id.image)
        name = view.findViewById(R.id.name)
        subscribers = view.findViewById(R.id.subscribers)
        videos = view.findViewById(R.id.videos)
        views = view.findViewById(R.id.views)
        description = view.findViewById(R.id.description)
        subscribe = view.findViewById(R.id.subscribe)
        chipGroup = view.findViewById(R.id.chip_group)
        recyclerView = view.findViewById(R.id.videolist)
        scrollView = view.findViewById(R.id.scrollView3)
        materialToolbar = view.findViewById(R.id.materialToolbar4)
        progressBar = view.findViewById(R.id.progressBar2)

        materialToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    companion object {
        fun newInstance(profileId:Int) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putInt(TAG, profileId)
                }
            }
        const val TAG =  "profile_id"
    }
}