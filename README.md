# ReactiveWiFi (WIP)

**This project is not ready yet**.

Android library listening available WiFi Access Points and change of the WiFi signal strength with RxJava Observables.

Its functionality was extracted from [ReactiveNetwork](https://github.com/pwittchen/ReactiveNetwork) project.

Library is compatible with RxJava 1.+ and RxAndroid 1.+ and uses them under the hood.

Contents
--------

- [Usage](#usage)
  - [Observing WiFi Access Points](#observing-wifi-access-points)
  - [Observing WiFi signal level](#observing-wifi-signal-level)
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
```

### Observing WiFi Access Points

We can observe WiFi Access Points with `observeWifiAccessPoints(context)` method. Subscriber will be called everytime, when strength of the WiFi Access Points signal changes (it usually happens when user is moving around with a mobile device). We can do it in the following way:

```java
new ReactiveWifi().observeWifiAccessPoints(context)
    .subscribeOn(Schedulers.io())
    ... // anything else what you can do with RxJava
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Action1<List<ScanResult>>() {
      @Override public void call(List<ScanResult> scanResults) {
        // do something with scanResults
      }
    });
```

**Hint**: If you want to operate on a single `ScanResult` instead of `List<ScanResult>` in a `subscribe(...)` method, consider using `flatMap(...)` and `Observable.from(...)` operators from RxJava for transforming the stream.

### Observing WiFi signal level

We can observe WiFi signal level with `observeWifiSignalLevel(context, numLevels)` method. Subscriber will be called everytime, when signal level of the connected WiFi  changes (it usually happens when user is moving around with a mobile device). We can do it in the following way:

```java
new ReactiveWifi().observeWifiSignalLevel(context, numLevels)
    .subscribeOn(Schedulers.io())
    ... // anything else what you can do with RxJava
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Action1<Integer>() {
      @Override public void call(Integer level) {
        // do something with level
      }
    });
```

We can also observe WiFi signal level with `observeWifiSignalLevel(final Context context)` method, which has predefined num levels value, which is equal to 4 and returns `Observable<WifiSignalLevel>`. `WifiSignalLevel` is an enum, which contains information about current signal level. We can do it as follows:

```java
new ReactiveWifi().observeWifiSignalLevel(context)
    .subscribeOn(Schedulers.io())
    ... // anything else what you can do with RxJava
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Action1<WifiSignalLevel>() {
      @Override public void call(WifiSignalLevel signalLevel) {
        // do something with signalLevel
      }
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

Examples
--------

Exemplary application is located in `app` directory of this repository.

Download
--------

TBD.

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
