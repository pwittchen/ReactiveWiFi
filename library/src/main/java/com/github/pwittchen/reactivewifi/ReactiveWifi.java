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
package com.github.pwittchen.reactivewifi;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;

/**
 * ReactiveWiFi is an Android library
 * listening available WiFi Access Points change of the WiFi signal strength
 * with RxJava Observables. It can be easily used with RxAndroid.
 */
public class ReactiveWifi {

  private final static String LOG_TAG = "ReactiveWifi";

  private ReactiveWifi() {
  }

  /**
   * Observes WiFi Access Points.
   * Returns fresh list of Access Points
   * whenever WiFi signal strength changes.
   *
   * @param context Context of the activity or an application
   * @return RxJava Observable with list of WiFi scan results
   */
  @SuppressLint("MissingPermission") @RequiresPermission(allOf = {
      ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, CHANGE_WIFI_STATE, ACCESS_WIFI_STATE
  }) public static Observable<List<ScanResult>> observeWifiAccessPoints(final Context context) {
    @SuppressLint("WifiManagerPotentialLeak") final WifiManager wifiManager =
        (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

    if (wifiManager != null) {
      wifiManager.startScan(); // without starting scan, we may never receive any scan results
    } else {
      Log.w(LOG_TAG, "WifiManager was null, so WiFi scan was not started");
    }

    final IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
    filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

    return Observable.create(new ObservableOnSubscribe<List<ScanResult>>() {

      @Override public void subscribe(final ObservableEmitter<List<ScanResult>> emitter)
          throws Exception {
        final BroadcastReceiver receiver = createWifiScanResultsReceiver(emitter, wifiManager);

        if (wifiManager != null) {
          context.registerReceiver(receiver, filter);
        } else {
          emitter.onError(new RuntimeException(
              "WifiManager was null, so BroadcastReceiver for Wifi scan results "
                  + "cannot be registered"));
        }

        Disposable disposable = disposeInUiThread(new Action() {
          @Override public void run() {
            tryToUnregisterReceiver(context, receiver);
          }
        });

        emitter.setDisposable(disposable);
      }
    });
  }

  @NonNull protected static BroadcastReceiver createWifiScanResultsReceiver(
      final ObservableEmitter<List<ScanResult>> emitter, final WifiManager wifiManager) {
    return new BroadcastReceiver() {
      @Override public void onReceive(Context context1, Intent intent) {
        wifiManager.startScan(); // we need to start scan again to get fresh results ASAP
        emitter.onNext(wifiManager.getScanResults());
      }
    };
  }

  /**
   * Observes WiFi signal level with predefined max num levels.
   * Returns WiFi signal level as enum with information about current level
   *
   * @param context Context of the activity or an application
   * @return WifiSignalLevel as an enum
   */
  @RequiresPermission(ACCESS_WIFI_STATE)
  public static Observable<WifiSignalLevel> observeWifiSignalLevel(final Context context) {
    return observeWifiSignalLevel(context, WifiSignalLevel.getMaxLevel()).map(
        new Function<Integer, WifiSignalLevel>() {
          @Override public WifiSignalLevel apply(Integer level) throws Exception {
            return WifiSignalLevel.fromLevel(level);
          }
        });
  }

  /**
   * Observes WiFi signal level.
   * Returns WiFi signal level as an integer
   *
   * @param context Context of the activity or an application
   * @param numLevels The number of levels to consider in the calculated level as Integer
   * @return RxJava Observable with WiFi signal level
   */
  @RequiresPermission(ACCESS_WIFI_STATE) public static Observable<Integer> observeWifiSignalLevel(
      final Context context, final int numLevels) {
    final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    final IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.RSSI_CHANGED_ACTION);

    return Observable.create(new ObservableOnSubscribe<Integer>() {
      @Override public void subscribe(final ObservableEmitter<Integer> emitter) throws Exception {
        final BroadcastReceiver receiver =
            createSignalLevelReceiver(emitter, wifiManager, numLevels);

        if (wifiManager != null) {
          context.registerReceiver(receiver, filter);
        } else {
          emitter.onError(new RuntimeException(
              "WifiManager is null, so BroadcastReceiver for Wifi signal level "
                  + "cannot be registered"));
        }

        Disposable disposable = disposeInUiThread(new Action() {
          @Override public void run() {
            tryToUnregisterReceiver(context, receiver);
          }
        });

        emitter.setDisposable(disposable);
      }
    }).defaultIfEmpty(0);
  }

  @NonNull protected static BroadcastReceiver createSignalLevelReceiver(
      final ObservableEmitter<Integer> emitter,
      final WifiManager wifiManager, final int numLevels) {
    return new BroadcastReceiver() {
      @Override public void onReceive(Context context, Intent intent) {
        final int rssi = wifiManager.getConnectionInfo().getRssi();
        final int level = WifiManager.calculateSignalLevel(rssi, numLevels);
        emitter.onNext(level);
      }
    };
  }

