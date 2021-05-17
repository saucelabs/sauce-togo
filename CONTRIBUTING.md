# Contributing to _Sauce To Go_

**Thank you for your interest in _Sauce To Go_. Your contributions are highly welcome.**

There are multiple ways of getting involved:

- [Requirements](#requirements)
- [Set up project](#set-up-and-build-project)
- [Release Sauce To Go](#release-sauce-to-go)
- [Report a bug](#report-a-bug)
- [Suggest a feature](#suggest-a-feature)
- [Contribute code](#contribute-code)

Below are a few guidelines we would like you to follow.
If you need help, please reach out to us by opening an issue.

## Requirements

This repository uses Java 8, [Maven](http://maven.apache.org/) and [Docker](https://www.docker.com/).

You only need Docker to build the project and Docker images. However, if the intention is to 
develop, debug, and troubleshoot the code, it is preferred to install all requirements.

We recommend to use [IntelliJ Idea](https://www.jetbrains.com/idea/download) for development of this
project, and using the Community Edition is more than enough. Any other Java capable IDE should also
be sufficient, but probably we won't be able to support you with debugging instructions. 

## Set up and build project

Start by cloning the repo:

```sh
$ git clone git@github.com:saucelabs/sauce-togo.git
$ cd sauce-togo
```

Run these commands on the root directory of the project.

1. Building all jars.

If you have all requirements installed:
```sh
mvn clean package
``` 

Only using Docker (in case you want to avoid installing and configuring Java):
```sh
docker run --rm \
  -v ${PWD}:/usr/src/mymaven \
  -v ${PWD}/.m2:/root/.m2 \
  -w /usr/src/mymaven \
  maven:3.6.3-jdk-8 mvn clean package
```

2. Move the generated jars to the `docker` directory, we'll use them to build the Docker images

```sh
# Move the jar with Grid extension to the docker directory 
mv sauce-grid/target/sauce-grid-<POM-VERSION>-jar-with-dependencies.jar docker/selenium-server.jar
# Move the Uploader jar to the docker directory
mv sauce-assets-uploader/target/sauce-assets-uploader-<POM-VERSION>-jar-with-dependencies.jar docker/sauce-assets-uploader.jar 
```

3. Build all the Docker images

```sh
# Go to the docker directory and build all the Docker images
cd docker && make all && cd ..
```

To double check, you can run `docker images --filter=reference='saucelabs/*:*'` to see the
generated images. To run Sauce To Go, please refer to the instructions described on the 
[README](./README.md). Be sure to use the same tags you used to generate the Docker images.

### Debugging and running Sauce Grid outside Docker

Please check the comments in the [Main](./sauce-grid/src/main/java/com/saucelabs/grid/Main.java)
class.

## Release Sauce To Go

The [push-images.yml](./.github/workflows/push-images.yml) automates most of the process. Releases
can only happen from the `main` branch. To trigger a release, add the keyword `[push]` (including
brackets) to your commit message on the `main` branch. Tagging and pushing the Docker images to 
Docker Hub happens automatically.

**Note:** When adding the `[push]` keyword to the commit message, make sure to not skip the build 
by having keywords such as [`[skip ci]`](https://github.blog/changelog/2021-02-08-github-actions-skip-pull-request-and-push-workflows-with-skip-ci/)
as part of the commit message. 

In order to update and release a new Sauce Connect image, do the following:

## Report a bug 

Reporting bugs is one of the best ways to contribute. Before creating a bug report, please check 
if an [issue](/issues) reporting the same problem does not already exist. If there is such an 
issue, you may add your information as a comment.

To report a new bug you should open an issue that summarizes the bug and set the label to "bug".

If you want to provide a fix along with your bug report: That is great! In this case please 
send us a pull request as described in section [Contribute Code](#contribute-code).

## Suggest a feature
To request a new feature you should open an [issue](../../issues/new) and summarize the desired 
functionality and its use case. Set the issue label to "feature".  

## Contribute code
This is an outline of what the workflow for code contributions looks like

- Check the list of open [issues](../../issues). Either assign an existing issue to yourself, or 
create a new one if you would like work on and discuss your ideas and use cases. 

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

### Commit messages
Your commit messages ideally can answer two questions: what changed and why. The subject line 
should feature the “what”, and the body of the commit should describe the “why”.  

When creating a pull request, its description should reference the corresponding issue id.

### Sign your work / Developer certificate of origin
All contributions (including pull requests) must agree to the Developer Certificate of Origin 
(DCO) version 1.1. This is exactly the same one created and used by the Linux kernel developers 
and posted on http://developercertificate.org/. This is a developer's certification that they have 
the right to submit the patch for inclusion into the project. Simply submitting a contribution 
implies this agreement, however, please include a "Signed-off-by" tag in every patch 
(this tag is a conventional way to confirm that you agree to the DCO) - you can automate 
this with a [Git hook](https://stackoverflow.com/questions/15015894/git-add-signed-off-by-line-using-format-signoff-not-working).

```
git commit -s -m "adding X to change Y"
```

**Have fun, and happy hacking!**
