package hr.cutva.forceupdate

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.asLiveData
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import hr.cutva.forceupdate.ui.theme.ForceUpdateTheme

private const val TAG = "ForceUpdate"

class MainActivity : ComponentActivity() {

    private val remoteConfig = RemoteConfig()

    private lateinit var appUpdateManager: AppUpdateManager

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            if (result.resultCode != RESULT_OK) {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        remoteConfig.setup()
        appUpdateManager = AppUpdateManagerFactory.create(this)

        remoteConfig.minVersionState.asLiveData().observe(this) {
            onMinVersionChanged(it)
        }

        enableEdgeToEdge()
        setContent {
            ForceUpdateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handleUpdateInProgress()
    }

    private fun handleUpdateInProgress() {
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    requestUpdate(appUpdateInfo)
                }
            }
    }

    private fun onMinVersionChanged(minVersion: Long) {
        if (shouldForceUpdate(minVersion)) {
            checkForUpdateAvailability(minVersion)
        } else {
            Log.d(TAG, "No update needed")
        }
    }

    private fun checkForUpdateAvailability(minVersion: Long) {
        Log.d(TAG, "Check for update availability")
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                Log.d(TAG, "Update available: ${appUpdateInfo.availableVersionCode()}")
                if (shouldForceUpdate(minVersion)) {
                    requestUpdate(appUpdateInfo)
                }
            }
        }
    }

    private fun shouldForceUpdate(minVersion: Long): Boolean {
        val currentVersion = BuildConfig.VERSION_CODE.toLong()
        Log.d(
            TAG,
            "Should force update? Current version: $currentVersion, min version: $minVersion"
        )
        return minVersion > currentVersion
    }

    private fun requestUpdate(appUpdateInfo: AppUpdateInfo) {
        Log.d(TAG, "Requesting update")
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            activityResultLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ForceUpdateTheme {
        Greeting("Android")
    }
}