## How to run Sauce To Go

1. Create a configuration file. Place it on a directory that Docker can access.

```toml
[docker]
# Configs have a mapping between the Docker image to use and the capabilities that need to be matched to
# start a container with the given image.
configs = [
    "saucelabs/stg-firefox:88.0", '{"browserName": "firefox", "platformName": "linux"}',
    "saucelabs/stg-edge:91.0", '{"browserName": "MicrosoftEdge", "platformName": "linux"}',
    "saucelabs/stg-chrome:90.0", '{"browserName": "chrome", "platformName": "linux"}'
]

# URL for connecting to the docker daemon
# host.docker.internal works for macOS and Windows.
# Linux could use --net=host in the `docker run` instruction or 172.17.0.1 in the URI below.
# To have Docker listening through tcp on macOS, install socat and run the following command
# socat -4 TCP-LISTEN:2375,fork UNIX-CONNECT:/var/run/docker.sock
url = "http://host.docker.internal:2375"
# Docker imagee used for video recording
video-image = "saucelabs/stg-video:ffmpeg-4.3.1-20210513"
# Docker imagee used to upload the generated test assets
assets-uploader-image = "saucelabs/stg-assets-uploader:20210515"

[node]
implementation = "com.saucelabs.grid.SauceNodeFactory"
```

2. Run Sauce To Go

You'll need to mount two volumes. The first one is the absolute path where the config file from
step 4 is, and the second one is an absolute path where you'd like the test assets to be stored. 

```shell script
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
    "username": "Your Sauce user name",
    "accessKey": "Your Sauce access key"
  }
}
```

The values for `username` and `accessKey` are mandatory.

You can see some sample tests [here](sauce-grid/src/test/java/com/saucelabs/grid/e2e/SampleTests.java).
