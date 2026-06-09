package com.replay

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.app.replaybuffer.ReplayBufferPackage
import com.app.replaybuffer.utils.CrashLogger

class MainApplication : Application(), ReactApplication {

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList =
        PackageList(this).packages.apply {
          // Packages that cannot be autolinked yet can be added manually here, for example:
          // add(MyReactNativePackage())
          add(ReplayBufferPackage())
        },
    )
  }

  override fun onCreate() {
    super.onCreate()
    CrashLogger.install(this)
    loadReactNative(this)
  }
}
