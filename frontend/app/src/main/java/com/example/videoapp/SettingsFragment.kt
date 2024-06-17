package com.example.videoapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.videoapp.network.MyProfile
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SettingsFragment : BaseFragment() {
    lateinit var materialToolbar: MaterialToolbar
    lateinit var pp: ShapeableImageView
    lateinit var name:TextView
    lateinit var createTime:TextView
    lateinit var views:TextView
    lateinit var likes:TextView
    lateinit var dislikes:TextView
    lateinit var logOut:TextView
    lateinit var updateProfile:TextView
    lateinit var deleteProfile:TextView
    lateinit var chipGroup: ChipGroup
    lateinit var linearLayout: LinearLayout
    lateinit var progressBar: ProgressBar
    lateinit var profileFail:TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        setViews(view)
        serverAPI.myProfile().enqueue(object:Callback<MyProfile>{
            override fun onResponse(p0: Call<MyProfile>, p1: Response<MyProfile>) {
                if(p1.body()?.id != null){
                    preparePage(p1.body()!!)
                } else {
                    progressBar.visibility = View.GONE
                    profileFail.visibility = View.VISIBLE
                }
            }

            override fun onFailure(p0: Call<MyProfile>, p1: Throwable) {
                progressBar.visibility = View.GONE
                profileFail.visibility = View.VISIBLE
                //Toast.makeText(context, R.string.failed_to_get_profile, Toast.LENGTH_LONG).show()
            }
        })
        return view
    }

    fun preparePage(data:MyProfile){
        Utils.setPP(data.profilePicture, pp)
        name.text = data.name
        createTime.text = getString(R.string.creation_date, Utils.dateToString(data.createTime, requireContext()))
        views.text = getString(R.string.total_views, data.views)
        views.setOnClickListener {
            if(data.views >0)
                MyDialogFragment.newInstance(MyDialogEnum.VIEW.name).show(parentFragmentManager, MyDialogEnum.VIEW.name)
        }
        likes.text = getString(R.string.total_likes, data.likes)
        likes.setOnClickListener {
            if(data.likes >0)
                MyDialogFragment.newInstance(MyDialogEnum.LIKE.name).show(parentFragmentManager,MyDialogEnum.LIKE.name)
        }
        dislikes.text = getString(R.string.total_dislikes, data.dislikes)
        dislikes.setOnClickListener {
            if(data.dislikes >0)
                MyDialogFragment.newInstance(MyDialogEnum.DISLIKE.name).show(parentFragmentManager,MyDialogEnum.DISLIKE.name)
        }
        updateProfile.setOnClickListener {
            findNavController().navigate(R.id.updateProfileFragment)
        }
        deleteProfile.setOnClickListener {
            AlertDialog.Builder(context).setMessage(R.string.delete_profile_confirmation).setPositiveButton(R.string.yes) { p0, p1 ->
                serverAPI.deleteProfile().enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(p0: Call<ResponseBody>, p1: Response<ResponseBody>) {
                        setToken(null)
                        setUserId(-1)
                        findNavController().navigate(R.id.registerFragment, null, navOptions { popUpTo(R.id.mobile_navigation) })
                        Toast.makeText(context, R.string.profile_deleted, Toast.LENGTH_LONG).show()
                    }
                    override fun onFailure(p0: Call<ResponseBody>, p1: Throwable) {
                        Toast.makeText(context, R.string.deleting_profile_failed, Toast.LENGTH_LONG).show()
                    }
                })
            }.setNegativeButton(R.string.no, null).show()
        }



        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            when(chipGroup.checkedChipId){
                R.id.english->(activity as MainActivity).changeLanguage("en")
                R.id.finnish->(activity as MainActivity).changeLanguage("fi")
            }
        }

        progressBar.visibility = View.GONE
        linearLayout.visibility = View.VISIBLE
    }

    private fun setViews(view:View){
        linearLayout = view.findViewById(R.id.linearLayout)
        progressBar = view.findViewById(R.id.progressBar)
        profileFail = view.findViewById(R.id.profile_fail)
        materialToolbar = view.findViewById(R.id.materialToolbar3)
        pp = view.findViewById(R.id.imageView)
        name = view.findViewById(R.id.name)
        createTime = view.findViewById(R.id.create_date)
        views = view.findViewById(R.id.total_views)
        likes = view.findViewById(R.id.liked_videos)
        dislikes = view.findViewById(R.id.disliked_videos)
        updateProfile = view.findViewById(R.id.update_profile)
        deleteProfile = view.findViewById(R.id.delete_profile)
        chipGroup = view.findViewById(R.id.chip_group)
        logOut = view.findViewById(R.id.log_out)
        logOut.setOnClickListener {
            setToken(null)
            setUserId(-1)
            findNavController().navigate(R.id.loginFragment, null, navOptions { popUpTo(R.id.mobile_navigation) })
        }
    }
}