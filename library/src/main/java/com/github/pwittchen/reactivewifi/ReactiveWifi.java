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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;
import java.util.List;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * ReactiveWiFi is an Android library
 * listening available WiFi Access Points change of the WiFi signal strength
 * with RxJava Observables. It can be easily used with RxAndroid.
 */
public class ReactiveWifi {

  /**
   * Observes WiFi connection
   *
   * @param context Context of the activity or an application
   * @return RxJava Observable with Boolean true means WiFi is On else false.
   */

  public Observable<Boolean> observeWifiConnection(final Context context) {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);

    return Observable.create(new Observable.OnSubscribe<Boolean>() {
      @Override public void call(final Subscriber<? super Boolean> subscriber) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
              subscriber.onNext(
                  intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
            }
          }
        };

        context.registerReceiver(receiver, filter);

        subscriber.add(unsubscribeInUiThread(new Action0() {
          @Override public void call() {
            context.unregisterReceiver(receiver);
          }
        }));
      }
    });
  }

  /**
   * Observes WiFi Access Points.
   * Returns fresh list of Access Points
   * whenever WiFi signal strength changes.
   *
   * @param context Context of the activity or an application
   * @return RxJava Observable with list of WiFi scan results
   */
  public Observable<List<ScanResult>> observeWifiAccessPoints(final Context context) {
    final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    wifiManager.startScan(); // without starting scan, we may never receive any scan results

    final IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
    filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

    return Observable.create(new Observable.OnSubscribe<List<ScanResult>>() {
      @Override public void call(final Subscriber<? super List<ScanResult>> subscriber) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            wifiManager.startScan(); // we need to start scan again to get fresh results ASAP
            subscriber.onNext(wifiManager.getScanResults());
          }
        };

        context.registerReceiver(receiver, filter);

        subscriber.add(unsubscribeInUiThread(new Action0() {
          @Override public void call() {
            context.unregisterReceiver(receiver);
          }
        }));
      }
    });
  }

  /**
   * Observes WiFi signal level with predefined max num levels.
   * Returns WiFi signal level as enum with information about current level
   *
   * @param context Context of the activity or an application
   * @return WifiSignalLevel as an enum
   */
  public Observable<WifiSignalLevel> observeWifiSignalLevel(final Context context) {
    return observeWifiSignalLevel(context, WifiSignalLevel.getMaxLevel()).map(
        new Func1<Integer, WifiSignalLevel>() {
          @Override public WifiSignalLevel call(Integer level) {
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
  public Observable<Integer> observeWifiSignalLevel(final Context context, final int numLevels) {
    final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    final IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.RSSI_CHANGED_ACTION);

    return Observable.create(new Observable.OnSubscribe<Integer>() {
      @Override public void call(final Subscriber<? super Integer> subscriber) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            final int rssi = wifiManager.getConnectionInfo().getRssi();
            final int level = WifiManager.calculateSignalLevel(rssi, numLevels);
            subscriber.onNext(level);
          }
        };

        context.registerReceiver(receiver, filter);

        subscriber.add(unsubscribeInUiThread(new Action0() {
          @Override public void call() {
            context.unregisterReceiver(receiver);
          }
        }));
      }
    }).defaultIfEmpty(0);
  }

  /**
   * Observes the current WPA supplicant state.
   * Returns the current WPA supplicant as a member of the {@link SupplicantState} enumeration,
   * returning {@link SupplicantState#UNINITIALIZED} if WiFi is not enabled.
   *
   * @param context Context of the activity or an application
   * @return RxJava Observable with SupplicantState
   */
  public Observable<SupplicantState> observeSupplicantState(final Context context) {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

    return Observable.create(new Observable.OnSubscribe<SupplicantState>() {
      @Override public void call(final Subscriber<? super SupplicantState> subscriber) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            SupplicantState supplicantState =
                intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);

            if ((supplicantState != null) && SupplicantState.isValidState(supplicantState)) {
              subscriber.onNext(supplicantState);
            }
          }
        };

        context.registerReceiver(receiver, filter);

        subscriber.add(unsubscribeInUiThread(new Action0() {
          @Override public void call() {
            context.unregisterReceiver(receiver);
          }
        }));
      }
    }).defaultIfEmpty(SupplicantState.UNINITIALIZED);
  }

  /**
   * Observes the WiFi network the device is connected to.
   * Returns the current WiFi network information as a {@link WifiInfo} object.
   *
   * @param context Context of the activity or an application
   * @return RxJava Observable with WifiInfo
   */
  public Observable<WifiInfo> observeWifiAccessPointChanges(final Context context) {
    final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    final IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

    return Observable.create(new Observable.OnSubscribe<WifiInfo>() {
      @Override public void call(final Subscriber<? super WifiInfo> subscriber) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            SupplicantState supplicantState =
                intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            if (supplicantState == SupplicantState.COMPLETED) {
              subscriber.onNext(wifiManager.getConnectionInfo());
            }
          }
        };

        context.registerReceiver(receiver, filter);

        subscriber.add(unsubscribeInUiThread(new Action0() {
          @Override public void call() {
            context.unregisterReceiver(receiver);
          }
        }));
      }
    });
  }

  private Subscription unsubscribeInUiThread(final Action0 unsubscribe) {
    return Subscriptions.create(new Action0() {
      @Override public void call() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
          unsubscribe.call();
        } else {
          final Scheduler.Worker inner = AndroidSchedulers.mainThread().createWorker();
          inner.schedule(new Action0() {
            @Override public void call() {
              unsubscribe.call();
              inner.unsubscribe();
            }
          });
        }
      }
    });
  }
}
