package com.porpoise.geocarching.NavUI.Records


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.porpoise.geocarching.R
import com.porpoise.geocarching.firebaseObjects.UserPlacedCache



class RecordsPlacedFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var recordsPlacedAdapter: RecordsPlacedAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_records_placed, container, false)

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
            firestore.collection(getString(R.string.firebase_collection_users)).whereEqualTo(getString(R.string.firebase_users_uid), currentAuthUser.uid).get().addOnSuccessListener { currentUserSnapshots ->
                firestore.collection(getString(R.string.firebase_collection_users))
                        .document(currentUserSnapshots.first().id).collection(getString(R.string.firebase_collection_users_placed_caches)).get().addOnSuccessListener { placedCacheSnapshots ->
                            Log.d("getCurrentUsersPlacedCaches", "found some placed caches for ${currentUserSnapshots.first().id}, # of placed: ${placedCacheSnapshots.size()}")
                            recordsPlacedAdapter.setUserPlacedList(placedCacheSnapshots.toObjects(UserPlacedCache::class.java))
                        }
            }
        }
    }


}
