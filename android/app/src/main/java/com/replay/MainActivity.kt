package com.replay

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.app.replaybuffer.ReplayBufferModule
import com.app.replaybuffer.service.ReplayBufferService
import com.app.replaybuffer.utils.Constants
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import java.lang.ref.WeakReference

class MainActivity : ReactActivity() {

  private var projectionCallback: ((Int, Intent?) -> Unit)? = null

  private val projectionLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = projectionCallback
        projectionCallback = null
        callback?.invoke(result.resultCode, result.data)
      }

  override fun getMainComponentName(): String = "Replay"

  override fun createReactActivityDelegate(): ReactActivityDelegate =
      DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    instanceRef = WeakReference(this)
    onBackPressedDispatcher.addCallback(
        this,
        object : OnBackPressedCallback(true) {
          override fun handleOnBackPressed() {
            if (ReplayBufferService.isRecordingActive) {
              // Keep the recording session alive. Finishing the Activity can cause
              // OEMs/Android 14 capture sessions to revoke MediaProjection.
              moveTaskToBack(true)
            } else {
              isEnabled = false
              onBackPressedDispatcher.onBackPressed()
            }
          }
        }
    )
    handleNotificationIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleNotificationIntent(intent)
  }

  override fun onDestroy() {
    if (instanceRef?.get() === this) {
      instanceRef = null
    }
    projectionCallback = null
    super.onDestroy()
  }

  fun requestScreenCapture(intent: Intent, callback: (Int, Intent?) -> Unit) {
    projectionCallback = callback
    projectionLauncher.launch(intent)
  }

  private fun handleNotificationIntent(intent: Intent?) {
    if (intent?.action == Constants.ACTION_START_FROM_NOTIFICATION) {
      window.decorView.post {
        ReplayBufferModule.emitNotificationStartRequest()
      }
    }
  }

  companion object {
    @Volatile
    private var instanceRef: WeakReference<MainActivity>? = null

    fun getInstance(): MainActivity? = instanceRef?.get()
  }
}
