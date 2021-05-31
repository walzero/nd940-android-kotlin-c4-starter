package com.udacity.project4.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseRecyclerViewAdapter


/**
 * Extension function to setup the RecyclerView
 */
fun <T> RecyclerView.setup(
    adapter: BaseRecyclerViewAdapter<T>
) {
    this.apply {
        layoutManager = LinearLayoutManager(this.context)
        this.adapter = adapter
    }
}

fun Fragment.setTitle(title: String) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }
}

fun Fragment.setDisplayHomeAsUpEnabled(bool: Boolean) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(
            bool
        )
    }
}

//animate changing the view visibility
fun View.fadeIn() {
    this.visibility = View.VISIBLE
    this.alpha = 0f
    this.animate().alpha(1f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeIn.alpha = 1f
        }
    })
}

//animate changing the view visibility
fun View.fadeOut() {
    this.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeOut.alpha = 1f
            this@fadeOut.visibility = View.GONE
        }
    })
}

fun View.showConfirmationSnackbar(
    text: String,
    length: Int = Snackbar.LENGTH_INDEFINITE,
    actionText: String,
    action: ((Snackbar) -> Unit)? = null
): Snackbar = Snackbar.make(this, text, length).apply {
    when (action) {
        null -> setAction(actionText, null)
        else -> setAction(actionText) { action(this) }
    }
}.also { it.show() }

fun View.showShortSnackbar(
    text: String,
    length: Int = Snackbar.LENGTH_SHORT
): Snackbar = Snackbar.make(this, text, length).also {
    it.show()
}

fun Context.showShortToast(text: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, length).show()
}

inline fun <reified T : Activity> Context.launchActivity() {
    startActivity(Intent(this, T::class.java))
}

fun Context.showYesNoDialog(
    @StringRes title: Int,
    @StringRes message: Int,
    @StringRes positiveText: Int = R.string.agree,
    @StringRes negativeText: Int = R.string.disagree,
    onNegativeAction: () -> Unit = {},
    onPositiveAction: () -> Unit = {}
) = MaterialDialog(this)
    .title(title)
    .message(message)
    .show {
        setOnCancelListener { onNegativeAction() }
        positiveButton(positiveText) { dialog ->
            dialog.dismiss()
            onPositiveAction()
        }
        negativeButton(negativeText) { dialog ->
            dialog.dismiss()
            onNegativeAction()
        }
    }
