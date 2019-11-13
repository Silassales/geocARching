package com.porpoise.geocarching

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.firestore.FirebaseFirestore
import com.porpoise.geocarching.firebaseObjects.Cache
import com.porpoise.geocarching.firebaseObjects.User
import com.porpoise.geocarching.firebaseObjects.UserVisit
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity(), MapsFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
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
                        R.id.nav_maps, R.id.nav_profile, R.id.nav_records, R.id.nav_settings, R.id.nav_about, R.id.nav_sign_out
                ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val floatingActionButton: FloatingActionButton = findViewById(R.id.fab)

        floatingActionButton.setOnClickListener {
            if (MapsFragment.nearbyCacheId == null) {
                // give an alert if we're not near a cache
                Snackbar.make(it, "No cache nearby, go and find one!", Snackbar.LENGTH_LONG).show()
            } else {
                // navigate if we're near a cache
                navController.navigate(R.id.AR)
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
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
