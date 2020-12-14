## How to build & run Sauce-ToGo
> This project is still a PoC, some build steps are manual.

Run these commands on the root directory of the project.

1. Download the prerelease build of the Selenium Server
```shell script
mkdir -p server-jar
wget https://github.com/SeleniumHQ/docker-selenium/raw/beta-jars/selenium-server-4.0.0-prerelease-beta-1-1f4909f59c.jar -O ${PWD}/server-jar/selenium-server.jar
``` 

2. We will use a Docker container to build Sauce-ToGo. First, install the dependencies locally. 

```shell script
# Pull and run the image
docker run --rm -ti \
  -v ${PWD}/server-jar:/usr/src/mymaven \
  -v ${PWD}/.m2:/root/.m2 \
  maven:3.6.3-jdk-8 bash
# Move to the mapped directory
cd /usr/src/mymaven
# Install the prerelease jar to the maven local repo (run this inside the container)
# Retry the command if the dependencies cannot be downloaded.
mvn install:install-file \
-Dfile=selenium-server.jar \
-DgroupId=org.seleniumhq.selenium \
-DartifactId=selenium-grid \
-Dversion=4.0.0-beta-1 \
-Dpackaging=jar \
-DgeneratePom=true
# Exit the container
exit
``` 

3. Build Sauce-ToGo

```shell script
# Pull and run the image
docker run --rm -ti \
  -v ${PWD}:/usr/src/mymaven \
  -v ${PWD}/.m2:/root/.m2 \
  maven:3.6.3-jdk-8 bash
# Move to the mapped directory
cd /usr/src/mymaven
# Build Sauce-ToGo (Grid & Uploader components)
mvn clean package
# Exit the container
exit
```

4. Move the generated jars to the `docker` directory, we'll use them to build the Docker images

```shell script
# Move the jar to the docker directory
mv sauce-grid/target/sauce-grid-1.0-SNAPSHOT-jar-with-dependencies.jar docker/selenium-server.jar
# Move the jar to the docker directory
mv sauce-assets-uploader/target/sauce-assets-uploader-1.0-SNAPSHOT-jar-with-dependencies.jar docker/sauce-assets-uploader-1.0-SNAPSHOT-jar-with-dependencies.jar 
```

5. Build all the Docker images

```shell script
# Go to the docker directory
cd docker
# Build all the Docker images
make all
```

6. Add a configuration file. Place it on a directory that Docker can access.

```toml
[docker]
# Configs have a mapping between the Docker image to use and the capabilities that need to be matched to
# start a container with the given image.
configs = [
    "saucelabs/standalone-firefox:4.0.0-beta-1-prerelease-20201208", "{\"browserName\": \"firefox\"}",
    "saucelabs/standalone-chrome:4.0.0-beta-1-prerelease-20201208", "{\"browserName\": \"chrome\"}"
]

# URL for connecting to the docker daemon
# host.docker.internal works for macOS and Windows.
# Linux could use --net=host in the `docker run` instruction or 172.17.0.1 in the URI below.
# To have Docker listening through tcp on macOS, install socat and run the following command
# socat -4 TCP-LISTEN:2375,fork UNIX-CONNECT:/var/run/docker.sock
host = "tcp://host.docker.internal:2375"
# Docker imagee used for video recording
video-image = "saucelabs/video:ffmpeg-4.3.1-20201208"
# Docker imagee used to upload the generated test assets
assets-uploader-image = "saucelabs/assets-uploader:20201208"

[node]
implementation = "com.saucelabs.grid.SauceNodeFactory"
```

7. Start Sauce-ToGo

You'll need to mount two volumes. The first one is the absolute path where the config file from
step 6 is, and the second one is an absolute path where you'd like the test assets to be stored. 

```shell script
docker run --rm -ti --name sauce-togo -p 4444:4444 \
    -v /absolute/path/to/your/sauce/togo/config.toml:/opt/bin/config.toml \
    -v /absolute/path/to/your/assets/directory:/opt/selenium/assets \
    saucelabs/standalone-docker:4.0.0-beta-1-prerelease-20201208
```


