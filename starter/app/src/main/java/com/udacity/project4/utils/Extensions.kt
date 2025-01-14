package com.udacity.project4.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
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
    val params: ViewGroup.LayoutParams = it.view.layoutParams
    if (params is CoordinatorLayout.LayoutParams) {
        params.gravity = Gravity.TOP
    } else {
        (params as FrameLayout.LayoutParams).gravity = Gravity.TOP
    }
    it.view.layoutParams = params
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
    autoDismiss: Boolean = true,
    onNegativeAction: () -> Unit = {},
    onPositiveAction: () -> Unit = {}
): MaterialDialog {
    val dialog = MaterialDialog(this).apply {
        title(title)
        message(message)
        if (!autoDismiss) noAutoDismiss()
    }

    dialog.show {
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

    return dialog
}

fun Activity.launchPermissionSettingsActivity() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    })
}

fun MaterialDialog.tryDimiss() {
    try {
        this.dismiss()
    } catch (e: Exception) {
        Log.e("DIALOGDISMISS", e.toString())
    }
}

fun Context.isAllowed(permission: String): Boolean =
    ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.areAllowed(permissions: Array<String>): Boolean {
    permissions.forEach { permission ->
        if (!isAllowed(permission))
            return false
    }

    return true
}

fun Activity.shouldShowBackgroundLocationDialog(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        shouldShowRequestPermissionRationale(backgroundPermission) else false

fun Fragment.hasFineLocationPermission() =
    requireContext().applicationContext.isAllowed(fineLocationPermission)

fun Context.hasForegroundPermissions(): Boolean = areAllowed(arrayOf(fineLocationPermission))

fun Context.hasBackgroundPermissions(): Boolean {
    var requiredPermissions = arrayOf(fineLocationPermission)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        requiredPermissions += backgroundPermission

    return areAllowed(requiredPermissions)
}

fun Activity.showGPSLocationRequiredDialog(
    @StringRes title: Int = R.string.location_required_title,
    @StringRes message: Int = R.string.location_required_error,
    autoDismiss: Boolean = true,
    onNegativeAction: () -> Unit = { },
    onPositiveAction: () -> Unit
): MaterialDialog = showYesNoDialog(
    title = title,
    message = message,
    autoDismiss = autoDismiss,
    positiveText = R.string.allow,
    negativeText = R.string.go_back,
    onNegativeAction = onNegativeAction,
    onPositiveAction = onPositiveAction
)

fun Activity.showPermissionsRequiredDialog(
    @StringRes title: Int = R.string.permission_required_title,
    @StringRes message: Int = R.string.permission_required_explanation,
    autoDismiss: Boolean = true,
    onNegativeAction: () -> Unit = { },
    onPositiveAction: () -> Unit
): MaterialDialog = showYesNoDialog(
    title = title,
    message = message,
    autoDismiss = autoDismiss,
    positiveText = R.string.allow,
    negativeText = R.string.go_back,
    onNegativeAction = onNegativeAction,
    onPositiveAction = onPositiveAction
)

fun <T : Any> MutableLiveData<T>.postSelf() {
    postValue(value)
}

fun <L : Any?> MediatorLiveData<*>.addSourceThenPost(source: LiveData<L>, onSourceValueChanged: (L?) -> Unit) {
    addSource(source) {
        onSourceValueChanged(it)
        postSelf()
    }
}