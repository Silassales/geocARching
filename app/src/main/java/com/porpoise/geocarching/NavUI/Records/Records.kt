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
import com.porpoise.geocarching.firebaseObjects.UserVisit


class Records : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecordsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_records, container, false)

        recyclerView = view.findViewById(R.id.records_recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)

        viewAdapter = RecordsAdapter()

        recyclerView.adapter = viewAdapter

        getCurrentUsersVisits()

        return view
    }

    private fun getCurrentUsersVisits() {
        val firestore = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users)).whereEqualTo(getString(R.string.firebase_users_uid), currentAuthUser.uid).get().addOnSuccessListener { currentUserSnapshots ->
                var currentUserId = ""
                for (currentUserSnapshot in currentUserSnapshots) {
                    currentUserId = currentUserSnapshot.id
                }
                firestore.collection(getString(R.string.firebase_collection_users))
                        .document(currentUserId).collection(getString(R.string.firebase_collection_users_visits)).get().addOnSuccessListener { visitedCacheSnapshots ->
                            Log.d("getCurrentUsersVists", "found some vists for $currentUserId, # of visits: ${visitedCacheSnapshots.size()}")
                            viewAdapter.setUserVisitList(visitedCacheSnapshots.toObjects(UserVisit::class.java))
                        }
            }
        }
    }
}

