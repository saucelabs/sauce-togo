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
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.time.Instant;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SampleTests {

  private static final Logger LOG = Logger.getLogger(SampleTests.class.getName());
  private static final String SAUCE_OPTIONS_CAPS = "sauce:options";
  private static final String SAUCE_BUILD_ID = Instant.now().toString();
  private static final String SAUCE_TO_GO_URL = "http://localhost:4444";

  static Stream<Arguments> browsersAndPlatforms() {
    return Stream.of(
      arguments(BrowserType.CHROME, Platform.LINUX),
      arguments(BrowserType.EDGE, Platform.LINUX),
      arguments(BrowserType.FIREFOX, Platform.LINUX)
    );
  }

  public RemoteWebDriver createDriver(String testName, String browserName, Platform platformName)
    throws Exception {
    LOG.info("Running " + testName);
    URL gridUrl = new URL(SAUCE_TO_GO_URL);
    MutableCapabilities capabilities;
    switch (browserName) {
      case BrowserType.CHROME:
        capabilities = new ChromeOptions();
        break;
      case BrowserType.FIREFOX:
        capabilities = new FirefoxOptions();
        break;
      case BrowserType.EDGE:
        capabilities = new EdgeOptions();
        break;
      default:
        throw new Exception("Browser not configured! " + browserName);
    }
    capabilities.setCapability(SAUCE_OPTIONS_CAPS, getSauceOptions(testName));
    capabilities.setCapability(CapabilityType.PLATFORM_NAME, platformName);
    RemoteWebDriver remoteWebDriver = new RemoteWebDriver(gridUrl, capabilities);
    remoteWebDriver.manage().window().maximize();
    return remoteWebDriver;
  }

  @ParameterizedTest(name = "{index} ==> Browser: {0}, Platform: {1}")
  @MethodSource("browsersAndPlatforms")
  public void addOneItemToCart(String browserName, Platform platformName, TestInfo testInfo)
    throws Exception {
    RemoteWebDriver driver = createDriver(getTestName(testInfo), browserName, platformName);
    try {
      loginToSauceDemo(driver, "standard_user", "secret_sauce");
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
  public void addTwoItemsToCart(String browserName, Platform platformName, TestInfo testInfo)
    throws Exception {
    RemoteWebDriver driver = createDriver(getTestName(testInfo), browserName, platformName);
    try {
      loginToSauceDemo(driver, "standard_user", "secret_sauce");
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
  public void validCredentials(String browserName, Platform platformName, TestInfo testInfo)
    throws Exception {
    RemoteWebDriver driver = createDriver(getTestName(testInfo), browserName, platformName);
    try {
      loginToSauceDemo(driver, "standard_user", "secret_sauce");
      assertTrue(driver.getCurrentUrl().contains("inventory"));
    } finally {
      driver.quit();
    }
  }

  @ParameterizedTest(name = "{index} ==> Browser: {0}, Platform: {1}")
  @MethodSource("browsersAndPlatforms")
  public void invalidCredentials(String browserName, Platform platformName, TestInfo testInfo)
    throws Exception {
    RemoteWebDriver driver = createDriver(getTestName(testInfo), browserName, platformName);
    try {
      loginToSauceDemo(driver, "bad", "bad");
      assertTrue(driver.findElements(By.className("error-button")).size() > 0);
    } finally {
      driver.quit();
    }
  }

  private void loginToSauceDemo(RemoteWebDriver driver, String bad, String bad2) {
    driver.get("https://www.saucedemo.com");
    driver.findElement(By.id("user-name")).sendKeys(bad);
    driver.findElement(By.id("password")).sendKeys(bad2);
    driver.findElement(By.className("btn_action")).click();
  }

  @ParameterizedTest(name = "{index} ==> Browser: {0}, Platform: {1}")
  @MethodSource("browsersAndPlatforms")
  public void longScript(String browserName, Platform platformName, TestInfo testInfo)
    throws Exception {
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
    capabilities.setCapability("dataCenter", "US");
    capabilities.setCapability("timeZone", "US/Pacific");
    capabilities.setCapability("screenResolution", "1920x1080");
    capabilities.setCapability("username", System.getenv("SAUCE_USERNAME"));
    capabilities.setCapability("accessKey", System.getenv("SAUCE_ACCESS_KEY"));
    capabilities.setCapability("name", testName);
    capabilities.setCapability("build", SAUCE_BUILD_ID);
    return capabilities;
  }
}
