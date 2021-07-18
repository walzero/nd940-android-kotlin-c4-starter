package com.udacity.project4.authentication

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

abstract class AuthenticationViewModel(app: Application) : AndroidViewModel(app) {

    abstract val authenticationState: LiveData<AuthenticationState>

    abstract fun logout()

    abstract fun createFirebaseAuthIntent(): Intent

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }
}