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
package com.github.pwittchen.reactivewifi.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.github.pwittchen.reactivewifi.ReactiveWifi;
import com.github.pwittchen.reactivewifi.WifiSignalLevel;
import com.github.pwittchen.reactivewifi.WifiState;
import java.util.ArrayList;
import java.util.List;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity {
  public static final boolean IS_PRE_M_ANDROID = Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
  private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1000;
  private static final String TAG = "ReactiveWifi";
  private static final String WIFI_SIGNAL_LEVEL_MESSAGE = "WiFi signal level: ";
  private static final String WIFI_STATE_MESSAGE = "WiFi State: ";
  private TextView tvWifiSignalLevel;
  private TextView tvWifiState;
  private ListView lvAccessPoints;
  private ReactiveWifi reactiveWifi;
  private Subscription wifiSubscription;
  private Subscription signalLevelSubscription;
  private Subscription supplicantSubscription;
  private Subscription wifiStateSubscription;
  private Subscription wifiInfoSubscription;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    lvAccessPoints = (ListView) findViewById(R.id.access_points);
    tvWifiSignalLevel = (TextView) findViewById(R.id.wifi_signal_level);
    tvWifiState = (TextView) findViewById(R.id.wifi_state_change);
  }

  @Override protected void onResume() {
    super.onResume();

    reactiveWifi = new ReactiveWifi();

    signalLevelSubscription = reactiveWifi.observeWifiSignalLevel(getApplicationContext())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<WifiSignalLevel>() {
          @Override public void call(final WifiSignalLevel level) {
            Log.d(TAG, level.toString());
            final String description = level.description;
            tvWifiSignalLevel.setText(WIFI_SIGNAL_LEVEL_MESSAGE.concat(description));
          }
        });

    if (!isCoarseLocationPermissionGranted()) {
      requestCoarseLocationPermission();
    } else if (isCoarseLocationPermissionGranted() || IS_PRE_M_ANDROID) {
      startWifiAccessPointsSubscription();
    }

    startSupplicantSubscription();
    startWifiInfoSubscription();
    startWifiStateSubscription();
  }

  private void startWifiAccessPointsSubscription() {
    wifiSubscription = reactiveWifi.observeWifiAccessPoints(getApplicationContext())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<List<ScanResult>>() {
          @Override public void call(final List<ScanResult> scanResults) {
            displayAccessPoints(scanResults);
          }
        });
  }

  private void startSupplicantSubscription() {
    supplicantSubscription = reactiveWifi.observeSupplicantState(getApplicationContext())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<SupplicantState>() {
          @Override public void call(SupplicantState supplicantState) {
            Log.d("ReactiveWifi", "New supplicant state: " + supplicantState.toString());
          }
        });
  }

  private void startWifiInfoSubscription() {
    wifiInfoSubscription = reactiveWifi.observeWifiAccessPointChanges(getApplicationContext())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<WifiInfo>() {
          @Override public void call(WifiInfo wifiInfo) {
            Log.d("ReactiveWifi", "New BSSID: " + wifiInfo.getBSSID());
          }
        });
  }

  private void startWifiStateSubscription() {
    wifiStateSubscription = reactiveWifi.observeWifiStateChange(getApplicationContext())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<WifiState>() {
          @Override public void call(WifiState wifiState) {
            Log.d(TAG, "call: " + wifiState.name());
            tvWifiState.setText(WIFI_STATE_MESSAGE.concat(wifiState.description));
          }
        });
  }

  private void displayAccessPoints(List<ScanResult> scanResults) {
    final List<String> ssids = new ArrayList<>();

    for (ScanResult scanResult : scanResults) {
      ssids.add(scanResult.SSID);
    }

    int itemLayoutId = android.R.layout.simple_list_item_1;
    lvAccessPoints.setAdapter(new ArrayAdapter<>(this, itemLayoutId, ssids));
  }

  @Override protected void onPause() {
    super.onPause();
    safelyUnsubscribe(wifiSubscription, signalLevelSubscription, supplicantSubscription,
        wifiInfoSubscription, wifiStateSubscription);
  }

  private void safelyUnsubscribe(Subscription... subscriptions) {
    for (Subscription subscription : subscriptions) {
      if (subscription != null && !subscription.isUnsubscribed()) {
        subscription.unsubscribe();
      }
    }
  }

  @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    final boolean isCoarseLocation = requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION;
    final boolean permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

    if (isCoarseLocation && permissionGranted && wifiSubscription == null) {
      startWifiAccessPointsSubscription();
    }
  }

  private void requestCoarseLocationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
          PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
    }
  }

  private boolean isCoarseLocationPermissionGranted() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        == PackageManager.PERMISSION_GRANTED;
  }
}
