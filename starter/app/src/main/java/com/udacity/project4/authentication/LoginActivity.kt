package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityLoginBinding
import com.udacity.project4.utils.showShortSnackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginActivity : AppCompatActivity() {

    private val TAG = LoginActivity::class.simpleName

    private lateinit var binding: ActivityLoginBinding
    private lateinit var signInLauncher: ActivityResultLauncher<Intent?>
    private val authViewModel: AuthenticationViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_login)

        signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult -> onSignInResult(result) }

        binding.loginButton.setOnClickListener { launchLogInFlow() }

        title = getString(R.string.welcome)
    }

    private fun launchLogInFlow() {
        if (::signInLauncher.isInitialized)
            signInLauncher.launch(authViewModel.createFirebaseAuthIntent())
    }

    private fun onSignInResult(result: ActivityResult) {
        val response = IdpResponse.fromResultIntent(result.data)
        response?.error?.let {
            Log.i(TAG, "Sign in unsuccessful ${it.errorCode}")
            binding.root.showShortSnackbar(getString(R.string.login_failed))
            return
        }

        if (result.resultCode == RESULT_OK) {
            // Successfully signed in user.
            Log.i(TAG, "Successfully signed in user ${firebaseUserName()}!")
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    private fun firebaseUserName() =
        FirebaseAuth.getInstance().currentUser?.displayName
}
