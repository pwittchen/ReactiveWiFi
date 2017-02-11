/*
 * Copyright (C) 2016 Piotr Wittchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pwittchen.reactivewifi.kotlinapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import com.github.pwittchen.reactivewifi.ReactiveWifi
import kotlinx.android.synthetic.main.activity_main.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*

class MainActivity : Activity() {
  private var reactiveWifi: ReactiveWifi? = null
  private var wifiSubscription: Subscription? = null
  private var signalLevelSubscription: Subscription? = null
  private var supplicantSubscription: Subscription? = null
  private var wifiInfoSubscription: Subscription? = null
  private var wifiStateSubscription: Subscription? = null

  companion object {
    private val PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1000
    private val TAG = "ReactiveWifi"
    private val WIFI_SIGNAL_LEVEL_MESSAGE = "WiFi signal level: "
    private val WIFI_STATE_CHANGE_MESSAGE = "WiFi State: "
    val IS_PRE_M_ANDROID = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }

  override fun onResume() {
    super.onResume()

    reactiveWifi = ReactiveWifi()
    signalLevelSubscription = (reactiveWifi as ReactiveWifi)
        .observeWifiSignalLevel(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { level ->
          Log.d(TAG, level.toString())
          val description = level.description
          wifi_signal_level.text = WIFI_SIGNAL_LEVEL_MESSAGE + description
        }

    if (!isCoarseLocationPermissionGranted) {
      requestCoarseLocationPermission()
    } else if (isCoarseLocationPermissionGranted || IS_PRE_M_ANDROID) {
      startWifiAccessPointsSubscription()
    }

    startSupplicantSubscription()
    startWifiInfoSubscription()
    startWifiStateSubscription()
  }

  private fun startWifiAccessPointsSubscription() {
    wifiSubscription = (reactiveWifi as ReactiveWifi)
        .observeWifiAccessPoints(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { scanResults ->
          displayAccessPoints(scanResults)
        }
  }

  private fun startSupplicantSubscription() {
    supplicantSubscription = (reactiveWifi as ReactiveWifi)
        .observeSupplicantState(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { supplicantState ->
          Log.d("ReactiveWifi", "New supplicant state: " + supplicantState.toString())
        }
  }

  private fun startWifiInfoSubscription() {
    wifiInfoSubscription = (reactiveWifi as ReactiveWifi)
        .observeWifiAccessPointChanges(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { wifiInfo ->
          Log.d("ReactiveWifi", "New BSSID: " + wifiInfo.bssid)
        }
  }

  private fun startWifiStateSubscription() {
    wifiStateSubscription = (reactiveWifi as ReactiveWifi)
        .observeWifiStateChange(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { wifiState ->
          Log.d("ReactiveWifi", wifiState.description)
          wifi_state_change.text = WIFI_STATE_CHANGE_MESSAGE + wifiState.description
        }
  }

  private fun displayAccessPoints(scanResults: List<ScanResult>) {
    val ssids = ArrayList<String>()

    for (scanResult in scanResults) {
      ssids.add(scanResult.SSID)
    }

    val itemLayoutId = android.R.layout.simple_list_item_1
    access_points.adapter = ArrayAdapter(this, itemLayoutId, ssids)
  }

  override fun onPause() {
    super.onPause()
    safelyUnsubscribe(wifiSubscription, signalLevelSubscription, supplicantSubscription,
        wifiInfoSubscription)
  }

  private fun safelyUnsubscribe(vararg subscriptions: Subscription?) {
    for (subscription in subscriptions) {
      if (subscription != null && !subscription.isUnsubscribed) {
        subscription.unsubscribe()
      }
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
      grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    val isCoarseLocation = requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
    val permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED

    if (isCoarseLocation && permissionGranted && wifiSubscription == null) {
      startWifiAccessPointsSubscription()
    }
  }

  private fun requestCoarseLocationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
          PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION)
    }
  }

  private val isCoarseLocationPermissionGranted: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
