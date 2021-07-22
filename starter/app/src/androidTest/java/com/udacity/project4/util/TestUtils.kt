package com.udacity.project4.util

import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Root
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.runner.permission.PermissionRequester
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.*
import kotlin.random.Random


fun generateRandomReminders(amountToGenerate: Int = 1): List<ReminderDTO> {
    val amount = amountToGenerate.takeIf { it > 0 } ?: 1
    val list = mutableListOf<ReminderDTO>()

    for (i in 1..amount) {
        list.add(
            ReminderDTO(
                id = i.toString(),
                title = UUID.randomUUID().toString(),
                description = UUID.randomUUID().toString(),
                location = UUID.randomUUID().toString(),
                longitude = Random.nextDouble(),
                latitude = Random.nextDouble()
            )
        )
    }

    return list
}

fun waitForInSeconds(delay: Long): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isRoot()
        }

        override fun getDescription(): String {
            return "wait for " + delay + "milliseconds"
        }

        override fun perform(uiController: UiController, view: View?) {
            uiController.loopMainThreadForAtLeast(delay)
        }
    }
}

inline fun <reified F : Fragment> launchFragmentScenario(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.AppTheme,
    initialState: Lifecycle.State = Lifecycle.State.RESUMED,
    factory: FragmentFactory? = null
) = launchFragmentInContainer<F>(fragmentArgs, themeResId, initialState, factory)

@ExperimentalCoroutinesApi
class MainCoroutineRuleAndroidTests(val dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()) :
    TestWatcher(),
    TestCoroutineScope by TestCoroutineScope(dispatcher) {
    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
        cleanupTestCoroutines()
    }
}

fun Fragment.setNavController(
    navHostController: TestNavHostController
) = Navigation.setViewNavController(requireView(), navHostController)

fun putLocationPermissions() {
    PermissionRequester().apply {
        addPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (BuildConfig.VERSION_CODE >= Build.VERSION_CODES.Q)
            addPermissions(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        requestPermissions()
    }
}

class ToastMatcher : TypeSafeMatcher<Root>() {
    override fun describeTo(description: org.hamcrest.Description) {
        description.appendText("is toast")
    }

    override fun matchesSafely(root: Root): Boolean {
        val type: Int = root.windowLayoutParams?.get()?.type!!
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            val decorView = root.decorView
            val windowToken: IBinder = decorView?.windowToken!!
            val appToken: IBinder = decorView.applicationWindowToken
            if (windowToken === appToken) return true
        }
        return false
    }
}

//fun withIdleScope(block: suspend () -> Unit) =
//    withContext(Dispatchers.IO) { block()