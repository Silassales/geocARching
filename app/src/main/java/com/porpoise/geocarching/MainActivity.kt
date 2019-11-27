package com.porpoise.geocarching

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity(), MapsFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var auth: FirebaseAuth
    lateinit var profilePicView: ImageView
    lateinit var usernameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val headerView: View = navView.getHeaderView(0)
        profilePicView = headerView.findViewById(R.id.nav_header_imageView)
        usernameTextView = headerView.findViewById(R.id.nav_header_textView)

        auth = FirebaseAuth.getInstance()
        auth.currentUser?.run {
            val user: FirebaseUser = auth.currentUser as FirebaseUser

            user.photoUrl?.let { Picasso.get().load(it).into(profilePicView) }
            user.displayName?.let { usernameTextView.text = it }
        }

        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
                setOf(
                        R.id.nav_maps, R.id.nav_profile, R.id.nav_records, R.id.nav_settings, R.id.nav_leaderboard, R.id.nav_sign_out
                ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val floatingActionButton: FloatingActionButton = findViewById(R.id.fab)

        floatingActionButton.setOnClickListener {
            if (MapsFragment.userLocation == null) {
                Snackbar.make(it, getString(R.string.maps_fab_snackbar_unset_location_message), Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (MapsFragment.nearbyCacheId == null) {
                // give an alert if we're not near a cache with option to place cache
                Snackbar.make(it, getString(R.string.maps_fab_snackbar_place_cache_message), Snackbar.LENGTH_LONG).setAction(getString(R.string.maps_fab_snackbar_place_cache_option)){
                    navController.navigate(R.id.cacheplacer_fragment)
                }.show()
            } else {
                // navigate if we're near a cache
                navController.navigate(R.id.cacheviewer_fragment)
            }
        }

        // add a listener to hide the fab when opening the cache viewer and to show it when leaving
        navController.addOnDestinationChangedListener { _: NavController, destination: NavDestination, _: Bundle? ->
            if (destination.id != R.id.nav_maps) {
                floatingActionButton.hide()
            } else {
                floatingActionButton.show()
            }
        }

        // create a notif channel for any future notifications we are going to send
        /*
            From docs
            Because you must create the notification channel before posting any notifications on
            Android 8.0 and higher, you should execute this code as soon as your app starts.
            It's safe to call this repeatedly because creating an existing notification channel
            performs no operation.
         */
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(getString(R.string.notif_channel_name),
                    getString(R.string.notif_name),
                    NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = getString(R.string.notif_desc)
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.createNotificationChannel(channel)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
