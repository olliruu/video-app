package com.example.videoapp

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import java.io.File

class AddPollItemDialogFragment : BottomSheetDialogFragment() {

    private lateinit var title:TextInputEditText
    private lateinit var image:ImageView
    private lateinit var imageButton: Button
    private lateinit var uploadButton:Button
    private var imageUri: Uri?  = null
    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        imageUri = uri
        Picasso.get().load(imageUri).into(image)
    }
    private val takeImage = registerForActivityResult(ActivityResultContracts.TakePicture()){success->
        Picasso.get().load(imageUri).into(image)
    }

    private var mPollItemListener: PollItemAddedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var layout = requireActivity().layoutInflater.inflate(R.layout.add_poll_item, null)
        title = layout.findViewById(R.id.title)
        image = layout.findViewById(R.id.image)
        uploadButton = layout.findViewById(R.id.add_button)
        imageButton = layout.findViewById(R.id.add_image_button)

        imageButton.setOnClickListener {
            AlertDialog.Builder(requireContext()).apply {
                setItems(arrayOf(getString(R.string.pick_from_gallery), getString(R.string.take_picture))){_,index->

                    when(index){
                        0-> pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        1-> {
                            imageUri = FileProvider.getUriForFile(requireContext(),
                                "com.example.videoapp.fileprovider", File(context.filesDir,"picture.jpg"))
                            takeImage.launch(imageUri)
                        }
                    }
                }
                show()
            }
        }

        image.setOnLongClickListener {
            AlertDialog.Builder(requireContext()).apply {
                setMessage(R.string.delete_image)
                setNegativeButton(R.string.cancel){_,_->}
                setPositiveButton(R.string.delete){_,_->
                    imageUri = null
                    Picasso.get().load(imageUri).into(image)
                }
                show()
            }
            true
        }

        uploadButton.setOnClickListener {
            var title = title.text.toString()
            if(title.isNotBlank() || imageUri != null){
                mPollItemListener?.pollAdded(title, imageUri)
                dismiss()
            }
        }
        return AlertDialog.Builder(requireContext()).setView(layout).create()
    //return super.onCreateDialog(savedInstanceState)
    }

    fun addListener(pollItemAddedListener: PollItemAddedListener){
        mPollItemListener = pollItemAddedListener
    }


    fun interface PollItemAddedListener{
        fun pollAdded(title:String, image:Uri?)
    }

    companion object {
        fun newInstance() =
            AddPollItemDialogFragment().apply {
                arguments = Bundle().apply {
                    //putInt(TAG, profileId)
                }
            }
        const val TAG =  "add_poll_item"
    }
}