package hr.cutva.forceupdate

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RemoteConfig {

    val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    private val _minVersionState = MutableStateFlow(0L)
    val minVersionState: StateFlow<Long> = _minVersionState

    fun setup() {
        fetchAndActivate()
        listenForUpdates()
    }

    private fun fetchAndActivate() {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    updateMinVersion()
                }
            }
    }

    private fun listenForUpdates() {
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate : ConfigUpdate) {
                if (configUpdate.updatedKeys.contains(Params.MinVersion.key)) {
                    remoteConfig.activate().addOnCompleteListener {
                        updateMinVersion()
                    }
                }
            }

            override fun onError(error : FirebaseRemoteConfigException) {}
        })
    }

    private fun updateMinVersion() {
        _minVersionState.value = remoteConfig.getLong(Params.MinVersion.key)
    }

    private enum class Params(val key: String) {
        MinVersion("minVersion")
    }

}