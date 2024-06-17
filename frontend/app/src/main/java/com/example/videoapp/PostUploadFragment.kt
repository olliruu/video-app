package com.example.videoapp

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.videoapp.network.CustomCallback
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File


public class PostUploadFragment : BaseFragment(), ImageUploadAdapter.PictureInterface {

    lateinit var toolbar: MaterialToolbar
    lateinit var content: TextInputEditText
    lateinit var images: RecyclerView
    lateinit var button: Button

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){ uri->
        uri?.apply {
            (images.adapter as ImageUploadAdapter).addImage(this)
        }
    }

    private var imageUri:Uri? = null

    private val takeImage = registerForActivityResult(ActivityResultContracts.TakePicture()){success->
        if(success){
            imageUri?.apply {
                (images.adapter as ImageUploadAdapter).addImage(this)
            }
        } else {
            imageUri = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var layout =  inflater.inflate(R.layout.fragment_post_upload, container, false)
        toolbar = layout.findViewById(R.id.upload_post_toolbar)
        content = layout.findViewById(R.id.post_content)
        images = layout.findViewById(R.id.uploaded_images_recyclerview)
        button = layout.findViewById(R.id.upload_button)
        images.layoutManager = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.HORIZONTAL
        }
        images.adapter = ImageUploadAdapter(activity as MainActivity, pickImage, this)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        button.setOnClickListener {
            var requestBody = MultipartBody.Builder().apply {
                setType(MultipartBody.FORM)
                addFormDataPart("content", content.text.toString().trim())
                (images.adapter as ImageUploadAdapter).dataset.forEachIndexed { index: Int, uri: Uri ->
                    val stream = requireActivity().contentResolver.openInputStream(uri)!!
                    addFormDataPart("$index", "file$index.jpg", stream.readBytes().toRequestBody())
                    stream.close()
                }
            }.build()
            serverAPI.createPost(requestBody).enqueue(object:CustomCallback<Int>(){
                override fun onSuccess(t: Int) {
                    findNavController().popBackStack()
                }
            })
        }
        return layout
    }

    override fun takePicture() {
        imageUri = FileProvider.getUriForFile(requireContext(),"com.example.videoapp.fileprovider", File(requireContext().filesDir,"picture.jpg"))
        takeImage.launch(imageUri)
    }
}

class ImageUploadAdapter(val activity:MainActivity, private val pickImage: ActivityResultLauncher<PickVisualMediaRequest>, private val pictureInterface: PictureInterface) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    var dataset = mutableListOf<Uri>()

    interface PictureInterface{
        fun takePicture()
    }

    fun addImage(uri: Uri?){
        if(uri != null){
            dataset.add(uri)
            notifyItemInserted(dataset.size)
        }
    }

    class ImageUploadViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var image = itemView as ShapeableImageView
    }

    class AddImageViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var image = itemView.findViewById<ImageView>(R.id.add_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == 0) {
            val shapeableImage = ShapeableImageView(parent.context).apply{
                shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(25F)
                layoutParams.apply {
                    height = LayoutParams.MATCH_PARENT
                    width = LayoutParams.MATCH_PARENT
                }
            }
            ImageUploadViewHolder(shapeableImage)
        } else {
            AddImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.add_button, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if(position < dataset.size) 0 else 1
    }

    override fun getItemCount(): Int {
        return dataset.size + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(getItemViewType(position) == 0){
            var img = (holder as ImageUploadViewHolder).image
            Picasso.get().load(dataset[position]).into(img)
            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(activity).apply {
                    setMessage(R.string.remove_image_question)
                    setNegativeButton(R.string.cancel){_, _-> }
                    setPositiveButton(R.string.remove){_, _->
                        dataset.removeAt(holder.bindingAdapterPosition)
                        notifyItemRemoved(holder.bindingAdapterPosition)
                    }
                }
                true
            }
        } else {
            holder.itemView.setOnClickListener {
                AlertDialog.Builder(activity).apply {
                    setMessage(R.string.pick_or_select_image)
                    setPositiveButton(R.string.take_picture){_,_->
                        pictureInterface.takePicture()
                    }
                    setNeutralButton(R.string.pick_from_gallery){_,_->
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    setNegativeButton(R.string.cancel){_,_->}
                }
            }
        }
    }

}