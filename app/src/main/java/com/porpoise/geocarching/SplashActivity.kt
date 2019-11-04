package com.porpoise.geocarching

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.AlarmClock.EXTRA_LENGTH
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.SignInButton
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.porpoise.geocarching.Util.Constants.DISCONNECT_MESSAGE
import com.porpoise.geocarching.Util.Constants.DONT_DISCONNECT_MESSAGE
import com.porpoise.geocarching.Util.Constants.DONT_SIGN_OUT_MESSAGE
import com.porpoise.geocarching.Util.Constants.RC_SIGN_IN
import com.porpoise.geocarching.Util.Constants.SIGN_OUT_MESSAGE
import com.porpoise.geocarching.Util.Constants.SPLASH_SCREEN_DELAY
import com.porpoise.geocarching.Util.Constants.STARTING_EXPERIENCE
import com.porpoise.geocarching.Util.Constants.STARTING_LEVEL
import com.porpoise.geocarching.firebaseObjects.User
import com.porpoise.geocarching.firebaseObjects.UserVisit


class SplashActivity : AppCompatActivity(), View.OnClickListener {


    lateinit var loadingTextView: TextView
    lateinit var signInBt: SignInButton
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_splash)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // see if this is being called from sign out, if so we need to sign the user out
        val messageSignOut = intent.getIntExtra(EXTRA_MESSAGE, DONT_SIGN_OUT_MESSAGE)
        if(messageSignOut == SIGN_OUT_MESSAGE) {
            Log.d("onCreate", "being called (i hope) from sign out, signing out of google account")
            mGoogleSignInClient.signOut()
        }

        // see if this is being called from disconnect, if so we need to sign the user out
        val messageDisconnect = intent.getIntExtra(EXTRA_LENGTH, DONT_DISCONNECT_MESSAGE)
        if(messageDisconnect == DISCONNECT_MESSAGE) {
            Log.d("onCreate", "being called (i hope) from disconnect, revoking access of google account")
            mGoogleSignInClient.revokeAccess()
        }

        loadingTextView = findViewById(R.id.loadingTextView)
        signInBt = findViewById(R.id.sign_in_button)
        signInBt.setOnClickListener(this)

        auth = FirebaseAuth.getInstance()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.sign_in_button -> signIn()
        }
    }

    private fun signIn() {
        Log.d("signIn", "Starting signin Intent")
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("onActivityResult", "results returned from GoogleSignInApi.getSignInIntent")
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                Log.d("onActivityResult", "sign in successful with account owner: ${account!!.displayName}")
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w("GoogleSignIn", "Google sign in failed", e)
                // ...
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d("firebaseAuthWithGoogle", "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("firebaseAuthWithGoogle", "signInWithCredential:success")
                        populateUserCollectionWithLoggedInUser(auth.currentUser)
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("firebaseAuthWithGoogle", "signInWithCredential:failure", task.exception)
                        Snackbar.make(findViewById(R.id.splash_layout), "Authentication Failed. Closing app.", Snackbar.LENGTH_SHORT).show()
                        finishAndRemoveTask()
                    }

                    // ...
                }
    }

    private fun populateUserCollectionWithLoggedInUser(currentUser: FirebaseUser?) {
        val db = FirebaseFirestore.getInstance()
        // only create new user if user doesn't exist
        val uid = currentUser?.uid
        Log.d("populateUserCollectionWithLoggedInUser", "checking if user with uid $uid has an account, and if not creating one for them")
        db.collection(getString(R.string.firebase_collection_users)).whereEqualTo(getString(R.string.firebase_users_uid), uid).get().addOnSuccessListener {
            if (it.isEmpty) {
                val newUser = User(currentUser?.displayName ?: getString(R.string.default_username), currentUser?.email
                        ?: "", currentUser?.uid ?: "", STARTING_EXPERIENCE, STARTING_LEVEL)

                db.collection("Users").add(newUser)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if(auth.currentUser != null) {
            // the user has already signed in, hide sign in, show loading, go to main activity
            signInBt.visibility = View.INVISIBLE
            loadingTextView.visibility = View.VISIBLE

            Handler().postDelayed({
                //start main activity
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                //finish
                finish()
            },SPLASH_SCREEN_DELAY)
        }
    }

}
