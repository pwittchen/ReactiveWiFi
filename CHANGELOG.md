CHANGELOG
=========

v. 0.3.0
--------
*06 Jan 2018*

- fixed bug: `Error receiving broadcast Intent...` #28 
- migrated library to RxJava2.x as a separate artifact
- bumped Gradle to v. 3.0
- updated project dependencies
- added Retrolambda to sample java app

v. 0.2.0
--------
*11 Feb 2017*

- added `WifiState` enum
- added `Observable<WifiState> observeWifiStateChange(context)` to `ReactiveWifi` class
- updated Gradle and Travis CI config
- updated Gradle Wrapper version
- bumped target SDK version to 25 and build tools version to 25.0.1
- bumped RxJava to v. 1.2.6
- made methods, which create Observables in `ReactiveWifi` class static
- made the constructor of ReactiveWifi class private
- added permission annotations
- added `AccessRequester` class responsible for checking if Location Services are enabled and redirecting the user to Location Settings

v. 0.1.1
--------
*31 Jul 2016*

- bumped RxJava to v. 1.1.8
- bumped RxAndroid to v. 1.2.1
- bumped Gradle Build Tools to 2.1.2

v. 0.1.0
--------
*18 Jun 2016*

- added ` Observable<SupplicantState> observeSupplicantState(context)` method, which observes the current WPA supplicant state.
- added `Observable<WifiInfo> observeWifiAccessPointChanges(context)` method, which observes the WiFi network the device is connected to.

v. 0.0.1
--------
*06 Jun 2016*

First release of the library
