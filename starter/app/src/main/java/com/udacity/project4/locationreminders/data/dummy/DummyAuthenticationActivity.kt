package com.udacity.project4.locationreminders.data.dummy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class DummyAuthenticationActivity : AppCompatActivity() {

    private val authViewModel: DummyAuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_OK)
        authViewModel.dummyAuthLiveData.postValue(authViewModel.expectedResultOnAuth)
        finish()
    }
}