package com.porpoise.geocarching.NavUI.CacheDetails

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.porpoise.geocarching.R
import com.porpoise.geocarching.firebaseObjects.User

private const val ARG_PARAM1 = "key"

class CacheDetailsFragment : Fragment() {
    private var key: String = ""
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: CacheDetailsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            key = it.getString(ARG_PARAM1).toString()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_cache_details, null)

        recyclerView = view.findViewById(R.id.cache_details_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        viewAdapter = CacheDetailsAdapter()
        recyclerView.adapter = viewAdapter

        FirebaseFirestore.getInstance().collection(getString(R.string.firebase_collection_caches)).document(key).collection(getString(R.string.firebase_collection_users_visits)).get().addOnSuccessListener { visits ->
            visits.forEach { visit ->
                viewAdapter.addVisitToList(visit.getString(getString(R.string.default_username)).toString())
            }
        }

        return view
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            CacheDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}
