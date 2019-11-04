package com.porpoise.geocarching

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.firebase.firestore.FirebaseFirestore
import com.porpoise.geocarching.firebaseObjects.Cache
import com.porpoise.geocarching.firebaseObjects.User
import com.porpoise.geocarching.firebaseObjects.UserVisit

class MainActivity : AppCompatActivity(), MapsFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
                setOf(
                        R.id.nav_maps, R.id.nav_profile, R.id.nav_records, R.id.nav_settings, R.id.nav_about, R.id.nav_sign_out
                ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val floatingActionButton: FloatingActionButton = findViewById(R.id.fab)

        floatingActionButton.setOnClickListener {
            if (MapsFragment.nearbyCacheId == null) {
                // give an alert if we're not near a cache
                Snackbar.make(it, "No cache nearby, go and find one!", Snackbar.LENGTH_LONG).show()
            } else {
                // navigate if we're near a cache
                navController.navigate(R.id.AR)
                // and add a visit to the current user
                MapsFragment.nearbyCacheId?.let { nearbyCacheId -> addVisitToCurrentUser(nearbyCacheId) }
            }
        }

        // add a listener to hide the fab when opening the cache viewer and to show it when leaving
        navController.addOnDestinationChangedListener { _: NavController, destination: NavDestination, _: Bundle? ->
            if (destination.id == R.id.AR) {
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

    private fun addVisitToCurrentUser(nearbyCacheId: String) {
        /* a great example of how firebase can be ugly has hell to write data.... but once its written its quick and easy to read
            with just something like collection(users).document(userId).collection(visits) gets you all the cache visits this user has made
            no querying the cache table, just one line
         */
        // TODO this could be cleaned up and separated into parts
        auth.currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users)).whereEqualTo(getString(R.string.firebase_users_uid), currentAuthUser.uid).get().addOnSuccessListener { currentUserSnapshots ->
                var currentUser: User? = null
                var currentUserId = ""
                for (currentUserSnapshot in currentUserSnapshots) {
                    currentUser = currentUserSnapshot.toObject(User::class.java)
                    currentUserId = currentUserSnapshot.id
                }
                currentUser?.let { safeCurrentUser ->
                    firestore.collection(getString(R.string.firebase_collection_caches)).document(nearbyCacheId).get().addOnSuccessListener { visitedCacheSnapshot ->
                        val visitedCache = visitedCacheSnapshot.toObject(Cache::class.java)
                        val visitedCacheId = visitedCacheSnapshot.id
                        visitedCache
                                ?: Log.d("addVisitToCurrentUser", "Couldn't populate fields from snapshot: {${visitedCacheSnapshot.id}")
                        visitedCache?.let { safeVisitedCache ->
                            Log.d("addVisitToCurrentUser", "user: $safeCurrentUser visited cache: ${safeVisitedCache.name}")
                            val visit = UserVisit(visitedCache.name, visitedCache.l, visitedCache.g)
                            firestore.collection(getString(R.string.firebase_collection_users))
                                    .document(currentUserId)
                                    .collection(getString(R.string.firebase_collection_users_visits))
                                    .document(visitedCacheId)
                                    .set(visit)
                        }
                    }
                }
            }
        }
    }
}
