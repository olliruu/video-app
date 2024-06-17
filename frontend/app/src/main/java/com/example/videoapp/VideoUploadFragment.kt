package com.example.videoapp

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import com.example.videoapp.network.CustomCallback
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.File


class VideoUploadFragment : BaseFragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var title: TextInputEditText
    private lateinit var description: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var thumbnail: ImageView
    private lateinit var thumbnailButton: Button
    private lateinit var video: Button
    private lateinit var videoPreview: PlayerView
    private lateinit var upload: Button
    private lateinit var loading: ConstraintLayout
    private lateinit var progress: ProgressBar

    private var thumbnailUri : Uri? = null
    private var videoUri : Uri? = null


    private var visibility = Visibility.PUBLIC

    private var videoCapture = registerForActivityResult(ActivityResultContracts.CaptureVideo()) {
        if(!it){
            videoUri = null
        } else {
            videoPreview.player?.apply {
                setMediaItem(MediaItem.fromUri(videoUri!!))
                prepare()
            }
        }
    }
    private val videoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        videoUri = uri
        videoPreview.player?.apply {
            setMediaItem(MediaItem.fromUri(videoUri!!))
            prepare()
        }
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {uri->
        uri?.let{
            thumbnailUri = uri
            val stream = requireContext().contentResolver.openInputStream(uri)!!
            Picasso.get().load(uri).rotate(Utils.imageReverseRotation(stream)).into(thumbnail)
            stream.close()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var layout = inflater.inflate(R.layout.fragment_video_upload, container, false)

        toolbar = layout.findViewById(R.id.materialToolbar8)
        title = layout.findViewById(R.id.video_title)
        description = layout.findViewById(R.id.video_description)
        chipGroup = layout.findViewById(R.id.visibility_group)
        thumbnail = layout.findViewById(R.id.thumbnail_image)
        thumbnailButton = layout.findViewById(R.id.thumbail_button)
        video = layout.findViewById(R.id.video_button)
        videoPreview = layout.findViewById(R.id.video_preview)
        upload = layout.findViewById(R.id.upload_video)
        loading = layout.findViewById(R.id.loading)
        progress = layout.findViewById(R.id.progress)
        videoPreview.player = ExoPlayer.Builder(requireContext()).build()

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        thumbnailButton.setOnClickListener {
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        video.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setMessage(R.string.video_question)
                setPositiveButton(R.string.take_video) { _, _ ->

                    videoUri = FileProvider.getUriForFile(context, "com.example.videoapp.fileprovider", File(context.filesDir,"capture.mp4"))
                    videoCapture.launch(videoUri)
                }
                setNeutralButton(R.string.pick_video_from_gallery) {_,_ ->
                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                }
                setNegativeButton(R.string.cancel, null)
                show()
            }
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            when(chipGroup.checkedChipId){
                R.id.chip_public->visibility = Visibility.PUBLIC
                R.id.chip_private->visibility = Visibility.PRIVATE
            }
        }

        upload.setOnClickListener {
            if (title.text.toString().isBlank().not() && description.text.toString().isBlank()
                    .not() && videoUri != null
            ) {
                val imageStream =
                    if (thumbnailUri != null) requireContext().contentResolver.openInputStream(
                        thumbnailUri!!
                    )!! else null

                val body = MultipartBody.Builder().apply {
                    setType(MultipartBody.FORM)
                    addFormDataPart("title", title.text.toString().trim())
                    addFormDataPart("description", description.text.toString().trim())
                    addFormDataPart("visibility", visibility.name.lowercase())
                    if (imageStream != null) {
                        addFormDataPart(
                            "thumbnail",
                            "thumbnail.jpg",
                            imageStream.readBytes().toRequestBody()
                        )
                    }
                }.build()

                serverAPI.postVideo(body).enqueue(object : CustomCallback<ResponseBody>() {
                    override fun onSuccess(t: ResponseBody) {
                        loading.visibility = View.VISIBLE
                        //8k bytes
                        upladingVideo()
                        val thread = Thread {
                            try {
                                val videoStream =
                                    requireContext().contentResolver.openInputStream(videoUri!!)!!
                                val length = videoStream.available()
                                val videoResource =
                                    JSONObject(t.string())["video_resource"] as String
                                val buffer = ByteArray(1024 * 256)
                                var readCount = 0
                                while (videoStream.read(buffer) != -1) {
                                    var chunk =
                                        buffer.toRequestBody("application/octet-stream".toMediaType())
                                    activity?.runOnUiThread{
                                        progress.progress = readCount / length
                                    }
                                    serverAPI.sendChunk(videoResource, chunk).execute()
                                    readCount += 1024 * 256
                                }
                                videoStream.close()
                                serverAPI.prepareVideo(videoResource).execute()
                                imageStream?.close()
                                Toast.makeText(context,R.string.video_sent, Toast.LENGTH_LONG).show()

                            } catch (e: Exception) {
                                e.printStackTrace()
                                imageStream?.close()
                                Handler(Looper.getMainLooper()).post {
                                    findNavController().popBackStack()
                                }
                            }
                        }
                        thread.start()
                    }
                })
            }
        }

        return layout
    }

    fun upladingVideo(){

    }
}

enum class Visibility {PUBLIC, PRIVATE}