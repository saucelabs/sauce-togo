package com.saucelabs.grid.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

// This code is using Selenium 4 and JUnit 5, it works with Selenium 3 with minor adjustments.
public class DemoTest {
  @Test
  public void demoTest() throws MalformedURLException {
    MutableCapabilities sauceOptions = new MutableCapabilities();
    // Depending where your Sauce Labs account is created, use 'EU' or 'US'
    sauceOptions.setCapability("dataCenter", "US");
    sauceOptions.setCapability("timeZone", "US/Pacific");
    sauceOptions.setCapability("screenResolution", "1920x1080");
    sauceOptions.setCapability("username", System.getenv("SAUCE_USERNAME"));
    sauceOptions.setCapability("accessKey", System.getenv("SAUCE_ACCESS_KEY"));
    sauceOptions.setCapability("name", "standaloneTest");

    URL gridUrl = new URL("http://localhost:4444");
    FirefoxOptions firefoxOptions = new FirefoxOptions();
    firefoxOptions.setCapability("platformName", "linux");
    firefoxOptions.setCapability("sauce:options", sauceOptions);
    RemoteWebDriver webDriver = new RemoteWebDriver(gridUrl, firefoxOptions);

    try {
      // Quick search for the 'webdriver' keyword
      webDriver.get("http://www.google.com/ncr");
      webDriver.findElement(By.name("q")).sendKeys("webdriver", Keys.RETURN);
      WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
      webDriverWait.until(ExpectedConditions.titleContains("webdriver"));
    } finally {
      webDriver.quit();
    }
  }
}
