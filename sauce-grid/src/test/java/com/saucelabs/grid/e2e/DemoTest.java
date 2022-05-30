package com.saucelabs.grid.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

public class DemoTest {
  @Test
  public void demoTest() throws MalformedURLException {
    MutableCapabilities sauceOptions = new MutableCapabilities();
    // Depending on where your Sauce Labs account is created, use 'EU' or 'US'
    sauceOptions.setCapability("dataCenter", "US");
    sauceOptions.setCapability("timeZone", "US/Pacific");
    sauceOptions.setCapability("screenResolution", "1920x1080");
    sauceOptions.setCapability("username", System.getenv("SAUCE_USERNAME"));
    sauceOptions.setCapability("accessKey", System.getenv("SAUCE_ACCESS_KEY"));
    sauceOptions.setCapability("name", "demoTest");

    URL gridUrl = new URL("http://localhost:4444");
    FirefoxOptions firefoxOptions = new FirefoxOptions();
    firefoxOptions.setCapability("platformName", "linux");
    firefoxOptions.setCapability("browserVersion", "100.0");
    firefoxOptions.setCapability("sauce:options", sauceOptions);
    RemoteWebDriver driver = new RemoteWebDriver(gridUrl, firefoxOptions);
    driver.manage().window().maximize();

    try {
      // Log in to www.saucedemo.com
      driver.get("https://www.saucedemo.com");
      driver.findElement(By.id("user-name")).sendKeys("standard_user");
      driver.findElement(By.id("password")).sendKeys("secret_sauce");
      driver.findElement(By.className("btn_action")).click();

      // Add two items to the shopping cart
      driver.get("https://www.saucedemo.com/inventory.html");
      driver.findElement(By.className("btn_primary")).click();
      driver.findElement(By.className("btn_primary")).click();
      assertEquals("2", driver.findElement(By.className("shopping_cart_badge")).getText());

      // Assert we have two items in the shopping cart
      driver.get("https://www.saucedemo.com/cart.html");
      assertEquals(2, driver.findElements(By.className("inventory_item_name")).size());
    } finally {
      driver.quit();
    }
  }
}
