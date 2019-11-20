package com.porpoise.geocarching.NavUI.Records

import android.graphics.Typeface.NORMAL
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.porpoise.geocarching.R
import com.porpoise.geocarching.firebaseObjects.UserPlacedCache



class RecordsPlacedFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var recordsPlacedAdapter: RecordsPlacedAdapter
    private lateinit var loadingText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_records_placed, container, false)

        loadingText = view.findViewById(R.id.records_placed_loading)
        recyclerView = view.findViewById(R.id.records_placed_recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)

        recordsPlacedAdapter = RecordsPlacedAdapter()

        recyclerView.adapter = recordsPlacedAdapter

        getCurrentUsersPlacedCaches()

        return view
    }

    private fun getCurrentUsersPlacedCaches() {
        val firestore = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users))
                    .document(currentAuthUser.uid)
                    .collection(getString(R.string.firebase_collection_users_placed_caches))
                    .get()
                    .addOnSuccessListener { placedCacheSnapshots ->
                        if(placedCacheSnapshots.isEmpty) {
                            loadingText.setTypeface(null, NORMAL)
                            loadingText.text = getString(R.string.no_placed_caches)
                        } else {
                            loadingText.visibility = View.GONE
                            recordsPlacedAdapter.setUserPlacedList(placedCacheSnapshots.toObjects(UserPlacedCache::class.java))
                        }

                        Log.d("getCurrentUsersPlacedCaches", "found some placed caches for ${currentAuthUser.uid}, # of placed: ${placedCacheSnapshots.size()}")
                    }
        }
    }
}