  /**
   * Observes the current WPA supplicant state.
   * Returns the current WPA supplicant as a member of the {@link SupplicantState} enumeration,
   * returning {@link SupplicantState#UNINITIALIZED} if WiFi is not enabled.
   *
   * @param context Context of the activity or an application
   * @return RxJava Observable with SupplicantState
   */
  @RequiresPermission(ACCESS_WIFI_STATE)
  public static Observable<SupplicantState> observeSupplicantState(final Context context) {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

    return Observable.create(new ObservableOnSubscribe<SupplicantState>() {
      @Override public void subscribe(final ObservableEmitter<SupplicantState> emitter)
          throws Exception {
        final BroadcastReceiver receiver = createSupplicantStateReceiver(emitter);

        context.registerReceiver(receiver, filter);

        Disposable disposable = disposeInUiThread(new Action() {
          @Override public void run() {
            tryToUnregisterReceiver(context, receiver);
          }
        });

        emitter.setDisposable(disposable);
      }
    }).defaultIfEmpty(SupplicantState.UNINITIALIZED);
  }

  @NonNull protected static BroadcastReceiver createSupplicantStateReceiver(
      final ObservableEmitter<SupplicantState> emitter) {
    return new BroadcastReceiver() {
      @Override public void onReceive(Context context, Intent intent) {
        SupplicantState supplicantState =
            intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);

        if ((supplicantState != null) && SupplicantState.isValidState(supplicantState)) {
          emitter.onNext(supplicantState);
        }
      }
    };
  }

  /**
   * Observes the WiFi network the device is connected to.
   * Returns the current WiFi network information as a {@link WifiInfo} object.
   *
   * @param context Context of the activity or an application
   * @return RxJava Observable with WifiInfo
   */
  @RequiresPermission(ACCESS_WIFI_STATE)
  public static Observable<WifiInfo> observeWifiAccessPointChanges(final Context context) {
    final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    final IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

    return Observable.create(new ObservableOnSubscribe<WifiInfo>() {
      @Override public void subscribe(final ObservableEmitter<WifiInfo> emitter) throws Exception {
        final BroadcastReceiver receiver =
            createAccessPointChangesReceiver(emitter, wifiManager);

        context.registerReceiver(receiver, filter);

        Disposable disposable = disposeInUiThread(new Action() {
          @Override public void run() {
            tryToUnregisterReceiver(context, receiver);
          }
        });

        emitter.setDisposable(disposable);
      }
    });
  }

  @NonNull protected static BroadcastReceiver createAccessPointChangesReceiver(
      final ObservableEmitter<WifiInfo> emitter, final WifiManager wifiManager) {
    return new BroadcastReceiver() {
      @Override public void onReceive(Context context, Intent intent) {
        SupplicantState supplicantState =
            intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
        if (supplicantState == SupplicantState.COMPLETED) {
          emitter.onNext(wifiManager.getConnectionInfo());
        }
      }
    };
  }

  /**
   * Observes WiFi State Change Action
   * Returns wifi state
   * whenever WiFi state changes such like enable,disable,enabling,disabling or Unknown
   *
   * @param context Context of the activity or an application
   * @return RxJava Observable with different state change
   */
  @RequiresPermission(ACCESS_WIFI_STATE) public static Observable<WifiState> observeWifiStateChange(
      final Context context) {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

    return Observable.create(new ObservableOnSubscribe<WifiState>() {
      @Override public void subscribe(final ObservableEmitter<WifiState> emitter) throws Exception {
        final BroadcastReceiver receiver = createWifiStateChangeReceiver(emitter);
        context.registerReceiver(receiver, filter);
        Disposable disposable = disposeInUiThread(new Action() {
          @Override public void run() {
            tryToUnregisterReceiver(context, receiver);
          }
        });

        emitter.setDisposable(disposable);
      }
    });
  }

  @NonNull protected static BroadcastReceiver createWifiStateChangeReceiver(
      final ObservableEmitter<WifiState> emitter) {
    return new BroadcastReceiver() {
      @Override public void onReceive(Context context, Intent intent) {
        //we receive whenever the wifi state is change
        int wifiState =
            intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        emitter.onNext(WifiState.fromState(wifiState));
      }
    };
  }

  protected static void tryToUnregisterReceiver(final Context context,
      final BroadcastReceiver receiver) {
    try {
      context.unregisterReceiver(receiver);
    } catch (Exception exception) {
      onError("receiver was already unregistered", exception);
    }
  }

  protected static void onError(final String message, final Exception exception) {
    Log.e(LOG_TAG, message, exception);
  }

  private static Disposable disposeInUiThread(final Action action) {
    return Disposables.fromAction(new Action() {
      @Override public void run() throws Exception {
        if (Looper.getMainLooper() == Looper.myLooper()) {
          action.run();
        } else {
          final Scheduler.Worker inner = AndroidSchedulers.mainThread().createWorker();
          inner.schedule(new Runnable() {
            @Override public void run() {
              try {
                action.run();
              } catch (Exception e) {
                onError("Could not unregister receiver in UI Thread", e);
              }
              inner.dispose();
            }
          });
        }
      }
    });
  }
}
