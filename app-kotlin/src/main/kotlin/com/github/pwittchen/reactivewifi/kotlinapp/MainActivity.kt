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
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.wifi.ScanResult
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import com.github.pwittchen.reactivewifi.AccessRequester
import com.github.pwittchen.reactivewifi.ReactiveWifi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : Activity() {
  private var wifiSubscription: Disposable? = null
  private var signalLevelSubscription: Disposable? = null
  private var supplicantSubscription: Disposable? = null
  private var wifiInfoSubscription: Disposable? = null
  private var wifiStateSubscription: Disposable? = null

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

    if (!isFineOrCoarseLocationPermissionGranted()) {
      requestCoarseLocationPermission()
    } else if (isFineOrCoarseLocationPermissionGranted() || IS_PRE_M_ANDROID) {
      startWifiAccessPointsSubscription()
    }

    startWifiSignalLevelSubscription()
    startSupplicantSubscription()
    startWifiInfoSubscription()
    startWifiStateSubscription()
  }

  private fun startWifiSignalLevelSubscription() {
    signalLevelSubscription = ReactiveWifi
        .observeWifiSignalLevel(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { level ->
          Log.d(TAG, level.toString())
          val description = level.description
          wifi_signal_level.text = WIFI_SIGNAL_LEVEL_MESSAGE + description
        }
  }

  @SuppressLint("MissingPermission")
  private fun startWifiAccessPointsSubscription() {
    if (!AccessRequester.isLocationEnabled(this)) {
      AccessRequester.requestLocationAccess(this)
      return
    }

    wifiSubscription = ReactiveWifi
        .observeWifiAccessPoints(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { scanResults ->
          displayAccessPoints(scanResults)
        }
  }

  private fun startSupplicantSubscription() {
    supplicantSubscription = ReactiveWifi
        .observeSupplicantState(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { supplicantState ->
          Log.d("ReactiveWifi", "New supplicant state: " + supplicantState.toString())
        }
  }

  private fun startWifiInfoSubscription() {
    wifiInfoSubscription = ReactiveWifi
        .observeWifiAccessPointChanges(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { wifiInfo ->
          Log.d("ReactiveWifi", "New BSSID: " + wifiInfo.bssid)
        }
  }

  private fun startWifiStateSubscription() {
    wifiStateSubscription = ReactiveWifi
        .observeWifiStateChange(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { wifiState ->
          Log.d("ReactiveWifi", wifiState.description)
          wifi_state_change.text = WIFI_STATE_CHANGE_MESSAGE + wifiState.description
        }
  }

  private fun displayAccessPoints(scanResults: List<ScanResult>) {
    val ssids = scanResults.map { it.SSID }

    val itemLayoutId = android.R.layout.simple_list_item_1
    access_points.adapter = ArrayAdapter(this, itemLayoutId, ssids)
  }

  override fun onPause() {
    super.onPause()
    safelyUnsubscribe(wifiSubscription, signalLevelSubscription, supplicantSubscription,
        wifiInfoSubscription)
  }

  private fun safelyUnsubscribe(vararg subscriptions: Disposable?) {
    subscriptions
        .filterNotNull()
        .filterNot { it.isDisposed }
        .forEach { it.dispose() }
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

  private fun isFineOrCoarseLocationPermissionGranted(): Boolean {
    val isAndroidMOrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    val isFineLocationPermissionGranted = isGranted(ACCESS_FINE_LOCATION)
    val isCoarseLocationPermissionGranted = isGranted(ACCESS_COARSE_LOCATION)

    return isAndroidMOrHigher && (isFineLocationPermissionGranted || isCoarseLocationPermissionGranted)
  }

  private fun isGranted(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED
  }
}
