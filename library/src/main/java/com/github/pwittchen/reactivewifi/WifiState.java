package com.github.pwittchen.reactivewifi;

import android.net.wifi.WifiManager;

public enum WifiState {
  DISABLING(WifiManager.WIFI_STATE_DISABLING, "disabling"),
  DISABLED(WifiManager.WIFI_STATE_DISABLED, "disabled"),
  ENABLING(WifiManager.WIFI_STATE_ENABLING, "enabling"),
  ENABLED(WifiManager.WIFI_STATE_ENABLED, "enabled"),
  STATE_UNKNOWN(WifiManager.WIFI_STATE_UNKNOWN, "unknown");

  public final int state;
  public final String description;

  WifiState(final int state, String description) {
    this.state = state;
    this.description = description;
  }

  /**
   * Gets WifiState enum basing on integer value
   *
   * @param state as an integer
   * @return WifiState enum
   */
  public static WifiState fromState(final int state) {
    switch (state) {
      case WifiManager.WIFI_STATE_DISABLING:
        return DISABLING;
      case WifiManager.WIFI_STATE_DISABLED:
        return DISABLED;
      case WifiManager.WIFI_STATE_ENABLING:
        return ENABLING;
      case WifiManager.WIFI_STATE_ENABLED:
        return ENABLED;
      case WifiManager.WIFI_STATE_UNKNOWN:
        return STATE_UNKNOWN;
      default:
        return STATE_UNKNOWN;
    }
  }

  @Override
  public String toString() {
    return "WifiState{" +
        "state=" + state +
        ", description='" + description + '\'' +
        '}';
  }
}
