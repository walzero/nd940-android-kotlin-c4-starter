package com.udacity.project4.locationreminders.data.dummy

import android.app.Application
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.udacity.project4.authentication.AuthenticationViewModel

class DummyAuthViewModel(
    private val app: Application
) : AuthenticationViewModel(app) {
    var expectedResultOnAuth: AuthenticationState = AuthenticationState.UNAUTHENTICATED

    val dummyAuthLiveData = MutableLiveData<AuthenticationState>().apply {
        postValue(AuthenticationState.UNAUTHENTICATED)
    }

    override val authenticationState: LiveData<AuthenticationState>
        get() = dummyAuthLiveData

    override fun logout() {}

    override fun createFirebaseAuthIntent() = Intent(app, DummyAuthenticationActivity::class.java)
}