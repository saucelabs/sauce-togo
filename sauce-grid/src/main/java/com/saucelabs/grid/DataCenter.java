package com.saucelabs.grid;

import java.util.stream.Stream;

@SuppressWarnings("unused")
public enum DataCenter {
  EU(
    "https://ondemand.eu-central-1.saucelabs.com",
    "api.eu-central-1.saucelabs.com",
    "https://app.eu-central-1.saucelabs.com"),
  US_WEST(
    "https://ondemand.us-west-1.saucelabs.com",
    "api.us-west-1.saucelabs.com",
    "https://app.saucelabs.com"),
  US_EAST(
    "https://ondemand.us-east-1.saucelabs.com",
    "api.us-east-1.saucelabs.com",
    "https://app.us-east-1.saucelabs.com"),
  US(US_WEST),
  STAGING(
    "https://ondemand.staging.saucelabs.net",
    "api.staging.saucelabs.net",
    "https://app.staging.saucelabs.net");
  public final String onDemandUrl;
  public final String apiUrl;
  public final String apiHost;
  public final String appUrl;

  DataCenter(String onDemandUrl, String apiHost, String appUrl) {
    this.onDemandUrl = onDemandUrl;
    this.apiHost = apiHost;
    this.apiUrl = "https://%s:%s@" + apiHost;
    this.appUrl = appUrl;
  }

  @SuppressWarnings("CopyConstructorMissesField")
  DataCenter(DataCenter dataCenter) {
    this(dataCenter.onDemandUrl, dataCenter.apiHost, dataCenter.appUrl);
  }

  public String onDemandUrl() {
    return onDemandUrl;
  }

  public String apiUrl() {
    return apiUrl;
  }

  public String getApiHost() {
    return apiHost;
  }

  public String appUrl() {
    return appUrl;
  }

  public static DataCenter fromString(String dataCenter) {
    return Stream
      .of(values())
      .filter(dc -> dc.name().equalsIgnoreCase(dataCenter))
      .findFirst()
      .orElse(US_WEST);
  }
}
