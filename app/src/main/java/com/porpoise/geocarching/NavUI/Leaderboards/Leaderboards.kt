package com.porpoise.geocarching.NavUI.Leaderboards

import android.graphics.Typeface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.porpoise.geocarching.R
import com.porpoise.geocarching.firebaseObjects.LeaderboardUser


class Leaderboards : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var leaderboardAdapter: LeaderboardsAdapter
    private lateinit var loadingText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view: View = inflater.inflate(R.layout.fragment_leaderboards, container, false)

        loadingText = view.findViewById(R.id.leaderboards_loading)
        recyclerView = view.findViewById(R.id.leaderboards_recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)

        leaderboardAdapter = LeaderboardsAdapter()

        recyclerView.adapter = leaderboardAdapter

        getLeaderboard()

        return view
    }

    private fun getLeaderboard() {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection(getString(R.string.firebase_collection_users))
                .orderBy(getString(R.string.firebase_users_level), Query.Direction.DESCENDING)
                .limit(50).get().addOnSuccessListener {leaderboardSnapshot ->
            if(leaderboardSnapshot.isEmpty) {
                loadingText.setTypeface(null, Typeface.NORMAL)
                loadingText.text = getString(R.string.no_users)
            } else {
                loadingText.visibility = View.GONE
                leaderboardAdapter.setLeaderboardList(leaderboardSnapshot.toObjects(LeaderboardUser::class.java))
            }
        }
    }
}
