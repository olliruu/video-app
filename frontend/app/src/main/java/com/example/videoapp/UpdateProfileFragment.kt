package com.example.videoapp

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.videoapp.network.Profile
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpdateProfileFragment : BaseFragment() {
    lateinit var pp:ShapeableImageView
    lateinit var ppButton:Button
    lateinit var bio:TextInputEditText
    lateinit var password:EditText
    lateinit var updateProfile:Button
    //lateinit var cancel:Button
    lateinit var toolbar: MaterialToolbar
    var uri: Uri? = null

    var imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        uri?.let {
            this@UpdateProfileFragment.uri = uri
            Picasso.get().load(uri).rotate(Utils.imageReverseRotation(it.toFile())).resize(80,80).into(pp)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout =inflater.inflate(R.layout.fragment_update_profile, container, false)
        pp = layout.findViewById(R.id.pp)
        ppButton = layout.findViewById(R.id.pp_button)
        bio = layout.findViewById(R.id.bio)
        password = layout.findViewById(R.id.password)
        updateProfile = layout.findViewById(R.id.update_profile)
        toolbar = layout.findViewById(R.id.materialToolbar11)
        val serverAPI = (activity as BaseActivity).serverAPI
        serverAPI.getProfile(null).enqueue(object: Callback<Profile> {
            override fun onResponse(p0: Call<Profile>, p1: Response<Profile>) {
                p1.body()?.let {
                    Utils.setPP(it.profilePicture, pp)
                    ppButton.setOnClickListener {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    bio.setText(it.description)

                    toolbar.setNavigationOnClickListener {
                        findNavController().popBackStack()
                    }

                    updateProfile.setOnClickListener {

                        var pp = uri?.toFile()?.readBytes()?.toRequestBody()
                        serverAPI.updateProfile(bio.text.toString(),password.text.toString(),pp).enqueue(object:Callback<ResponseBody>{
                            override fun onResponse(
                                p0: Call<ResponseBody>,
                                p1: Response<ResponseBody>
                            ) {
                                Toast.makeText(context, R.string.profile_updated, Toast.LENGTH_LONG).show()
                            }
                            override fun onFailure(p0: Call<ResponseBody>, p1: Throwable) {
                                throw p1
                            }
                        })
                        parentFragmentManager.popBackStack()
                    }
                }
            }

            override fun onFailure(p0: Call<Profile>, p1: Throwable) {
                throw p1
            }
        })
        return layout
    }
}