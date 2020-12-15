package com.saucelabs.grid.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SampleTests {

  private static final Logger LOG = Logger.getLogger(SampleTests.class.getName());
  private static final String SAUCE_OPTIONS_CAPS = "sauce:options";

  static Stream<Arguments> browsersAndPlatforms() {
    return Stream.of(
      arguments("chrome", "linux"),
      arguments("firefox", "linux")
    );
  }
  public RemoteWebDriver createDriver(String testName, String browserName, String platformName)
    throws MalformedURLException {
    LOG.info("Running " + testName);
    URL gridUrl = new URL("http://localhost:4444");
    MutableCapabilities capabilities;
    if (BrowserType.CHROME.equalsIgnoreCase(browserName)) {
      capabilities = new ChromeOptions();
    } else {
      capabilities = new FirefoxOptions();
    }
    capabilities.setCapability(SAUCE_OPTIONS_CAPS, getSauceOptions(testName));
    capabilities.setCapability(CapabilityType.PLATFORM_NAME, platformName);
    RemoteWebDriver remoteWebDriver = new RemoteWebDriver(gridUrl, capabilities);
    remoteWebDriver.manage().window().maximize();
    return remoteWebDriver;
  }

  @ParameterizedTest(name = "{index} ==> Browser: {0}, Platform: {1}")
  @MethodSource("browsersAndPlatforms")
  public void addOneItemToCart(String browserName, String platformName, TestInfo testInfo)
    throws MalformedURLException {
    RemoteWebDriver driver = createDriver(getTestName(testInfo), browserName, platformName);
    try {
      driver.get("https://www.saucedemo.com/inventory.html");
      driver.findElement(By.className("btn_primary")).click();
      assertEquals("1", driver.findElement(By.className("shopping_cart_badge")).getText());

      driver.get("http://www.saucedemo.com/cart.html");
      assertEquals(1, driver.findElements(By.className("inventory_item_name")).size());
    } finally {
      driver.quit();
    }
  }

  @ParameterizedTest(name = "{index} ==> Browser: {0}, Platform: {1}")
  @MethodSource("browsersAndPlatforms")
  public void addTwoItemsToCart(String browserName, String platformName, TestInfo testInfo)
    throws MalformedURLException {
    RemoteWebDriver driver = createDriver(getTestName(testInfo), browserName, platformName);
    try {
      driver.get("https://www.saucedemo.com/inventory.html");
      driver.findElement(By.className("btn_primary")).click();
      driver.findElement(By.className("btn_primary")).click();
      assertEquals("2", driver.findElement(By.className("shopping_cart_badge")).getText());

      driver.get("http://www.saucedemo.com/cart.html");
      assertEquals(2, driver.findElements(By.className("inventory_item_name")).size());
    } finally {
      driver.quit();
    }
  }

  @ParameterizedTest(name = "{index} ==> Browser: {0}, Platform: {1}")
  @MethodSource("browsersAndPlatforms")
  public void validCredentials(String browserName, String platformName, TestInfo testInfo)
    throws MalformedURLException {
    RemoteWebDriver driver = createDriver(getTestName(testInfo), browserName, platformName);
    try {
      driver.get("https://www.saucedemo.com");
      driver.findElement(By.id("user-name")).sendKeys("standard_user");
      driver.findElement(By.id("password")).sendKeys("secret_sauce");
      driver.findElement(By.className("btn_action")).click();

      assertTrue(driver.getCurrentUrl().contains("inventory"));
    } finally {
      driver.quit();
    }
  }

  @ParameterizedTest(name = "{index} ==> Browser: {0}, Platform: {1}")
  @MethodSource("browsersAndPlatforms")
  public void invalidCredentials(String browserName, String platformName, TestInfo testInfo)
    throws MalformedURLException {
    RemoteWebDriver driver = createDriver(getTestName(testInfo), browserName, platformName);
    try {
      driver.get("https://www.saucedemo.com");
      driver.findElement(By.id("user-name")).sendKeys("bad");
      driver.findElement(By.id("password")).sendKeys("bad");
      driver.findElement(By.className("btn_action")).click();

      assertTrue(driver.findElements(By.className("error-button")).size() > 0);
    } finally {
      driver.quit();
    }
  }

  @ParameterizedTest(name = "{index} ==> Browser: {0}, Platform: {1}")
  @MethodSource("browsersAndPlatforms")
  public void longScript(String browserName, String platformName, TestInfo testInfo)
    throws MalformedURLException {
    RemoteWebDriver driver = createDriver(getTestName(testInfo), browserName, platformName);
    try {
      driver.get("https://time.is/");
      driver.get("https://www.amazon.de/");
      driver.get("https://www.airbnb.com/");
      driver.get("https://www.saucelabs.com/");
      driver.get("https://opensource.saucelabs.com/");
    } finally {
      driver.quit();
    }
  }

  private String getTestName(TestInfo testInfo) {
    if (testInfo.getTestMethod().isPresent()) {
      return testInfo.getTestMethod().get().getName() + " - " + testInfo.getDisplayName();
    }
    return testInfo.getDisplayName();
  }

  private MutableCapabilities getSauceOptions(String testName) {
    MutableCapabilities capabilities = new MutableCapabilities();
    capabilities.setCapability("dataCenter", "EU");
    capabilities.setCapability("timeZone", "US/Pacific");
    capabilities.setCapability("screenResolution", "1920x1080");
    capabilities.setCapability("username", System.getenv("SAUCE_USERNAME"));
    capabilities.setCapability("accessKey", System.getenv("SAUCE_ACCESS_KEY"));
    capabilities.setCapability("name", testName);
    return capabilities;
  }
}
