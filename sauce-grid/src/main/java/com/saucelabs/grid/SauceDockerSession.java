package com.saucelabs.grid;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.docker.Container;
import org.openqa.selenium.grid.docker.DockerSessionAssetsPath;
import org.openqa.selenium.grid.node.ProtocolConvertingSession;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.remote.Dialect;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.tracing.Tracer;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SauceDockerSession extends ProtocolConvertingSession {
  private static final Logger LOG = Logger.getLogger(SauceDockerSession.class.getName());
  private final Container container;
  private final Container videoContainer;
  private final AtomicInteger screenshotCount;
  private final DockerSessionAssetsPath assetsPath;

  SauceDockerSession(
    Container container,
    Container videoContainer,
    Tracer tracer,
    HttpClient client,
    SessionId id,
    URL url,
    Capabilities stereotype,
    Capabilities capabilities,
    Dialect downstream,
    Dialect upstream,
    Instant startTime,
    DockerSessionAssetsPath assetsPath) {
    super(tracer, client, id, url, downstream, upstream, stereotype, capabilities, startTime);
    this.container = Require.nonNull("Container", container);
    this.videoContainer = videoContainer;
    this.assetsPath = Require.nonNull("Assets path", assetsPath);
    this.screenshotCount = new AtomicInteger(0);
  }

  public int increaseScreenshotCount() {
    return screenshotCount.incrementAndGet();
  }

  public DockerSessionAssetsPath getAssetsPath() {
    return assetsPath;
  }

  @Override
  public void stop() {
    if (videoContainer != null) {
      videoContainer.stop(Duration.ofSeconds(10));
    }
    List<String> logs = container.getLogs().getLogs();
    Optional<Path> logsPath = assetsPath.createContainerSessionAssetsPath(getId());
    if (logsPath.isPresent() && !logs.isEmpty()) {
      String filePathPng = String.format("%s/selenium-server.log", logsPath.get());
      try {
        Files.write(Paths.get(filePathPng), logs);
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Error saving Selenium Server log", e);
      }
    }
    container.stop(Duration.ofMinutes(1));
  }
}
