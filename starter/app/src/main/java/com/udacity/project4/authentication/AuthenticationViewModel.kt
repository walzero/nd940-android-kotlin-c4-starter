package com.udacity.project4.authentication

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.map
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R

class AuthenticationViewModel(
    private val app: Application
): AndroidViewModel(app) {
    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

    fun logout() {
        AuthUI.getInstance().signOut(app)
    }

    fun createFirebaseAuthIntent(): Intent {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        return AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.FirebaseAuthTheme)
            .build()
    }

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }
}