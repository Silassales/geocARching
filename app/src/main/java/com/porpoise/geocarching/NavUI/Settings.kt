package com.porpoise.geocarching.NavUI

import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.porpoise.geocarching.R
import com.porpoise.geocarching.SplashActivity
import com.porpoise.geocarching.Util.Constants

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [Settings.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [Settings.newInstance] factory method to
 * create an instance of this fragment.
 */
class Settings : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var disconnectAccountButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_settings, container, false)

        auth = FirebaseAuth.getInstance()

        disconnectAccountButton = view.findViewById(R.id.disconnect_account_button)
        disconnectAccountButton.setOnClickListener {
            removeUserFromFirebase()

            val intent = Intent(context, SplashActivity::class.java).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, Constants.DISCONNECT_MESSAGE)
            }

            startActivity(intent)
        }
        var notifOn = true
        val sharedPref = context?.getSharedPreferences(getString(R.string.app_shared_pref), Context.MODE_PRIVATE)
        sharedPref?.let { safeSP ->
            notifOn = safeSP.getBoolean(getString(R.string.shared_pref_notif_on), true)
        }
        val notifSwitch = view.findViewById<Switch>(R.id.notif_switch)
        notifSwitch.isChecked = notifOn
        notifSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref?.edit()?.putBoolean(getString(R.string.shared_pref_notif_on), isChecked)?.apply()
            if(!isChecked) {
                context?.let {safeContext ->
                    val scheduler = safeContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                    scheduler.cancel(Constants.NOTIFICATION_JOB_ID)
                }
            }
        }

        return view
    }

    private fun removeUserFromFirebase() {
        val firestore = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid.toString()

        firestore.collection(getString(R.string.firebase_collection_users)).document(uid).delete().addOnSuccessListener {
            Log.d("removeUserFromFirebase", "removing user: ${auth.currentUser?.uid}")
        }

        auth.currentUser?.delete()
        auth.signOut()
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Settings.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                Settings().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
