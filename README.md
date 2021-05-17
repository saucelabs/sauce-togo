# Sauce To Go

Run tests on your infrastructure and see the results (video/screenshots/logs) in [Sauce Labs](https://saucelabs.com/).

_You'll need an active Sauce Labs account to use Sauce To Go, if you don't have one yet, please
[sign-up](https://saucelabs.com/sign-up)._

## How to run Sauce To Go

1. Create a directory on a path Docker can access and copy the following configuration example. 
Save the file as `config.toml`.

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

2. Run Sauce To Go

You'll need to mount two volumes. The first one is the absolute path where the config file from
step 1 is, and the second one is an absolute path where the test assets will be temporarily stored. 

```sh
docker run --rm -ti --name sauce-togo -p 4444:4444 \
    -v /absolute/path/to/your/sauce/togo/config.toml:/opt/bin/config.toml \
    -v /absolute/path/to/your/assets/directory:/opt/selenium/assets \
    saucelabs/stg-standalone:20210515
```

3. Run your tests and point them to `http://localhost:4444` or `http://localhost:4444/wd/hub`

Your test capabilities need to include the `sauce:options` section, here is an example: 

```json
{
  "browserName": "firefox",
  "platformName": "linux",
  "sauce:options": {
    "timeZone": "US/Pacific",
    "screenResolution": "1920x1080",
    "dataCenter": "US",
    "name": "Your test name",
    "username": "<SAUCE_USERNAME>",
    "accessKey": "<SAUCE_ACCESS_KEY>"
  }
}
```

The values for `username` and `accessKey` are mandatory.
