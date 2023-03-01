---
id: getting-started
title: Getting Started
sidebar_label: Getting Started
---

## Prerequisites

Please make sure you have the following:

- [Docker installed](https://docs.docker.com/engine/install/)
- `docker run hello-world` runs without any errors
- An active Sauce Labs account, if you don't have one yet, please [sign-up](https://saucelabs.com/sign-up?utm_source=referral&utm_medium=ospo&utm_campaign=saucetogo&utm_term=)
- Recommended - Your Sauce Labs `SAUCE_USERNAME` and `SAUCE_ACCESS_KEY` as environment variables

```bash
SAUCE_USERNAME='valid.username'
SAUCE_ACCESS_KEY='valid.key'
```

Here are instructions for setting environment variables on each Operating System:
* [Windows 10](https://www.architectryan.com/2018/08/31/how-to-change-environment-variables-on-windows-10/)
* [MacOS](https://apple.stackexchange.com/questions/106778/how-do-i-set-environment-variables-on-os-x)
* [Linux](https://askubuntu.com/questions/58814/how-do-i-add-environment-variables)

## Get Sauce To Go up and running

### Two-minute demo
<video style={{maxWidth: '100%'}} controls>
  <source src="https://user-images.githubusercontent.com/5992658/135048022-58e73843-69d7-4f04-8e9e-ae8f6a83c89d.mp4" type="video/mp4"/>
  Your browser does not support the video tag.
</video>

### 1. Create a directory and copy the configuration example

Save the file as `config.toml`

Check the comments in the configuration example for specific adjustments on each operating system.

```toml
[docker]
# Configs have a mapping between a Docker image and the capabilities that need to be matched to
# start a container with the given image.
configs = [
    "saucelabs/stg-firefox:102.0", '{"browserName": "firefox", "browserVersion": "102.0", "platformName": "linux"}',
    "saucelabs/stg-edge:103.0", '{"browserName": "MicrosoftEdge", "browserVersion": "103.0", "platformName": "linux"}',
    "saucelabs/stg-chrome:103.0", '{"browserName": "chrome", "browserVersion": "103.0", "platformName": "linux"}'
]

# URL for connecting to the docker daemon
# Linux: 172.17.0.1 (make sure the Docker deamon is listening to this url first)
# Docker Desktop on macOS and Windows: host.docker.internal
# To have Docker listening through tcp on macOS or Linux, install socat and run the following command
# socat -4 TCP-LISTEN:2375,fork UNIX-CONNECT:/var/run/docker.sock
url = "http://host.docker.internal:2375"
# Docker image used for video recording
video-image = "saucelabs/stg-video:20220629"
# Docker image used to upload test assets
assets-uploader-image = "saucelabs/stg-assets-uploader:20220629"

[node]
implementation = "com.saucelabs.grid.SauceNodeFactory"
```

_Make sure the directory path can be accessed by Docker._



**Tip:** To improve loading time, pull the Docker images before moving to step two
(only needed once).

```bash
docker pull saucelabs/stg-firefox:102.0
docker pull saucelabs/stg-edge:103.0
docker pull saucelabs/stg-chrome:103.0
docker pull saucelabs/stg-video:20220629
docker pull saucelabs/stg-assets-uploader:20220629
```

### 2. Start Sauce To Go

You'll need to mount two volumes. The first one is the path where the config file from
step 1 is, and the second one is the path where the test assets will be temporarily stored.
In Linux, you might need to change the permissions of the assets folder using the command
`sudo chown 1200:1201 assets`.

_Be sure to be in the same directory you created on step 1._

```bash
docker run --rm --name sauce-togo -p 4444:4444 \
    -v ${PWD}/config.toml:/opt/bin/config.toml \
    -v ${PWD}/assets:/opt/selenium/assets \
    saucelabs/stg-standalone:20220629
```

### 3. Run your tests

Point them to either `http://localhost:4444` or to `http://localhost:4444/wd/hub`.

Your test capabilities need to include the `sauce:options` section, check the example below.

<details>
    <summary>Click to see the test example</summary>

```java
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    firefoxOptions.setCapability("browserVersion", "102.0");
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
```
</details>


### 4. Head to [Sauce Labs](https://app.saucelabs.com/) and check your tests!
