package com.saucelabs.grid;

import static com.saucelabs.grid.Common.JSON;
import static com.saucelabs.grid.Common.SAUCE_OPTIONS;
import static com.saucelabs.grid.Common.getSauceCapability;
import static java.util.Optional.ofNullable;
import static org.openqa.selenium.ImmutableCapabilities.copyOf;
import static org.openqa.selenium.docker.ContainerConfig.image;
import static org.openqa.selenium.remote.Dialect.W3C;
import static org.openqa.selenium.remote.http.Contents.string;
import static org.openqa.selenium.remote.http.HttpMethod.GET;
import static org.openqa.selenium.remote.http.HttpMethod.POST;
import static org.openqa.selenium.remote.tracing.Tags.EXCEPTION;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.PersistentCapabilities;
import org.openqa.selenium.RetrySessionRequestException;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UsernameAndPassword;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.docker.Container;
import org.openqa.selenium.docker.ContainerConfig;
import org.openqa.selenium.docker.ContainerInfo;
import org.openqa.selenium.docker.Device;
import org.openqa.selenium.docker.Docker;
import org.openqa.selenium.docker.Image;
import org.openqa.selenium.docker.Port;
import org.openqa.selenium.grid.data.CreateSessionRequest;
import org.openqa.selenium.grid.data.DefaultSlotMatcher;
import org.openqa.selenium.grid.data.SlotMatcher;
import org.openqa.selenium.grid.node.ActiveSession;
import org.openqa.selenium.grid.node.SessionFactory;
import org.openqa.selenium.grid.node.docker.DockerAssetsPath;
import org.openqa.selenium.internal.Either;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.Dialect;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.ProtocolHandshake;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.ClientConfig;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.TreeMap;
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
  private final List<Device> devices;
  private final Image videoImage;
  private final Image assetsUploaderImage;
  private final DockerAssetsPath assetsPath;
  private final String networkName;
  private final boolean runningInDocker;
  private final SlotMatcher slotMatcher;
  private final Duration sessionTimeout;

  public SauceDockerSessionFactory(
    Tracer tracer,
    HttpClient.Factory clientFactory,
    Duration sessionTimeout,
    Docker docker,
    URI dockerUri,
    Image browserImage,
    Capabilities stereotype,
    List<Device> devices,
    Image videoImage,
    Image assetsUploaderImage,
    DockerAssetsPath assetsPath,
    String networkName,
    boolean runningInDocker) {
    this.tracer = Require.nonNull("Tracer", tracer);
    this.clientFactory = Require.nonNull("HTTP client", clientFactory);
    this.sessionTimeout = Require.nonNull("Session timeout", sessionTimeout);
    this.docker = Require.nonNull("Docker command", docker);
    this.dockerUri = Require.nonNull("Docker URI", dockerUri);
    this.browserImage = Require.nonNull("Docker browser image", browserImage);
    this.networkName = Require.nonNull("Docker network name", networkName);
    this.stereotype = copyOf(Require.nonNull("Stereotype", stereotype));
    this.devices = Require.nonNull("Container devices", devices);
    this.videoImage = videoImage;
    this.assetsUploaderImage = assetsUploaderImage;
    this.assetsPath = assetsPath;
    this.runningInDocker = runningInDocker;
    this.slotMatcher = new DefaultSlotMatcher();
  }

  @Override
  public boolean test(Capabilities capabilities) {
    return slotMatcher.matches(stereotype, capabilities);
  }

  @Override
  public Either<WebDriverException, ActiveSession> apply(CreateSessionRequest sessionRequest) {
    Optional<Object> accessKey =
      getSauceCapability(sessionRequest.getDesiredCapabilities(), "accessKey");
    Optional<Object> userName =
      getSauceCapability(sessionRequest.getDesiredCapabilities(), "username");
    if (!accessKey.isPresent() && !userName.isPresent()) {
      String message = String.format("Unable to create session. No Sauce Labs accessKey and "
                       + "username were found in the '%s' block.", SAUCE_OPTIONS);
      LOG.log(Level.WARNING, message);
      return Either.left(new SessionNotCreatedException(message));
    }
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    UsernameAndPassword usernameAndPassword =
      new UsernameAndPassword(userName.get().toString(), accessKey.get().toString());

    Optional<Object> dc =
      getSauceCapability(sessionRequest.getDesiredCapabilities(), "dataCenter");
    DataCenter dataCenter = DataCenter.US_WEST;
    if (dc.isPresent()) {
      dataCenter = DataCenter.fromString(String.valueOf(dc.get()));
    }

    Capabilities sessionReqCaps = removeSauceKey(sessionRequest.getDesiredCapabilities());
    LOG.info("Starting session for " + sessionReqCaps);

    int port = runningInDocker ? 4444 : PortProber.findFreePort();
    try (Span span = tracer.getCurrentContext().createSpan("docker_session_factory.apply")) {
      Map<String, EventAttributeValue> attributeMap = new HashMap<>();
      attributeMap.put(AttributeKey.LOGGER_CLASS.getKey(),
                       EventAttribute.setValue(this.getClass().getName()));

      String logMessage = runningInDocker ? "Creating container..." :
                          "Creating container, mapping container port 4444 to " + port;
      LOG.info(logMessage);
      Container container = createBrowserContainer(port, sessionReqCaps);
      container.start();
      ContainerInfo containerInfo = container.inspect();

      String containerIp = containerInfo.getIp();
      URL remoteAddress = getUrl(port, containerIp);
      ClientConfig clientConfig = ClientConfig
        .defaultConfig()
        .baseUrl(remoteAddress)
        .readTimeout(sessionTimeout);
      HttpClient client = clientFactory.createClient(clientConfig);

      attributeMap.put("docker.browser.image", EventAttribute.setValue(browserImage.toString()));
      attributeMap.put("container.port", EventAttribute.setValue(port));
      attributeMap.put("container.id", EventAttribute.setValue(container.getId().toString()));
      attributeMap.put("container.ip", EventAttribute.setValue(containerInfo.getIp()));
      attributeMap.put("docker.server.url", EventAttribute.setValue(remoteAddress.toString()));

      LOG.info(
        String.format("Waiting for server to start (container id: %s, url %s)",
                      container.getId(),
                      remoteAddress));
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
        String message = String.format(
          "Unable to connect to docker server (container id: %s)", container.getId());
        LOG.warning(message);
        return Either.left(new RetrySessionRequestException(message));
      }
      LOG.info(String.format("Server is ready (container id: %s)", container.getId()));

      Command command = new Command(
        null,
        DriverCommand.NEW_SESSION(sessionReqCaps));
      ProtocolHandshake.Result result;
      Response response;
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
        String message = "Unable to create session: " + e.getMessage();
        LOG.log(Level.WARNING, message);
        return Either.left(new SessionNotCreatedException(message));
      }

      SessionId id = new SessionId(response.getSessionId());
      Capabilities capabilities = new ImmutableCapabilities((Map<?, ?>) response.getValue());
      Capabilities mergedCapabilities = capabilities.merge(sessionReqCaps);

      Container videoContainer = null;
      Optional<DockerAssetsPath> path = ofNullable(this.assetsPath);
      if (path.isPresent()) {
        // Seems we can store session assets
        String containerPath = path.get().getContainerPath(id);
        saveSessionCapabilities(mergedCapabilities, containerPath);
        String hostPath = path.get().getHostPath(id);
        videoContainer = startVideoContainer(mergedCapabilities, containerInfo.getIp(), hostPath);
      }
      Instant startTime = Instant.now();
      Instant videoStartTime = Instant.now();

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
        .setStartTime(startTime.toEpochMilli())
        .setVideoStartTime(videoStartTime.toEpochMilli())
        .setEndTime(Instant.now().toEpochMilli())
        .setRequest(sessionReqCaps)
        .setResult(mergedCapabilities)
        .setPath("/session")
        .setHttpStatus(response.getStatus())
        .setHttpMethod(POST.name())
        .setStatusCode(0)
        .setScreenshotId(-1)
        .build();

      span.addEvent("Docker driver service created session", attributeMap);
      LOG.fine(String.format(
        "Created session: %s - %s (container id: %s)",
        id,
        capabilities,
        container.getId()));
      return Either.right(new SauceDockerSession(
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
        usernameAndPassword,
        dataCenter,
        assetsUploaderImage,
        commandInfo,
        docker));
    }
  }

  private Container createBrowserContainer(int port, Capabilities sessionCapabilities) {
    Map<String, String> browserContainerEnvVars = getBrowserContainerEnvVars(sessionCapabilities);
    long browserContainerShmMemorySize = 2147483648L; //2GB
    ContainerConfig containerConfig = image(browserImage)
      .env(browserContainerEnvVars)
      .shmMemorySize(browserContainerShmMemorySize)
      .network(networkName)
      .devices(devices);
    if (!runningInDocker) {
      containerConfig = containerConfig.map(Port.tcp(4444), Port.tcp(port));
    }
    return docker.create(containerConfig);
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
    int videoPort = 9000;
    Map<String, String> envVars = getVideoContainerEnvVars(
      sessionCapabilities,
      browserContainerIp);
    Map<String, String> volumeBinds = Collections.singletonMap(hostPath, "/videos");
    ContainerConfig containerConfig = image(videoImage)
      .env(envVars)
      .bind(volumeBinds)
      .network(networkName);
    if (!runningInDocker) {
      videoPort = PortProber.findFreePort();
      containerConfig = containerConfig.map(Port.tcp(9000), Port.tcp(videoPort));
    }
    Container videoContainer = docker.create(containerConfig);
    videoContainer.start();
    String videoContainerIp = runningInDocker ? videoContainer.inspect().getIp() : "localhost";
    try {
      URL videoContainerUrl = new URL(String.format("http://%s:%s", videoContainerIp, videoPort));
      HttpClient videoClient = clientFactory.createClient(videoContainerUrl);
      LOG.fine(String.format("Waiting for video recording... (id: %s)", videoContainer.getId()));
      waitForServerToStart(videoClient, Duration.ofMinutes(1));
    } catch (Exception e) {
      videoContainer.stop(Duration.ofSeconds(10));
      String message = String.format(
        "Unable to verify video recording started (container id: %s), %s", videoContainer.getId(),
        e.getMessage());
      LOG.warning(message);
    }
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
      getSauceCapability(sessionRequestCapabilities, "timeZone");
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
      getSauceCapability(sessionRequestCapabilities, "screenResolution");
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
      getSauceCapability(sessionRequestCapabilities, "recordVideo");
    if (recordVideoCapability.isPresent()) {
      recordVideo = Boolean.parseBoolean(recordVideoCapability.get().toString());
    }
    return recordVideo;
  }

  private void saveSessionCapabilities(Capabilities sessionRequestCapabilities, String path) {
    String capsToJson = JSON.toJson(sessionRequestCapabilities);
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

  private URL getUrl(int port, String containerIp) {
    try {
      String host = "localhost";
      if (runningInDocker) {
        host = containerIp;
      } else {
        if (dockerUri.getScheme().startsWith("tcp") || dockerUri.getScheme().startsWith("http")) {
          host = dockerUri.getHost();
        }
      }
      return new URL(String.format("http://%s:%s/wd/hub", host, port));
    } catch (MalformedURLException e) {
      throw new SessionNotCreatedException(e.getMessage(), e);
    }
  }

  private Capabilities removeSauceKey(Capabilities capabilities) {
    Capabilities filteredCaps = copyOf(capabilities);
    Object rawSauceOptions = filteredCaps.getCapability(SAUCE_OPTIONS);
    if (rawSauceOptions instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> original = (Map<String, Object>) rawSauceOptions;
      Map<String, Object> updated = new TreeMap<>(original);
      updated.remove("accessKey");
      return new PersistentCapabilities(filteredCaps).setCapability(SAUCE_OPTIONS, updated);
    }
    return capabilities;
  }

}
