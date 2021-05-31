package com.udacity.project4.authentication

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.BuildConfig
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.utils.launchActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
abstract class AuthenticationActivity : AppCompatActivity() {

    private val TAG = AuthenticationActivity::class.simpleName
    private val authViewModel: AuthenticationViewModel by viewModel()

    open fun onUserAuthenticated() {}
    open fun onUserUnauthenticated() {
        launchActivity<LoginActivity>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        authViewModel.authenticationState.observe(this) { authenticationState ->
            onAuthResult(authenticationState)
        }
    }

    private fun onAuthResult(authenticationState: AuthenticationViewModel.AuthenticationState?) {
        when (authenticationState) {
            AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                Log.i(TAG, "Authenticated")
                onUserAuthenticated()
            }
            // If the user is not logged in, they should not be able to set any preferences,
            // so navigate them to the login fragment
            AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED -> {
                Log.i(TAG, "UnAuthenticated")
                onUserUnauthenticated()
            }
            else -> Log.e(
                TAG, "New $authenticationState state that doesn't require any UI change"
            )
        }
    }
}
