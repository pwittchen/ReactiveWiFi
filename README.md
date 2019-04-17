# ReactiveWiFi [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-ReactiveWiFi-brightgreen.svg?style=true)](https://android-arsenal.com/details/1/3913)

Android library listening available WiFi Access Points and related information with RxJava Observables.

| Current Branch | Branch  | Artifact Id | Build Status  | Maven Central |
|:--------------:|:-------:|:-----------:|:-------------:|:-------------:|
| | [`RxJava1.x`](https://github.com/pwittchen/ReactiveWifi/tree/RxJava1.x) | `reactivewifi` | [![Build Status for RxJava1.x](https://travis-ci.org/pwittchen/ReactiveWiFi.svg?branch=RxJava1.x)](https://travis-ci.org/pwittchen/ReactiveWiFi) | ![Maven Central](https://img.shields.io/maven-central/v/com.github.pwittchen/reactivewifi.svg?style=flat) |
| :ballot_box_with_check: | [`RxJava2.x`](https://github.com/pwittchen/ReactiveWifi/tree/RxJava2.x) | `reactivewifi-rx2` | [![Build Status for RxJava2.x](https://travis-ci.org/pwittchen/ReactiveWiFi.svg?branch=RxJava2.x)](https://travis-ci.org/pwittchen/ReactiveWiFi) | ![Maven Central](https://img.shields.io/maven-central/v/com.github.pwittchen/reactivewifi-rx2.svg?style=flat) |

This library is one of the successors of the [NetworkEvents](https://github.com/pwittchen/NetworkEvents) library. Its functionality was extracted from [ReactiveNetwork](https://github.com/pwittchen/ReactiveNetwork) project to make it more specialized and reduce number of required permissions required to perform specific task.

If you are searching library for observing network or Internet connectivity check [ReactiveNetwork](https://github.com/pwittchen/ReactiveNetwork) project.

JavaDoc is available at: http://pwittchen.github.io/ReactiveWiFi/RxJava2.x

Contents
--------

- [Usage](#usage)
  - [Observing WiFi Access Points](#observing-wifi-access-points)
  - [Observing WiFi signal level](#observing-wifi-signal-level)
  - [Observing WiFi information changes](#observing-wifi-information-changes)
  - [Observing WPA Supplicant state changes](#observing-wpa-supplicant-state-changes)
  - [Observing WiFi State changes](#observing-wifi-state-changes)
- [Examples](#examples)
- [Download](#download)
- [Code style](#code-style)
- [Static code analysis](#static-code-analysis)
- [License](#license)

Usage
-----

Library has the following RxJava Observables available in the public API:

```java
Observable<List<ScanResult>> observeWifiAccessPoints(final Context context)
Observable<Integer> observeWifiSignalLevel(final Context context, final int numLevels)
Observable<WifiSignalLevel> observeWifiSignalLevel(final Context context)
Observable<SupplicantState> observeSupplicantState(final Context context)
Observable<WifiInfo> observeWifiAccessPointChanges(final Context context)
Observable<WifiState> observeWifiStateChange(final Context context)
```

**Please note**: Due to memory leak in `WifiManager` reported
in [issue 43945](https://code.google.com/p/android/issues/detail?id=43945) in Android issue tracker
it's recommended to use Application Context instead of Activity Context.

### Observing WiFi Access Points

**Please note**: If you want to observe WiFi access points on Android M (6.0) or higher, you need to [request runtime permission](https://developer.android.com/training/permissions/requesting.html) for `ACCESS_COARSE_LOCATION` or `ACCESS_FINE_LOCATION`. After that, location services have to be enabled. See sample app in `app` directory to check how it's done. User needs to enable Location manually. You can suggest him or her to do it via `AccessRequester` class from this library as follows:

```java
if (!AccessRequester.isLocationEnabled(this)) {
  AccessRequester.requestLocationAccess(this);
} else {
  // observe WiFi Access Points
}
```

If you need more customization (e.g. custom title and message of the dialog window or custom listener), check public API of the `AccessRequester` class.

We can observe WiFi Access Points with `observeWifiAccessPoints(context)` method. Subscriber will be called everytime, when strength of the WiFi Access Points signal changes (it usually happens when user is moving around with a mobile device). We can do it in the following way:

```java
ReactiveWifi.observeWifiAccessPoints(context)
    .subscribeOn(Schedulers.io())
    ... // anything else what you can do with RxJava
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(scanResults -> {
      // do something with ScanResults
    });
```

**Hint**: If you want to operate on a single `ScanResult` instead of `List<ScanResult>` in a `subscribe(...)` method, consider using `flatMap(...)` and `Observable.from(...)` operators from RxJava for transforming the stream.

### Observing WiFi signal level

We can observe WiFi signal level with `observeWifiSignalLevel(context, numLevels)` method. Subscriber will be called everytime, when signal level of the connected WiFi  changes (it usually happens when user is moving around with a mobile device). We can do it in the following way:

```java
ReactiveWifi.observeWifiSignalLevel(context, numLevels)
    .subscribeOn(Schedulers.io())
    ... // anything else what you can do with RxJava
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(level -> {
      // do something with level
    });
```

We can also observe WiFi signal level with `observeWifiSignalLevel(final Context context)` method, which has predefined num levels value, which is equal to 4 and returns `Observable<WifiSignalLevel>`. `WifiSignalLevel` is an enum, which contains information about current signal level. We can do it as follows:

```java
ReactiveWifi.observeWifiSignalLevel(context)
    .subscribeOn(Schedulers.io())
    ... // anything else what you can do with RxJava
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(level -> {
      // do something with level
    });
```

`WifiSignalLevel` has the following values:

```java
public enum WifiSignalLevel {
  NO_SIGNAL(0, "no signal"),
  POOR(1, "poor"),
  FAIR(2, "fair"),
  GOOD(3, "good"),
  EXCELLENT(4, "excellent");
  ...
}
```

### Observing WiFi information changes

We can observe WiFi network information changes with `observeWifiAccessPointChanges(context)` method. Subscriber will be called every time the WiFi network the device is connected to has changed. We can do it in the following way:

```java
ReactiveWifi.observeWifiAccessPointChanges(context)
    .subscribeOn(Schedulers.io())
    ... // anything else what you can do with RxJava
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(wifiInfo -> {
      // do something with wifiInfo
    });;
```

### Observing WPA Supplicant state changes

We can observe changes in the WPA Supplicant state with `observeSupplicantState(context)` method. Subscriber will be called every time the WPA Supplicant will change its state, getting information at a lower level than usually available. We can do it in the following way:

```java
ReactiveWifi.observeSupplicantState(context)
    .subscribeOn(Schedulers.io())
    ... // anything else what you can do with RxJava
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(state -> {
      // do something with state
    });
```

### Observing WiFi state changes

We can observe wifi state change with `observeWifiStateChange(context)` method. Subscriber will be called every time whenever the wifi state change such like enabling,disabling,enabled and disabled. We can do it in the following way:

```java
ReactiveWifi.observeWifiStateChange(context)
    .subscribeOn(Schedulers.io())
    ... // anything else what you can do with RxJava
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(state -> {
      // do something with level
    });
```
Examples
--------

Exemplary application is located in `app` directory of this repository.

If you want to use this library with Kotlin, check `app-kotlin` directory.

Download
--------

```xml
<dependency>
    <groupId>com.github.pwittchen</groupId>
    <artifactId>reactivewifi-rx2</artifactId>
    <version>0.3.0</version>
</dependency>
```

or through Gradle:

```groovy
dependencies {
  implementation 'com.github.pwittchen:reactivewifi-rx2:0.3.0'
}
```

Code style
----------

Code style used in the project is called `SquareAndroid` from Java Code Styles repository by Square available at: https://github.com/square/java-code-styles.

Static code analysis
--------------------

Static code analysis runs Checkstyle, FindBugs, PMD and Lint. It can be executed with command:

 ```
 ./gradlew check
 ```

Reports from analysis are generated in `library/build/reports/` directory.

License
-------

    Copyright 2016 Piotr Wittchen

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
