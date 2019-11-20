package com.porpoise.geocarching.NavUI

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.porpoise.geocarching.R
import com.porpoise.geocarching.Util.Leveling
import com.squareup.picasso.Picasso

class Profile : Fragment() {

    private lateinit var auth: FirebaseAuth
    lateinit var profilePicView: ImageView
    lateinit var userFullNameTextView: TextView
    lateinit var usernameTextView: TextView
    lateinit var emailTextView: TextView
    lateinit var levelTextView: TextView
    lateinit var xpTextView: TextView
    lateinit var currentLevelTextView: TextView
    lateinit var nextLevelTextView: TextView
    lateinit var levelProgressBar: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_profile, container, false)
        profilePicView = view.findViewById(R.id.profile_image_view)
        userFullNameTextView = view.findViewById(R.id.userfullname_text_view)
        usernameTextView = view.findViewById(R.id.username_text_view)
        emailTextView = view.findViewById(R.id.email_text_view)
        levelTextView = view.findViewById(R.id.profile_level_text_view)
        xpTextView = view.findViewById(R.id.profile_experience_text_view)
        currentLevelTextView = view.findViewById(R.id.profile_current_level_text_view)
        nextLevelTextView = view.findViewById(R.id.profile_next_level_text_view)
        levelProgressBar = view.findViewById(R.id.profile_level_progress_bar)

        auth = FirebaseAuth.getInstance()
        auth.currentUser?.run {
            val user: FirebaseUser = auth.currentUser as FirebaseUser

            user.photoUrl?.let { Picasso.get().load(it).into(profilePicView) }
            user.displayName?.let { userFullNameTextView.text = it }
            user.displayName?.let { usernameTextView.text = it }
            user.email?.let { emailTextView.text = it }
        }

        setXpLevel()

        return view
    }

    private fun setXpLevel() {
        val firestore = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users)).document(currentAuthUser.uid).get().addOnSuccessListener { currentUser ->
                val currentLevel = (currentUser[getString(R.string.firebase_collection_users_level)] as Long).toInt()
                val currentXp = (currentUser[getString(R.string.firebase_collection_users_experience)] as Long).toInt()

                levelTextView.text = currentLevel.toString()
                xpTextView.text = currentXp.toString()

                currentLevelTextView.text = currentLevel.toString()
                nextLevelTextView.text = (currentLevel + 1).toString()

                val progressLevel = Leveling.getProgressToNextLevel(currentLevel, currentXp)

                if(!(progressLevel < 0 || progressLevel > 100)) {
                    if(progressLevel == 0) {
                        // to show some progress so the user can tell what the widget it
                        levelProgressBar.setProgress(1, true)
                    } else {
                        levelProgressBar.setProgress(progressLevel, true)
                    }
                } else {
                    Log.d("setXpLevel", "Invalid progress level returned with current level: $currentLevel and current XP: $currentXp. Value returned: $progressLevel")
                }
            }
        }
    }
}
