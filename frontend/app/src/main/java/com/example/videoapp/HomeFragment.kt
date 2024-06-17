package com.example.videoapp


import android.annotation.SuppressLint
import android.app.SearchManager
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView


import androidx.appcompat.widget.SearchView.OnSuggestionListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.FragmentActivity

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.videoapp.network.SearchResponse
import com.example.videoapp.network.Video
import com.example.videoapp.network.VideosResponse
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : BaseFragment(){

    lateinit var recyclerView: RecyclerView
    lateinit var search: SearchView
    lateinit var toolbar: MaterialToolbar

    var callback = object:Callback<VideosResponse>{
        override fun onResponse(p0: Call<VideosResponse>, p1: Response<VideosResponse>) {
            if(p1.body()?.results.isNullOrEmpty().not()){
                val videos = p1.body()!!.results
                val seed = p1.body()!!.seedValue
                (recyclerView.adapter as VideoPageAdapter).replaceDataset(videos, seed, "search")
            } else {
                Toast.makeText(context, R.string.videos_fail, Toast.LENGTH_LONG).show()
            }
        }
        override fun onFailure(p0: Call<VideosResponse>, p1: Throwable) {
            Toast.makeText(context, R.string.videos_fail, Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        toolbar = view.findViewById(R.id.materialToolbar9)
        recyclerView = view.findViewById(R.id.videos)
        recyclerView.addOnScrollListener(object:RecyclerView.OnScrollListener(){
            var isLoading = false
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                var pos = (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                var adapter = recyclerView.adapter as VideoPageAdapter
                if(pos == adapter.dataset.size && isLoading.not()) {
                    var call = if(adapter.type =="feed")
                        serverAPI.feed(5, adapter.pageNumber, adapter.seed)
                    else
                        serverAPI.searchVideos(search.query as String,5,adapter.pageNumber,adapter.seed)
                    var callback = object:Callback<VideosResponse>{
                        override fun onResponse(p0:Call<VideosResponse>,p1:Response<VideosResponse>)
                        {
                            val videos = p1.body()!!.results
                            val page = p1.body()!!.pageNumber
                            adapter.updateDataset(videos, page)
                        }
                        override fun onFailure(p0: Call<VideosResponse>, p1: Throwable) {}
                    }
                    call.enqueue(callback)
                }
            }
        })

        recyclerView.adapter = VideoPageAdapter(requireActivity())
        recyclerView.layoutManager = LinearLayoutManager(context)
        search = view.findViewById(R.id.searchView)

        val adapter = SimpleCursorAdapter(context, R.layout.search_item, null,
            arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1), intArrayOf(R.id.search_item),
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)

        search.suggestionsAdapter = adapter

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                if(!p0.isNullOrBlank())
                    serverAPI.searchVideos(p0, 5, 1, null).enqueue(callback)
                return false
            }
            override fun onQueryTextChange(p0: String?): Boolean {
                val cursor = MatrixCursor(arrayOf(BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1))

                serverAPI.search(p0.orEmpty()).enqueue(object:Callback<Array<SearchResponse>>{
                    override fun onResponse(p0:Call<Array<SearchResponse>>,p1:Response<Array<SearchResponse>>){
                        p1.body()?.forEachIndexed { index, s ->
                            cursor.addRow(arrayOf(index, s.title))
                        }
                        adapter.changeCursor(cursor)
                    }
                    override fun onFailure(p0: Call<Array<SearchResponse>>, p1: Throwable) {
                        throw p1
                    }
                    })
                return true
            }
        })

        search.findViewById<SearchAutoComplete>(androidx.appcompat.R.id.search_src_text).threshold = 1

        search.setOnSuggestionListener(object:OnSuggestionListener{
            override fun onSuggestionSelect(p0: Int): Boolean { return false }
            override fun onSuggestionClick(p0: Int): Boolean {
                val cursor = search.suggestionsAdapter.getItem(p0) as Cursor
                val selection = cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1))
                search.setQuery(selection, false)
                serverAPI.searchVideos(selection, 5,1, null).enqueue(callback)

                return true
            }
        })

        serverAPI.feed(5, 1, null).enqueue(object : Callback<VideosResponse>{
            override fun onResponse(p0: Call<VideosResponse>, p1: Response<VideosResponse>) {
                if(p1.body()?.results != null){
                    val videos = p1.body()!!.results
                    val seed = p1.body()!!.seedValue
                    (recyclerView.adapter as VideoPageAdapter).replaceDataset(videos, seed, "feed")
                } else {
                    Toast.makeText(context, R.string.no_videos_available, Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(p0: Call<VideosResponse>, p1: Throwable) {
                Toast.makeText(requireContext(), R.string.videos_fail, Toast.LENGTH_LONG).show()
            }
        })
        return view
    }
}

class VideoPageAdapter(private val activity: FragmentActivity) : RecyclerView.Adapter<VideoPageAdapter.ViewHolder>(){
    var dataset = mutableListOf <Video>()
    var seed = 0.0f
    var pageNumber = 1
    var type = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.video_preview, parent, false)
        return ViewHolder(view)
    }

    fun replaceDataset(newDataset:MutableList<Video>, seed:Float,type:String) {
        dataset = newDataset
        this.seed = seed
        pageNumber = 1
        this.type = type
        notifyDataSetChanged()
    }

    fun updateDataset(itemsToAdd:MutableList<Video>, page:Int) {
        pageNumber = page
        var startIndex = itemCount
        dataset.addAll(itemsToAdd)
        notifyItemRangeInserted(startIndex, itemsToAdd.size)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataset[position]
        holder.apply {
            Utils.setPP(data.profilePicture, pp)
            Picasso.get().load("${MyApp.BASE_URL}/files/${data.thumbnail}.jpg").into(thumbnail)
            videoName.text = data.title
            likes.text = activity.getString(R.string.likes_count, data.likes)
            dislikes.text = activity.getString(R.string.dislikes_count, data.dislikes)
            channelName.text = data.ownerName
            views.text = data.views.toString()
            duration.text = data.duration.toString()
            pp.setOnClickListener {
                (activity as MainActivity).showProfile(data.ownerId)
            }
            itemView.setOnClickListener {
                (activity as MainActivity).startVideo(data.id)
            }
        }
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        var thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        var pp: ShapeableImageView = itemView.findViewById(R.id.pp)
        var videoName: TextView = itemView.findViewById(R.id.video_name)
        var likes: TextView = itemView.findViewById(R.id.likes)
        var dislikes: TextView = itemView.findViewById(R.id.dislikes)
        var channelName: TextView = itemView.findViewById(R.id.channel_name)
        var views: TextView = itemView.findViewById(R.id.channel_name)
        var duration: TextView = itemView.findViewById(R.id.duration)
    }
}