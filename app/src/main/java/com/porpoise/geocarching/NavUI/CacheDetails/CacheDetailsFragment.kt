package com.porpoise.geocarching.NavUI.CacheDetails

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Typeface.NORMAL
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.porpoise.geocarching.R
import android.widget.Toast
import android.content.Intent



private const val ARG_PARAM1 = "key"

class CacheDetailsFragment : Fragment() {
    private var key: String = ""
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var loadingText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: CacheDetailsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            key = it.getString(ARG_PARAM1).toString()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // To clear previously added menu items
        menu.clear()
        inflater.inflate(R.menu.cache_details_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.cache_details_report -> {
                activity?.let { safeActivtiy ->
                    val builder = AlertDialog.Builder(safeActivtiy)

                    builder.setView(View.inflate(context, R.layout.report_cache_dialog, null))
                            .setPositiveButton(R.string.report_cache_positive) { dialogInterface, _ ->
                                val dialog = dialogInterface as Dialog
                                val comment = dialog.findViewById<EditText>(R.id.report_cache_comment_edit_text).text.toString()

                                val i = Intent(Intent.ACTION_SEND)
                                i.type = "message/rfc822"
                                i.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.developer_email)))
                                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_cache_email_subject, key))
                                i.putExtra(Intent.EXTRA_TEXT, getString(R.string.report_cache_body, key, comment))
                                try {
                                    startActivity(Intent.createChooser(i, "Send mail..."))
                                } catch (ex: android.content.ActivityNotFoundException) {
                                    Toast.makeText(context, "There are no email clients installed.", Toast.LENGTH_SHORT).show()
                                }
                            }.setNegativeButton(R.string.report_cache_negative) { dialog, _ ->
                                dialog.dismiss()
                            }

                    builder.setTitle(R.string.report_cache_title)

                    builder.show()
                } ?: Log.d("ReportCache", "activity null")
                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_cache_details, null)
        val firestore = FirebaseFirestore.getInstance()

        loadingText = view.findViewById(R.id.cache_details_loading)
        recyclerView = view.findViewById(R.id.cache_details_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        viewAdapter = CacheDetailsAdapter()
        recyclerView.adapter = viewAdapter

        firestore.collection(getString(R.string.firebase_collection_caches))
                .document(key)
                .collection(getString(R.string.firebase_collection_found_caches))
                .get()
                .addOnSuccessListener { visits ->
            if(visits.isEmpty) {
                loadingText.setTypeface(null, NORMAL)
                loadingText.text = getString(R.string.no_visits)
            } else {
                loadingText.visibility = View.GONE
                visits.forEach { visit ->
                    viewAdapter.addVisitToList(visit.getString(getString(R.string.default_username)).toString())
                }
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
