package com.example.videoapp

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.videoapp.network.LoginResponse
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class RegisterFragment : BaseFragment() {

    private var uri : Uri? = null
    private lateinit var usernameTaken:TextView
    private lateinit var pp:ShapeableImageView
    private var listener = object: TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            usernameTaken.visibility = View.GONE
        }
        override fun afterTextChanged(p0: Editable?) {}
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){link->
        link?.let {
            uri = it

            Picasso.get().load(it).placeholder(R.drawable.person).rotate(Utils.imageReverseRotation(
                requireContext().contentResolver.openInputStream(link)!!
            )).resize(80,80).into(pp)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val layout = inflater.inflate(R.layout.fragment_register, container, false)
        val username = layout.findViewById<TextInputEditText>(R.id.username)
        val password = layout.findViewById<TextInputEditText>(R.id.password)
        username.addTextChangedListener(listener)
        password.addTextChangedListener(listener)
        val bio = layout.findViewById<TextInputEditText>(R.id.bio)
        pp = layout.findViewById(R.id.pp)
        usernameTaken = layout.findViewById(R.id.username_taken)

        layout.findViewById<TextView>(R.id.login).setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
        layout.findViewById<Button>(R.id.pick_image).setOnClickListener {
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        layout.findViewById<Button>(R.id.create_account).setOnClickListener {
            val user = username.text.toString().trim()
            val pass = password.text.toString().trim()
            if(user.isBlank() || pass.isBlank())
                return@setOnClickListener

            val stream = if(uri != null)requireContext().contentResolver.openInputStream(uri!!) else null
            serverAPI.register(user, pass, bio.text.toString().trim(),stream?.readBytes()?.toRequestBody())
                .enqueue(object:Callback<LoginResponse>{
                override fun onResponse(p0: Call<LoginResponse>, p1: Response<LoginResponse>) {
                    p1.body()?.apply {
                        if(id>0){
                            setUserId(id)
                            setToken(token)
                            findNavController().navigate(R.id.homeFragment, null, navOptions { popUpTo(R.id.mobile_navigation) })
                        } else {
                            username.setText("")
                            password.setText("")
                            usernameTaken.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onFailure(p0: Call<LoginResponse>, p1: Throwable) {

                }
            })
            stream?.close()
        }
        return layout
    }
}