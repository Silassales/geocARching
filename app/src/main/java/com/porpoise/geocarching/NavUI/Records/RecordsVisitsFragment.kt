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
import com.porpoise.geocarching.firebaseObjects.UserVisit

class RecordsVisitsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewVisitsAdapter: RecordsVisitsAdapter
    private lateinit var loadingText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_records_visits, container, false)

        loadingText = view.findViewById(R.id.records_visits_loading)
        recyclerView = view.findViewById(R.id.records_visits_recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)

        viewVisitsAdapter = RecordsVisitsAdapter()

        recyclerView.adapter = viewVisitsAdapter

        getCurrentUsersVisits()

        return view
    }

    private fun getCurrentUsersVisits() {
        val firestore = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users))
                .document(currentAuthUser.uid)
                .collection(getString(R.string.firebase_collection_users_visits))
                .get()
                .addOnSuccessListener { visitedCacheSnapshots ->
                    if(visitedCacheSnapshots.isEmpty) {
                        loadingText.setTypeface(null, NORMAL)
                        loadingText.text = getString(R.string.no_visits)
                    } else {
                        loadingText.visibility = View.GONE
                        viewVisitsAdapter.setUserVisitList(visitedCacheSnapshots.toObjects(UserVisit::class.java))
                    }

                    Log.d("getCurrentUsersVists", "found some visits for ${currentAuthUser.uid}, # of visits: ${visitedCacheSnapshots.size()}")
                }
        }
    }


}
