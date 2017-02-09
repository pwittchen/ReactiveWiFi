package com.github.pwittchen.reactivewifi;

/**
 * Created by bhavdip on 2/9/17.
 */

public enum WifiState {
  WIFI_STATE_DISABLING(0, "disabling"),
  WIFI_STATE_DISABLED(1, "disabled"),
  WIFI_STATE_ENABLING(2, "enabling"),
  WIFI_STATE_ENABLED(3, "enabled"),
  WIFI_STATE_UNKNOWN(4, "unknown");

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
      case 0:
        return WIFI_STATE_DISABLING;
      case 1:
        return WIFI_STATE_DISABLED;
      case 2:
        return WIFI_STATE_ENABLING;
      case 3:
        return WIFI_STATE_ENABLED;
      case 4:
        return WIFI_STATE_UNKNOWN;
      default:
        return WIFI_STATE_UNKNOWN;
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
