package com.example.videoapp

import android.os.Bundle
import android.view.Menu
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.example.videoapp.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        //val appBarConfiguration = AppBarConfiguration(setOf(R.id.navigation_home, R.id.navigation_create, R.id.navigation_subscriptions, R.id.navigation_profile))
        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setOnItemSelectedListener {
                when(it.itemId){
                    R.id.navigation_create -> navController.navigate(R.id.createFragment)
                    R.id.navigation_home -> navController.navigate(R.id.homeFragment)
                    R.id.navigation_profile -> navController.navigate(R.id.settingsFragment)
                    R.id.navigation_subscriptions -> navController.navigate(R.id.subscriptionsFragment)
                }
            true
        }
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
                if(destination.id == R.id.loginFragment || destination.id == R.id.registerFragment
                    ||destination.id == R.id.updateProfileFragment || destination.id == R.id.videoUploadFragment
                    || destination.id == R.id.profileFragment){
                    navView.visibility = View.GONE
                } else{
                    navView.visibility = View.VISIBLE
                }
             }


        if(token() != null){
            //supportFragmentManager.beginTransaction().add(R.id.profile_container, LoginFragment()).commit()
            //test token
            navController.navigate(R.id.homeFragment, null, navOptions { popUpTo(R.id.mobile_navigation) })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    fun startVideo(videoId:Int){
        supportFragmentManager.beginTransaction().replace(R.id.video_container,
            BottomSheet::class.java, Bundle().apply { putInt("video_id",videoId) }).commit()
    }

    fun showProfile(profileId:Int){
        navController.navigate(R.id.profileFragment,
            Bundle().apply { putInt(ProfileFragment.TAG, profileId) })
    }
}