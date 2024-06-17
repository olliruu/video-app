package com.example.videoapp

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.videoapp.network.LoginRequest
import com.example.videoapp.network.LoginResponse
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginFragment : BaseFragment() {

    lateinit var wrongInfo : TextView
    private var listener = object:TextWatcher{
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            wrongInfo.visibility = View.GONE
        }
        override fun afterTextChanged(p0: Editable?) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val layout =  inflater.inflate(R.layout.fragment_login, container, false)
        var name = layout.findViewById<TextInputEditText>(R.id.username)
        var password = layout.findViewById<EditText>(R.id.password)
        wrongInfo = layout.findViewById(R.id.wrong_info)
        name.addTextChangedListener(listener)
        password.addTextChangedListener(listener)
        layout.findViewById<TextView>(R.id.register).setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
        layout.findViewById<Button>(R.id.login).setOnClickListener {
            serverAPI.login(LoginRequest(name.text.toString().trim(), password.text.toString().trim())).enqueue(object:Callback<LoginResponse>{
                override fun onResponse(p0: Call<LoginResponse>, p1: Response<LoginResponse>) {
                    p1.body()?.apply {
                        if(id >0){
                            setToken(token)
                            setUserId(id)
                            Toast.makeText(context, R.string.logged_in, Toast.LENGTH_LONG).show()
                            findNavController().navigate(R.id.homeFragment, null, navOptions { popUpTo(R.id.mobile_navigation) })
                        } else {
                            name.setText("")
                            password.setText("")
                            wrongInfo.visibility = View.VISIBLE
                        }
                    }
                }
                override fun onFailure(p0: Call<LoginResponse>, p1: Throwable) {
                    Toast.makeText(context, R.string.login_failed, Toast.LENGTH_LONG).show()
                }
            })
        }
        return layout
    }
}