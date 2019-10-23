package com.porpoise.geocarching

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.porpoise.geocarching.firebaseObjects.Cache
import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.extension.setLocation


class MainActivity : AppCompatActivity(), MapsFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // this is just to track which permission was request from onRequestPermissionsResult
    private val MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 1

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
            Let's check for permissions before anything else
         */
        checkPermissionsAccess()

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

        //Collections are tables, documents are entries
        val db = FirebaseFirestore.getInstance()
        val profiles = db.collection("profiles")
        //val profile = hashMapOf("name" to "Aaron", "num_caches" to 0)
        //profiles.document("document_title").set(profile)
        //val acquired_profile = profiles.get("document_title")
        profiles.get().addOnSuccessListener(){ result ->
            for(document in result) {
                Log.d("TESTTAG", "${document.id} => ${document.data}")
            }
        }

        // for adding default cache data to firebase
//        val lat = 43.545115
//        val long = -80.247056
//        val newCache = Cache(GeoPoint(lat, long),
//                date_placed = Timestamp.now(),
//                description = "testing cache",
//                model = 1,
//                name = "testing cache 2"
//                )
//        db.collection("Caches").add(newCache).addOnSuccessListener { cacheID ->
//            val geoFirestore = GeoFirestore(db.collection(getString(R.string.firebase_collection_caches)))
//
//            geoFirestore.setLocation(cacheID.id, GeoPoint(lat, long)) { exception ->
//                if(exception != null) Log.d("geoFirestore" , "failed to add location to cache ${cacheID.id}, exception: ${exception.message}")
//            }
//        }

        val floatingActionButton : FloatingActionButton = findViewById(R.id.fab)

        floatingActionButton.setOnClickListener{
            navController.navigate(R.id.AR)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /*
    Permissions stuff
     */

    private fun checkPermissionsAccess() {
        /*
            If you have any permissions that you need to add, add them here as this will be called
            whenever we need to confirm we have the correct permissions
         */
        // check for fine location permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            // In the future we may want to add prompts for the users to know why we need these permissions
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_ACCESS_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            MY_PERMISSIONS_REQUEST_ACCESS_LOCATION -> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED)) {
                    /* welp permission still denined.. we can't do anything without location so we
                    * need to exit, in the future add a dialog to tell the user this
                    */
                    finishAndRemoveTask()
                }
                return
            }
        }
    }
}
