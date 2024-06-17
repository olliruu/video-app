package com.example.videoapp

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import com.example.videoapp.network.Media
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateFragment : BaseFragment() {

    lateinit var fab:FloatingActionButton
    lateinit var recyclerView: RecyclerView
    lateinit var toolbar: MaterialToolbar
    lateinit var noVideos : TextView
    lateinit var progressBar: ProgressBar
    lateinit var scrollView: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view =  inflater.inflate(R.layout.fragment_create, container, false)
        noVideos = view.findViewById(R.id.no_videos)
        toolbar = view.findViewById(R.id.materialToolbar)
        progressBar = view.findViewById(R.id.create_progress)
        scrollView = view.findViewById(R.id.scrollView2)
        fab = view.findViewById(R.id.fab)
        fab.setOnClickListener {
            AlertDialog.Builder(requireContext()).apply {
                setTitle(R.string.create_question)
                setItems(
                    arrayOf(getString(R.string.video))
                    //arrayOf(getString(R.string.video), getString(R.string.post), getString(R.string.poll))
                ) { _, i ->
                    when (i) {
                        0 -> findNavController().navigate(R.id.videoUploadFragment)
                        1 -> findNavController().navigate(R.id.postUploadFragment)
                        2 -> findNavController().navigate(R.id.pollUploadFragment)
                    }
                }
                show()
            }
        }

        recyclerView = view.findViewById(R.id.my_videos)
        serverAPI.myMedias().enqueue(object: Callback<MutableList<Media>>{
            override fun onResponse(p0: Call<MutableList<Media>>, resp: Response<MutableList<Media>>) {
                val data = resp.body()!!
                if(data.isNotEmpty()) {
                    recyclerView.adapter = MediaAdapter(data, requireActivity())
                    recyclerView.layoutManager = LinearLayoutManager(context)
                    recyclerView.addItemDecoration(CustomDecoration(15))
                }
                else {
                    noVideos.text = getString(R.string.no_medias)
                    noVideos.visibility = View.VISIBLE
                }
                progressBar.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
            }
            override fun onFailure(p0: Call<MutableList<Media>>, p1: Throwable) {
                Toast.makeText(context, "Failed to get videos", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
        })

        return view
    }
}

class MediaAdapter(private val myDataset: MutableList<Media>, val activity:FragmentActivity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val serverAPI = (activity as MainActivity).serverAPI

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            0-> VideoViewHolder(VideoPreview(activity).apply { layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT) })
            1-> PostViewHolder(Post(activity).apply { layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT) } )
            else-> PollViewHolder(Poll(activity).apply { layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT) })
        }
    }

    override fun getItemCount(): Int {
        return myDataset.size
    }

    override fun getItemViewType(position: Int): Int {
        return when(myDataset[position].type){
            "video"->0
            "post"->1
            "poll"->2
            else -> -1
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = myDataset[position]
        when(getItemViewType(position)){
            0-> (holder as VideoViewHolder).videoPreview.getVideoPreview(data.id, this, holder)
            1-> (holder as PostViewHolder).post.getPost(data.id, this, holder)
            2-> (holder as PollViewHolder).poll.getPoll(data.id, this, holder)
        }
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var videoPreview = itemView as VideoPreview
    }

    class PollViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var poll = itemView as Poll
    }

    class PostViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var post = itemView as Post
    }

}

class CustomDecoration(private val spaceSize: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val density = view.resources.displayMetrics.density.toInt()
        with(outRect) {
            if (parent.getChildAdapterPosition(view) == 0) {
                top = spaceSize * density
            }
            bottom = spaceSize * density
        }
    }
}