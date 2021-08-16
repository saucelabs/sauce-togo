---
id: contributing
title: Become a Contributor
sidebar_label: Contribute
---

**Thank you for your interest in Sauce To Go. Your contributions are highly welcome.**

Sauce Labs proudly supports open source technologies and encourages open source projects like 
Sauce To Go. If you would like to contribute there are several ways of doing so.

## Ways To Contribute

The project offers a variety of ways to contribute:

* submit code features
* improve documentation (the code for this website is 
[on GitHub](https://github.com/saucelabs/sauce-togo/tree/main/website/docs))
* create educational content (blog posts, tutorials, videos, etc.)
* spread the good word about Sauce to Go (e.g. via Twitter)
* [report bugs](https://github.com/saucelabs/sauce-togo/issues) if you discover them while using 
  Sauce To Go
* See something you'd like fixed? Have a good idea for how to improve something? 
[Create an issue](https://github.com/saucelabs/sauce-togo/issues/new) or add to an existing issue. 

## Contributing Docs

A great way to start working with any open source project is through improving documentation. 
To get started, follow these 
[instructions](https://github.com/saucelabs/sauce-togo/blob/main/website/README.md).

## Building and contributing to the code base:

- [Requirements](#requirements)
- [Set up project](#set-up-and-build-project)
- [Release Sauce To Go](#release-sauce-to-go)
- [Code contribution guidelines](#code-contribution-guidelines)

Below are a few guidelines we would like you to follow.
If you need help, please reach out to us by opening an issue.

### Requirements

This repository uses Java 8, [Maven](http://maven.apache.org/) and [Docker](https://www.docker.com/).

You only need Docker to build the project and Docker images. However, if the intention is to
develop, debug, and troubleshoot the code, it is preferred to install all requirements.

We recommend to use [IntelliJ Idea](https://www.jetbrains.com/idea/download) for development of this
project, and using the Community Edition is more than enough. Any other Java capable IDE should also
be sufficient, but probably we won't be able to support you with debugging instructions.

### Set up and build project

[Fork](https://docs.github.com/en/get-started/quickstart/fork-a-repo) the latest code to your 
account.

Clone the code onto your local computer

```bash
git clone git@github.com/<YOUR_GITHUB_USERNAME>/sauce-togo.git
cd sauce-togo
``` 


Run these commands on the root directory of the project.

1. Building all jars.

If you have all requirements installed:
```bash
mvn clean package
``` 

Only using Docker (in case you want to avoid installing and configuring Java):
```bash
docker run --rm \
  -v ${PWD}:/usr/src/mymaven \
  -v ${PWD}/.m2:/root/.m2 \
  -w /usr/src/mymaven \
  maven:3.6.3-jdk-8 mvn clean package
```

2. Move the generated jars to the `docker` directory, we'll use them to build the Docker images

```bash
# Move the jar with Grid extension to the docker directory 
mv sauce-grid/target/sauce-grid-<POM-VERSION>-jar-with-dependencies.jar docker/selenium-server.jar
# Move the Uploader jar to the docker directory
mv sauce-assets-uploader/target/sauce-assets-uploader-<POM-VERSION>-jar-with-dependencies.jar docker/sauce-assets-uploader.jar 
```

3. Build all the Docker images

```bash
# Go to the docker directory and build all the Docker images
cd docker && make all && cd ..
```

To double-check, you can run `docker images --filter=reference='saucelabs/*:*'` to see the
generated images. To run Sauce To Go, please refer to the [Overview](https://opensource.saucelabs.com/sauce-togo/). 
Be sure to use the same tags you used to generate the Docker images.

You run some sample [tests](https://github.com/saucelabs/sauce-togo/blob/main/sauce-grid/src/test/java/com/saucelabs/grid/e2e/SampleTests.java) 
to verify your changes.

#### Debugging and running Sauce Grid outside Docker

Please check the comments in the [Main](https://github.com/saucelabs/sauce-togo/blob/main/sauce-grid/src/main/java/com/saucelabs/grid/Main.java)
class.

### Release Sauce To Go

The [push-images.yml](https://github.com/saucelabs/sauce-togo/blob/main/.github/workflows/push-images.yml) 
automates most of the process. Releases can only happen from the `main` branch. To trigger a 
release, add the keyword `[push]` (including brackets) to your commit message on the `main` branch. 
Tagging and pushing the Docker images to Docker Hub happens automatically.

**Note:** When adding the `[push]` keyword to the commit message, make sure to not skip the build
by having keywords such as [`[skip ci]`](https://github.blog/changelog/2021-02-08-github-actions-skip-pull-request-and-push-workflows-with-skip-ci/)
as part of the commit message.

In order to update and release a new Sauce Connect image, do the following:

### Code contribution guidelines

This is an outline of what the workflow for code contributions looks like

- Check the list of open [issues](https://github.com/saucelabs/sauce-togo/issues). Either assign
  an existing issue to yourself, or create a new one if you would like work on and discuss your
  ideas and use cases.

It is always best to discuss your plans beforehand, to ensure that your contribution is in
line with our goals.

- Fork the repository on GitHub
- Create a topic branch from where you want to base your work. This is usually master.
- Open a new pull request, label it `work in progress` and outline what you will be contributing
- Make commits of logical units.
- Make sure to sign-off on your commits `git commit -s -m "adding X to change Y"`
- Write good commit messages (see below).
- Push your changes to a topic branch in your fork of the repository.
- As you push your changes, update the pull request with new information and tasks as you complete them
- Project maintainers might comment on your work as you progress
- When you are done, remove the `work in progess` label and ping the maintainers for a review
- Your pull request must receive a :thumbsup: from the repository maintainers.

Thanks for your contributions!

#### Commit messages
Your commit messages ideally can answer two questions: what changed and why. The subject line
should feature the “what”, and the body of the commit should describe the “why”.

When creating a pull request, its description should reference the corresponding issue id.

**Have fun, and happy hacking!**
