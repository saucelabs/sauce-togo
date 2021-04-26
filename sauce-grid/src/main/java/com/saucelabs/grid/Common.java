package com.saucelabs.grid;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.json.Json;

import java.util.Map;
import java.util.Optional;

public class Common {

  static final String SAUCE_OPTIONS = "sauce:options";
  static final Json JSON = new Json();

  public static Optional<Object> getSauceCapability(Capabilities requestCapabilities,
                                                    String capabilityName) {
    Object rawSeleniumOptions = requestCapabilities.getCapability(SAUCE_OPTIONS);
    if (rawSeleniumOptions instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> seleniumOptions = (Map<String, Object>) rawSeleniumOptions;
      return Optional.ofNullable(seleniumOptions.get(capabilityName));
    }
    return Optional.empty();
  }

}
