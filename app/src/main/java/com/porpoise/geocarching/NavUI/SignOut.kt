package com.porpoise.geocarching.NavUI

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_LENGTH
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.porpoise.geocarching.R
import com.porpoise.geocarching.SplashActivity
import com.porpoise.geocarching.Util.Constants.DISCONNECT_MESSAGE
import com.porpoise.geocarching.Util.Constants.SIGN_OUT_MESSAGE


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SignOut.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SignOut.newInstance] factory method to
 * create an instance of this fragment.
 */
class SignOut : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var signOutButton: Button
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
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_sign_out, container, false)

        auth = FirebaseAuth.getInstance()

        signOutButton = view.findViewById(R.id.sign_out_button)
        signOutButton.setOnClickListener {
            auth.signOut()

            val intent = Intent(context, SplashActivity::class.java).apply {
                putExtra(EXTRA_MESSAGE, SIGN_OUT_MESSAGE)
            }
            startActivity(intent)
        }

        disconnectAccountButton = view.findViewById(R.id.disconnect_account_button)
        disconnectAccountButton.setOnClickListener {
            removeUserFromFirebase()

            val intent = Intent(context, SplashActivity::class.java).apply {
                putExtra(EXTRA_LENGTH, DISCONNECT_MESSAGE)
            }

            startActivity(intent)
        }

        return view
    }

    private fun removeUserFromFirebase() {
        val db = FirebaseFirestore.getInstance()

        db.collection(getString(R.string.firebase_collection_users)).whereEqualTo(getString(R.string.firebase_users_uid), auth.currentUser?.uid).get().addOnSuccessListener {
            for ( document in it) {
                Log.i("removeUserFromFirebase", "removing user: ${document.id}")
                db.collection(getString(R.string.firebase_collection_users)).document(document.id).delete()
            }

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
         * @return A new instance of fragment SignOut.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                SignOut().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
