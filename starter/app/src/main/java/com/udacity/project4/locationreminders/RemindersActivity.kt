package com.udacity.project4.locationreminders

import android.os.Bundle
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticatedActivity
import com.udacity.project4.databinding.ActivityRemindersBinding
import com.udacity.project4.utils.showShortSnackbar

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AuthenticatedActivity() {

    private lateinit var binding: ActivityRemindersBinding

    override fun onUserAuthenticated() {
        if(::binding.isInitialized)
            binding.root.showShortSnackbar(getString(R.string.user_authenticated))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_reminders)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                binding.navHostFragment.findNavController().popBackStack()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
