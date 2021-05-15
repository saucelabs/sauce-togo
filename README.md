## How to build & run Sauce To Go
> This project is still a PoC, build instructions might be incomplete.

Run these commands on the root directory of the project.

1. We will use a Docker container to build Sauce To Go.

```shell script
docker run --rm -ti \
  -v ${PWD}:/usr/src/mymaven \
  -v ${PWD}/.m2:/root/.m2 \
  -w /usr/src/mymaven \
  maven:3.6.3-jdk-8 mvn clean package
```

2. Move the generated jars to the `docker` directory, we'll use them to build the Docker images

```shell script
# Move the jar with Grid extension to the docker directory 
mv sauce-grid/target/sauce-grid-0.1-SNAPSHOT-jar-with-dependencies.jar docker/selenium-server.jar
# Move the Uploader jar to the docker directory
mv sauce-assets-uploader/target/sauce-assets-uploader-0.1-SNAPSHOT-jar-with-dependencies.jar docker/sauce-assets-uploader-0.1-SNAPSHOT.jar 
```

3. Build all the Docker images

```shell script
# Go to the docker directory and build all the Docker images
cd docker && make all
```

4. Add a configuration file. Place it on a directory that Docker can access.

```toml
[docker]
# Configs have a mapping between the Docker image to use and the capabilities that need to be matched to
# start a container with the given image.
configs = [
    "saucelabs/standalone-firefox:4.0.0-beta-4-prerelease-20210513", "{\"browserName\": \"firefox\", \"platformName\": \"linux\"}",
    "saucelabs/standalone-edge:4.0.0-beta-4-prerelease-20210513", "{\"browserName\": \"MicrosoftEdge\", \"platformName\": \"linux\"}",
    "saucelabs/standalone-chrome:4.0.0-beta-4-prerelease-20210513", "{\"browserName\": \"chrome\", \"platformName\": \"linux\"}"
]

# URL for connecting to the docker daemon
# host.docker.internal works for macOS and Windows.
# Linux could use --net=host in the `docker run` instruction or 172.17.0.1 in the URI below.
# To have Docker listening through tcp on macOS, install socat and run the following command
# socat -4 TCP-LISTEN:2375,fork UNIX-CONNECT:/var/run/docker.sock
url = "http://host.docker.internal:2375"
# Docker imagee used for video recording
video-image = "saucelabs/video:ffmpeg-4.3.1-20210513"
# Docker imagee used to upload the generated test assets
assets-uploader-image = "saucelabs/assets-uploader:20210513"

[node]
implementation = "com.saucelabs.grid.SauceNodeFactory"
```

5. Run Sauce To Go

You'll need to mount two volumes. The first one is the absolute path where the config file from
step 4 is, and the second one is an absolute path where you'd like the test assets to be stored. 

```shell script
docker run --rm -ti --name sauce-togo -p 4444:4444 \
    -v /absolute/path/to/your/sauce/togo/config.toml:/opt/bin/config.toml \
    -v /absolute/path/to/your/assets/directory:/opt/selenium/assets \
    saucelabs/standalone-docker:4.0.0-beta-4-prerelease-20210513
```

6. Run your tests and point them to `http://localhost:4444` or `http://localhost:4444/wd/hub`

Your test capabilities need to include the `sauce:options` section, here is an example: 

```json
{
  "browserName": "firefox",
  "platformName": "linux",
  "sauce:options": {
    "timeZone": "US/Pacific",
    "screenResolution": "1920x1080",
    "dataCenter": "EU",
    "name": "Your test name",
    "username": "Your Sauce user name",
    "accessKey": "Your Sauce access key"
  }
}
```

The values for `username` and `accessKey` are mandatory.

You can see some sample tests [here](sauce-grid/src/test/java/com/saucelabs/grid/e2e/SampleTests.java).
