# Sauce To Go

Run tests on your infrastructure and see the results (video/screenshots/logs) in [Sauce Labs](https://saucelabs.com/).

_You'll need an active Sauce Labs account to use Sauce To Go, if you don't have one yet, please
[sign-up](https://saucelabs.com/sign-up)._

## How to run Sauce To Go

### 1. Create a directory on a path Docker can access and copy the following configuration example. 


https://user-images.githubusercontent.com/5992658/118480134-24d27a00-b712-11eb-90be-4ba59c1bd01f.mp4



Save the file as `config.toml`

Check the comments in the configuration example for specific adjustments on each operating system.

```toml
[docker]
# Configs have a mapping between a Docker image and the capabilities that need to be matched to
# start a container with the given image.
configs = [
    "saucelabs/stg-firefox:88.0", '{"browserName": "firefox", "platformName": "linux"}',
    "saucelabs/stg-edge:91.0", '{"browserName": "MicrosoftEdge", "platformName": "linux"}',
    "saucelabs/stg-chrome:90.0", '{"browserName": "chrome", "platformName": "linux"}'
]

# URL for connecting to the docker daemon
# Linux: 172.17.0.1 (make sure the Docker deamon is listening to this url first) 
# Docker Desktop on macOS and Windows: host.docker.internal
# To have Docker listening through tcp on macOS, install socat and run the following command
# socat -4 TCP-LISTEN:2375,fork UNIX-CONNECT:/var/run/docker.sock
url = "http://host.docker.internal:2375"
# Docker image used for video recording
video-image = "saucelabs/stg-video:ffmpeg-4.3.1-20210513"
# Docker image used to upload test assets
assets-uploader-image = "saucelabs/stg-assets-uploader:20210515"

[node]
implementation = "com.saucelabs.grid.SauceNodeFactory"
```

**Tip:** To improve loading time, pull the Docker images before moving to step two 
(only needed once).

```sh
docker pull saucelabs/stg-firefox:88.0
docker pull saucelabs/stg-edge:91.0
docker pull saucelabs/stg-chrome:90.0
docker pull saucelabs/stg-video:ffmpeg-4.3.1-20210513
docker pull saucelabs/stg-assets-uploader:20210515
```

### 2. Run Sauce To Go


https://user-images.githubusercontent.com/5992658/118480326-5ea38080-b712-11eb-8118-c27fe5e7cdcc.mp4



You'll need to mount two volumes. The first one is the path where the config file from
step 1 is, and the second one is the path where the test assets will be temporarily stored. 

```sh
docker run --rm --name sauce-togo -p 4444:4444 \
    -v /path/to/your/config.toml:/opt/bin/config.toml \
    -v /path/to/your/assets/directory:/opt/selenium/assets \
    saucelabs/stg-standalone:20210515
```

### 3. Run your tests and check them in [Sauce Labs](https://app.saucelabs.com/)

Point them to either `http://localhost:4444` or to `http://localhost:4444/wd/hub`.

Your test capabilities need to include the `sauce:options` section, here is an example: 

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
    // Depending where your Sauce Labs account is created, use 'EU' or 'US'
    sauceOptions.setCapability("dataCenter", "US");
    sauceOptions.setCapability("timeZone", "US/Pacific");
    sauceOptions.setCapability("screenResolution", "1920x1080");
    sauceOptions.setCapability("username", System.getenv("SAUCE_USERNAME"));
    sauceOptions.setCapability("accessKey", System.getenv("SAUCE_ACCESS_KEY"));
    sauceOptions.setCapability("name", "demoTest");

    URL gridUrl = new URL("http://localhost:4444");
    FirefoxOptions firefoxOptions = new FirefoxOptions();
    firefoxOptions.setCapability("platformName", "linux");
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
      driver.get("http://www.saucedemo.com/cart.html");
      assertEquals(2, driver.findElements(By.className("inventory_item_name")).size());
    } finally {
      driver.quit();
    }
  }
}
```

