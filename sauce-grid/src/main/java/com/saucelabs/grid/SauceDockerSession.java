package com.saucelabs.grid;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.docker.Container;
import org.openqa.selenium.grid.node.ProtocolConvertingSession;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.remote.Dialect;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.tracing.Tracer;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;

public class SauceDockerSession extends ProtocolConvertingSession {
  private final Container container;
  private final Container videoContainer;

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
    Instant startTime) {
    super(tracer, client, id, url, downstream, upstream, stereotype, capabilities, startTime);
    this.container = Require.nonNull("Container", container);
    this.videoContainer = videoContainer;
  }

  @Override
  public void stop() {
    if (videoContainer != null) {
      videoContainer.stop(Duration.ofSeconds(10));
    }
    container.stop(Duration.ofMinutes(1));
  }
}
