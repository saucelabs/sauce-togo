package com.saucelabs.grid;

import static java.util.Optional.ofNullable;
import static org.openqa.selenium.docker.ContainerConfig.image;
import static org.openqa.selenium.remote.Dialect.W3C;
import static org.openqa.selenium.remote.http.Contents.string;
import static org.openqa.selenium.remote.http.HttpMethod.GET;
import static org.openqa.selenium.remote.http.HttpMethod.POST;
import static org.openqa.selenium.remote.tracing.Tags.EXCEPTION;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.docker.Container;
import org.openqa.selenium.docker.ContainerInfo;
import org.openqa.selenium.docker.Docker;
import org.openqa.selenium.docker.Image;
import org.openqa.selenium.docker.Port;
import org.openqa.selenium.grid.data.CreateSessionRequest;
import org.openqa.selenium.grid.docker.DockerAssetsPath;
import org.openqa.selenium.grid.node.ActiveSession;
import org.openqa.selenium.grid.node.SessionFactory;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.Dialect;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.ProtocolHandshake;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.tracing.AttributeKey;
import org.openqa.selenium.remote.tracing.EventAttribute;
import org.openqa.selenium.remote.tracing.EventAttributeValue;
import org.openqa.selenium.remote.tracing.Span;
import org.openqa.selenium.remote.tracing.Status;
import org.openqa.selenium.remote.tracing.Tracer;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SauceDockerSessionFactory implements SessionFactory {
  private static final Logger LOG = Logger.getLogger(SauceDockerSessionFactory.class.getName());

  private final Tracer tracer;
  private final HttpClient.Factory clientFactory;
  private final Docker docker;
  private final URI dockerUri;
  private final Image browserImage;
  private final Capabilities stereotype;
  private final Image videoImage;
  private final DockerAssetsPath assetsPath;

  public SauceDockerSessionFactory(
    Tracer tracer,
    HttpClient.Factory clientFactory,
    Docker docker,
    URI dockerUri,
    Image browserImage,
    Capabilities stereotype,
    Image videoImage,
    DockerAssetsPath assetsPath) {
    this.tracer = Require.nonNull("Tracer", tracer);
    this.clientFactory = Require.nonNull("HTTP client", clientFactory);
    this.docker = Require.nonNull("Docker command", docker);
    this.dockerUri = Require.nonNull("Docker URI", dockerUri);
    this.browserImage = Require.nonNull("Docker browser image", browserImage);
    this.stereotype = ImmutableCapabilities.copyOf(
      Require.nonNull("Stereotype", stereotype));
    this.videoImage = videoImage;
    this.assetsPath = assetsPath;
  }

  @Override
  public boolean test(Capabilities capabilities) {
    return stereotype.getCapabilityNames().stream()
      .map(
        name ->
          Objects.equals(stereotype.getCapability(name), capabilities.getCapability(name)))
      .reduce(Boolean::logicalAnd)
      .orElse(false);
  }

  @Override
  public Optional<ActiveSession> apply(CreateSessionRequest sessionRequest) {
    LOG.info("Starting session for " + sessionRequest.getCapabilities());
    int port = PortProber.findFreePort();
    URL remoteAddress = getUrl(port);
    HttpClient client = clientFactory.createClient(remoteAddress);

    try (Span span = tracer.getCurrentContext().createSpan("docker_session_factory.apply")) {
      Map<String, EventAttributeValue> attributeMap = new HashMap<>();
      attributeMap.put(AttributeKey.LOGGER_CLASS.getKey(),
                       EventAttribute.setValue(this.getClass().getName()));
      LOG.info("Creating container, mapping container port 4444 to " + port);
      Container container = createBrowserContainer(port, sessionRequest.getCapabilities());
      container.start();
      ContainerInfo containerInfo = container.inspect();

      attributeMap.put("docker.browser.image", EventAttribute.setValue(browserImage.toString()));
      attributeMap.put("container.port", EventAttribute.setValue(port));
      attributeMap.put("container.id", EventAttribute.setValue(container.getId().toString()));
      attributeMap.put("container.ip", EventAttribute.setValue(containerInfo.getIp()));
      attributeMap.put("docker.server.url", EventAttribute.setValue(remoteAddress.toString()));

      LOG.info(String.format("Waiting for server to start (container id: %s)", container.getId()));
      try {
        waitForServerToStart(client, Duration.ofMinutes(1));
      } catch (TimeoutException e) {
        span.setAttribute("error", true);
        span.setStatus(Status.CANCELLED);

        EXCEPTION.accept(attributeMap, e);
        attributeMap.put(AttributeKey.EXCEPTION_MESSAGE.getKey(),
                         EventAttribute.setValue(
                           "Unable to connect to docker server. Stopping container: " +
                           e.getMessage()));
        span.addEvent(AttributeKey.EXCEPTION_EVENT.getKey(), attributeMap);

        container.stop(Duration.ofMinutes(1));
        LOG.warning(String.format(
          "Unable to connect to docker server (container id: %s)", container.getId()));
        return Optional.empty();
      }
      LOG.info(String.format("Server is ready (container id: %s)", container.getId()));

      Command command = new Command(
        null,
        DriverCommand.NEW_SESSION(sessionRequest.getCapabilities()));
      ProtocolHandshake.Result result;
      Response response;
      Instant startTime = Instant.now();
      try {
        result = new ProtocolHandshake().createSession(client, command);
        response = result.createResponse();
        attributeMap.put(
          AttributeKey.DRIVER_RESPONSE.getKey(),
          EventAttribute.setValue(response.toString()));
      } catch (IOException | RuntimeException e) {
        span.setAttribute("error", true);
        span.setStatus(Status.CANCELLED);

        EXCEPTION.accept(attributeMap, e);
        attributeMap.put(
          AttributeKey.EXCEPTION_MESSAGE.getKey(),
          EventAttribute
            .setValue("Unable to create session. Stopping and  container: " + e.getMessage()));
        span.addEvent(AttributeKey.EXCEPTION_EVENT.getKey(), attributeMap);

        container.stop(Duration.ofMinutes(1));
        LOG.log(Level.WARNING, "Unable to create session: " + e.getMessage(), e);
        return Optional.empty();
      }

      SessionId id = new SessionId(response.getSessionId());
      Capabilities capabilities = new ImmutableCapabilities((Map<?, ?>) response.getValue());
      Capabilities mergedCapabilities = capabilities.merge(sessionRequest.getCapabilities());

      Container videoContainer = null;
      Optional<DockerAssetsPath> path = ofNullable(this.assetsPath);
      Instant videoStartTime = Instant.now();
      if (path.isPresent()) {
        // Seems we can store session assets
        String containerPath = path.get().getContainerPath(id);
        saveSessionCapabilities(mergedCapabilities, containerPath);
        String hostPath = path.get().getHostPath(id);
        videoContainer = startVideoContainer(mergedCapabilities, containerInfo.getIp(), hostPath);
      }

      Dialect downstream = sessionRequest.getDownstreamDialects().contains(result.getDialect()) ?
                           result.getDialect() :
                           W3C;
      attributeMap.put(
        AttributeKey.DOWNSTREAM_DIALECT.getKey(),
        EventAttribute.setValue(downstream.toString()));
      attributeMap.put(
        AttributeKey.DRIVER_RESPONSE.getKey(),
        EventAttribute.setValue(response.toString()));

      SauceCommandInfo commandInfo = new SauceCommandInfo.Builder()
        .setStartTime(startTime.getEpochSecond())
        .setVideoStartTime(videoStartTime.getEpochSecond())
        .setEndTime(Instant.now().getEpochSecond())
        .setRequest(sessionRequest.getCapabilities())
        .setResult(mergedCapabilities)
        .setPath("/session")
        .setHttpStatus(response.getStatus())
        .setHttpMethod(POST.name())
        .setStatusCode(0)
        .build();

      span.addEvent("Docker driver service created session", attributeMap);
      LOG.fine(String.format(
        "Created session: %s - %s (container id: %s)",
        id,
        capabilities,
        container.getId()));
      return Optional.of(new SauceDockerSession(
        container,
        videoContainer,
        tracer,
        client,
        id,
        remoteAddress,
        stereotype,
        mergedCapabilities,
        downstream,
        result.getDialect(),
        startTime,
        assetsPath,
        commandInfo,
        docker));
    }
  }

  private Container createBrowserContainer(int port, Capabilities sessionCapabilities) {
    Map<String, String> browserContainerEnvVars =
      getBrowserContainerEnvVars(sessionCapabilities);
    Map<String, String> devShmMount =
      Collections.singletonMap("/dev/shm", "/dev/shm");
    return docker.create(
      image(browserImage)
        .env(browserContainerEnvVars)
        .bind(devShmMount)
        .map(Port.tcp(4444), Port.tcp(port)));
  }

  private Map<String, String> getBrowserContainerEnvVars(Capabilities sessionRequestCapabilities) {
    Optional<Dimension> screenResolution =
      ofNullable(getScreenResolution(sessionRequestCapabilities));
    Map<String, String> envVars = new HashMap<>();
    if (screenResolution.isPresent()) {
      envVars.put("SCREEN_WIDTH", String.valueOf(screenResolution.get().getWidth()));
      envVars.put("SCREEN_HEIGHT", String.valueOf(screenResolution.get().getHeight()));
    }
    Optional<TimeZone> timeZone = ofNullable(getTimeZone(sessionRequestCapabilities));
    timeZone.ifPresent(zone -> envVars.put("TZ", zone.getID()));
    return envVars;
  }

  private Container startVideoContainer(Capabilities sessionCapabilities,
                                        String browserContainerIp, String hostPath) {
    if (!recordVideoForSession(sessionCapabilities)) {
      return null;
    }
    Map<String, String> envVars = getVideoContainerEnvVars(
      sessionCapabilities,
      browserContainerIp);
    Map<String, String> volumeBinds = Collections.singletonMap(hostPath, "/videos");
    Container videoContainer = docker.create(image(videoImage).env(envVars).bind(volumeBinds));
    videoContainer.start();
    LOG.info(String.format("Video container started (id: %s)", videoContainer.getId()));
    return videoContainer;
  }

  private Map<String, String> getVideoContainerEnvVars(Capabilities sessionRequestCapabilities,
                                                       String containerIp) {
    Map<String, String> envVars = new HashMap<>();
    envVars.put("DISPLAY_CONTAINER_NAME", containerIp);
    Optional<Dimension> screenResolution =
      ofNullable(getScreenResolution(sessionRequestCapabilities));
    screenResolution.ifPresent(dimension -> envVars
      .put("VIDEO_SIZE", String.format("%sx%s", dimension.getWidth(), dimension.getHeight())));
    return envVars;
  }

  private TimeZone getTimeZone(Capabilities sessionRequestCapabilities) {
    Optional<Object> timeZone =
      ofNullable(getCapability(sessionRequestCapabilities, "timeZone"));
    if (timeZone.isPresent()) {
      String tz =  timeZone.get().toString();
      if (Arrays.asList(TimeZone.getAvailableIDs()).contains(tz)) {
        return TimeZone.getTimeZone(tz);
      }
    }
    return null;
  }

  private Dimension getScreenResolution(Capabilities sessionRequestCapabilities) {
    Optional<Object> screenResolution =
      ofNullable(getCapability(sessionRequestCapabilities, "screenResolution"));
    if (!screenResolution.isPresent()) {
      return null;
    }
    try {
      String[] resolution = screenResolution.get().toString().split("x");
      int screenWidth = Integer.parseInt(resolution[0]);
      int screenHeight = Integer.parseInt(resolution[1]);
      if (screenWidth > 0 && screenHeight > 0) {
        return new Dimension(screenWidth, screenHeight);
      } else {
        LOG.warning("One of the values provided for screenResolution is negative, " +
                    "defaults will be used. Received value: " + screenResolution);
      }
    } catch (Exception e) {
      LOG.warning("Values provided for screenResolution are not valid integers or " +
                  "either width or height are missing, defaults will be used." +
                  "Received value: " + screenResolution);
    }
    return null;
  }

  private boolean recordVideoForSession(Capabilities sessionRequestCapabilities) {
    boolean recordVideo = true;
    Optional<Object> recordVideoCapability =
      ofNullable(getCapability(sessionRequestCapabilities, "recordVideo"));
    if (recordVideoCapability.isPresent()) {
      recordVideo = Boolean.parseBoolean(recordVideoCapability.get().toString());
    }
    return recordVideo;
  }

  private Object getCapability(Capabilities sessionRequestCapabilities, String capabilityName) {
    Object rawSeleniumOptions =
      sessionRequestCapabilities.getCapability("sauce:options");
    if (rawSeleniumOptions instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> seleniumOptions = (Map<String, Object>) rawSeleniumOptions;
      return seleniumOptions.get(capabilityName);
    }
    return null;
  }

  private void saveSessionCapabilities(Capabilities sessionRequestCapabilities, String path) {
    String capsToJson = new Json().toJson(sessionRequestCapabilities);
    try {
      Files.createDirectories(Paths.get(path));
      Files.write(
        Paths.get(path, "sessionCapabilities.json"),
        capsToJson.getBytes(Charset.defaultCharset()));
    } catch (IOException e) {
      LOG.log(Level.WARNING,
              "Failed to save session capabilities", e);
    }
  }

  private void waitForServerToStart(HttpClient client, Duration duration) {
    Wait<Object> wait = new FluentWait<>(new Object())
      .withTimeout(duration)
      .ignoring(UncheckedIOException.class);

    wait.until(obj -> {
      HttpResponse response = client.execute(new HttpRequest(GET, "/status"));
      LOG.fine(string(response));
      return 200 == response.getStatus();
    });
  }

  private URL getUrl(int port) {
    try {
      String host = "localhost";
      if (dockerUri.getScheme().startsWith("tcp") || dockerUri.getScheme().startsWith("http")) {
        host = dockerUri.getHost();
      }
      return new URL(String.format("http://%s:%s/wd/hub", host, port));
    } catch (MalformedURLException e) {
      throw new SessionNotCreatedException(e.getMessage(), e);
    }
  }

}
