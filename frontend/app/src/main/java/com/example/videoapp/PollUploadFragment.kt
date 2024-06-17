package com.example.videoapp

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toFile
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.videoapp.network.CustomCallback
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class PollUploadFragment : BaseFragment() {

    lateinit var toolbar: MaterialToolbar
    lateinit var content: TextInputEditText
    lateinit var noPollItems: TextView
    lateinit var newPollItem: Button
    lateinit var upload: Button
    lateinit var pollItems: LinearLayout
    lateinit var preview: Poll

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var layout = inflater.inflate(R.layout.fragment_poll_upload, container, false)
        toolbar = layout.findViewById(R.id.create_poll_toolbar)
        content = layout.findViewById(R.id.content)
        noPollItems = layout.findViewById(R.id.no_poll_items_text)
        upload = layout.findViewById(R.id.upload_poll)
        newPollItem = layout.findViewById(R.id.new_poll_item_button)
        preview = layout.findViewById(R.id.preview_poll)
        pollItems = layout.findViewById(R.id.poll_items)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        upload.setOnClickListener {
            if(preview.getPollItems().size < 2 || preview.getPollItems().size > 4){
                Toast.makeText(context, R.string.wrong_poll_item_count, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            var body = MultipartBody.Builder().apply {
                setType(MultipartBody.FORM)
                addFormDataPart("content", content.text.toString().trim())
                addFormDataPart("poll_options", preview.pollOptionsToJsonString())
                preview.getPollItems().forEachIndexed { index, pollItem ->
                    if(pollItem.imageUri != null){
                        val stream = requireContext().contentResolver.openInputStream(pollItem.imageUri!!)
                        addFormDataPart("$index", "$index.jpg", stream!!.readBytes().toRequestBody())
                        stream.close()
                    }
                }
            }.build()
            serverAPI.createPoll(body).enqueue(object:CustomCallback<Int>(){
                override fun onSuccess(t: Int) {
                    findNavController().popBackStack()
                }
            })
        }

        newPollItem.setOnClickListener {
            AddPollItemDialogFragment.newInstance().apply {
                addListener { title, image ->
                    addPollItem(title, image)
                }
                //show(parentFragmentManager, AddPollItemDialogFragment.TAG)

            }.show(parentFragmentManager, AddPollItemDialogFragment.TAG)
        }

        return layout
    }
    private fun deletePollItem(item:PollItem){
        pollItems.removeView(item)
        preview.removePollItem(item)
    }

    fun addPollItem(title:String, image:Uri?){
        pollItems.addView(preview.addPollItem(requireContext(),title, image).also {
            it.setOnLongClickListener {_->
                AlertDialog.Builder(requireContext()).apply {
                    setMessage(R.string.delete_poll_item)
                    setPositiveButton(R.string.delete){_,_->
                        deletePollItem(it)
                    }
                    setNegativeButton(R.string.cancel){_,_->}
                    show()
                }
                true
            }
        })
    }

}