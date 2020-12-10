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

4. Use the generated jar in the Docker images

```shell script
# Move the jar to the docker directory
mv sauce-grid/target/sauce-grid-1.0-SNAPSHOT-jar-with-dependencies.jar docker/selenium-server.jar
# Go to the docker directory
cd docker
# Build the standalone Docker image
make standalone_docker
# Build the video images
make video_latest video
```

5. Run it in standalone mode

- Put uploader in a docker image
