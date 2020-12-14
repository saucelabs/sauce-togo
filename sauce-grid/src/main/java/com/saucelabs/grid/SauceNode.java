package com.saucelabs.grid;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Optional.ofNullable;
import static org.openqa.selenium.OutputType.FILE;
import static org.openqa.selenium.grid.data.Availability.DRAINING;
import static org.openqa.selenium.grid.data.Availability.UP;
import static org.openqa.selenium.grid.node.CapabilityResponseEncoder.getEncoder;
import static org.openqa.selenium.json.Json.MAP_TYPE;
import static org.openqa.selenium.json.Json.OBJECT_TYPE;
import static org.openqa.selenium.remote.HttpSessionId.getSessionId;
import static org.openqa.selenium.remote.RemoteTags.CAPABILITIES;
import static org.openqa.selenium.remote.RemoteTags.SESSION_ID;
import static org.openqa.selenium.remote.http.Contents.asJson;
import static org.openqa.selenium.remote.http.Contents.string;
import static org.openqa.selenium.remote.http.HttpMethod.DELETE;
import static org.openqa.selenium.remote.http.HttpMethod.GET;
import static org.openqa.selenium.remote.http.HttpMethod.POST;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.PersistentCapabilities;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.concurrent.Regularly;
import org.openqa.selenium.events.EventBus;
import org.openqa.selenium.grid.data.CreateSessionRequest;
import org.openqa.selenium.grid.data.CreateSessionResponse;
import org.openqa.selenium.grid.data.NodeDrainComplete;
import org.openqa.selenium.grid.data.NodeDrainStarted;
import org.openqa.selenium.grid.data.NodeId;
import org.openqa.selenium.grid.data.NodeStatus;
import org.openqa.selenium.grid.data.Session;
import org.openqa.selenium.grid.data.SessionClosedEvent;
import org.openqa.selenium.grid.data.Slot;
import org.openqa.selenium.grid.data.SlotId;
import org.openqa.selenium.grid.docker.DockerAssetsPath;
import org.openqa.selenium.grid.node.ActiveSession;
import org.openqa.selenium.grid.node.HealthCheck;
import org.openqa.selenium.grid.node.Node;
import org.openqa.selenium.grid.node.SessionFactory;
import org.openqa.selenium.grid.node.local.LocalNode;
import org.openqa.selenium.grid.node.local.SessionSlot;
import org.openqa.selenium.grid.security.Secret;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.io.TemporaryFilesystem;
import org.openqa.selenium.io.Zip;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.tracing.AttributeKey;
import org.openqa.selenium.remote.tracing.EventAttribute;
import org.openqa.selenium.remote.tracing.EventAttributeValue;
import org.openqa.selenium.remote.tracing.Span;
import org.openqa.selenium.remote.tracing.Status;
import org.openqa.selenium.remote.tracing.Tracer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SauceNode extends Node {
  private static final Json JSON = new Json();
  private static final Logger LOG = Logger.getLogger(LocalNode.class.getName());
  private final EventBus bus;
  private final URI externalUri;
  private final URI gridUri;
  private final HealthCheck healthCheck;
  private final int maxSessionCount;
  private final List<SessionSlot> factories;
  private final Cache<SessionId, SessionSlot> currentSessions;
  private final Cache<SessionId, TemporaryFilesystem> tempFileSystems;
  private final Regularly regularly;
  private final Secret registrationSecret;
  private AtomicInteger pendingSessions = new AtomicInteger();

  private SauceNode(
    Tracer tracer,
    EventBus bus,
    URI uri,
    URI gridUri,
    HealthCheck healthCheck,
    int maxSessionCount,
    Ticker ticker,
    Duration sessionTimeout,
    List<SessionSlot> factories,
    Secret registrationSecret) {
    super(tracer, new NodeId(UUID.randomUUID()), uri, registrationSecret);

    this.bus = Require.nonNull("Event bus", bus);

    this.externalUri = Require.nonNull("Remote node URI", uri);
    this.gridUri = Require.nonNull("Grid URI", gridUri);
    this.maxSessionCount = Math.min(Require.positive("Max session count", maxSessionCount), factories.size());
    this.factories = ImmutableList.copyOf(factories);
    this.registrationSecret = Require.nonNull("Registration secret", registrationSecret);

    this.healthCheck = healthCheck == null ?
                       () -> new HealthCheck.Result(
                         isDraining() ? DRAINING : UP,
                         String.format("%s is %s", uri, isDraining() ? "draining" : "up")) :
                       healthCheck;

    this.currentSessions = CacheBuilder.newBuilder()
      .expireAfterAccess(sessionTimeout)
      .ticker(ticker)
      .removalListener((RemovalListener<SessionId, SessionSlot>) notification -> {
        // If we were invoked explicitly, then return: we know what we're doing.
        if (!notification.wasEvicted()) {
          return;
        }

        killSession(notification.getValue());
      })
      .build();

    this.tempFileSystems = CacheBuilder.newBuilder()
      .expireAfterAccess(sessionTimeout)
      .ticker(ticker)
      .removalListener((RemovalListener<SessionId, TemporaryFilesystem>) notification -> {
        TemporaryFilesystem tempFS = notification.getValue();
        tempFS.deleteTemporaryFiles();
        tempFS.deleteBaseDir();
      })
      .build();

    this.regularly = new Regularly("Local Node: " + externalUri);
    regularly.submit(currentSessions::cleanUp, Duration.ofSeconds(30), Duration.ofSeconds(30));
    regularly.submit(tempFileSystems::cleanUp, Duration.ofSeconds(30), Duration.ofSeconds(30));

    bus.addListener(SessionClosedEvent.listener(id -> {
      try {
        this.stop(id);
      } catch (NoSuchSessionException ignore) {
      }
      if (this.isDraining()) {
        int done = pendingSessions.decrementAndGet();
        if (done <= 0) {
          LOG.info("Firing node drain complete message");
          bus.fire(new NodeDrainComplete(this.getId()));
        }
      }
    }));
  }

  @Override
  public boolean isReady() {
    return bus.isReady();
  }

  @VisibleForTesting
  public int getCurrentSessionCount() {
    // It seems wildly unlikely we'll overflow an int
    return Math.toIntExact(currentSessions.size());
  }

  @Override
  public boolean isSupporting(Capabilities capabilities) {
    return factories.parallelStream().anyMatch(factory -> factory.test(capabilities));
  }

  @Override
  public Optional<CreateSessionResponse> newSession(CreateSessionRequest sessionRequest) {
    Require.nonNull("Session request", sessionRequest);

    try (Span span = tracer.getCurrentContext().createSpan("node.new_session")) {
      Map<String, EventAttributeValue> attributeMap = new HashMap<>();
      attributeMap
        .put(AttributeKey.LOGGER_CLASS.getKey(), EventAttribute.setValue(getClass().getName()));
      LOG.fine("Creating new session using span: " + span);
      attributeMap.put("session.request.capabilities",
                       EventAttribute.setValue(sessionRequest.getCapabilities().toString()));
      attributeMap.put("session.request.downstreamdialect",
                       EventAttribute.setValue(sessionRequest.getDownstreamDialects().toString()));
      int currentSessionCount = getCurrentSessionCount();
      span.setAttribute("current.session.count", currentSessionCount);
      attributeMap.put("current.session.count", EventAttribute.setValue(currentSessionCount));

      if (getCurrentSessionCount() >= maxSessionCount) {
        span.setAttribute("error", true);
        span.setStatus(Status.RESOURCE_EXHAUSTED);
        attributeMap.put("max.session.count", EventAttribute.setValue(maxSessionCount));
        span.addEvent("Max session count reached", attributeMap);
        return Optional.empty();
      }
      if (isDraining()) {
        span.setStatus(Status.UNAVAILABLE.withDescription("The node is draining. Cannot accept new sessions."));
        return Optional.empty();
      }

      // Identify possible slots to use as quickly as possible to enable concurrent session starting
      SessionSlot slotToUse = null;
      synchronized(factories) {
        for (SessionSlot factory : factories) {
          if (!factory.isAvailable() || !factory.test(sessionRequest.getCapabilities())) {
            continue;
          }

          factory.reserve();
          slotToUse = factory;
          break;
        }
      }

      if (slotToUse == null) {
        span.setAttribute("error", true);
        span.setStatus(Status.NOT_FOUND);
        span.addEvent("No slot matched capabilities ", attributeMap);
        return Optional.empty();
      }

      Optional<ActiveSession> possibleSession = slotToUse.apply(sessionRequest);

      if (!possibleSession.isPresent()) {
        slotToUse.release();
        span.setAttribute("error", true);
        span.setStatus(Status.NOT_FOUND);
        span.addEvent("No slots available for capabilities ", attributeMap);
        return Optional.empty();
      }

      ActiveSession session = possibleSession.get();
      currentSessions.put(session.getId(), slotToUse);

      SessionId sessionId = session.getId();
      Capabilities caps = session.getCapabilities();
      SESSION_ID.accept(span, sessionId);
      CAPABILITIES.accept(span, caps);
      String downstream = session.getDownstreamDialect().toString();
      String upstream = session.getUpstreamDialect().toString();
      String sessionUri = session.getUri().toString();
      span.setAttribute(AttributeKey.DOWNSTREAM_DIALECT.getKey(), downstream);
      span.setAttribute(AttributeKey.UPSTREAM_DIALECT.getKey(), upstream);
      span.setAttribute(AttributeKey.SESSION_URI.getKey(), sessionUri);

      // The session we return has to look like it came from the node, since we might be dealing
      // with a webdriver implementation that only accepts connections from localhost
      Session externalSession = createExternalSession(session, externalUri);
      return Optional.of(new CreateSessionResponse(
        externalSession,
        getEncoder(session.getDownstreamDialect()).apply(externalSession)));
    }
  }

  @Override
  public boolean isSessionOwner(SessionId id) {
    Require.nonNull("Session ID", id);
    return currentSessions.getIfPresent(id) != null;
  }

  @Override
  public Session getSession(SessionId id) throws NoSuchSessionException {
    Require.nonNull("Session ID", id);

    SessionSlot slot = currentSessions.getIfPresent(id);
    if (slot == null) {
      throw new NoSuchSessionException("Cannot find session with id: " + id);
    }

    return createExternalSession(slot.getSession(), externalUri);
  }

  @Override
  public TemporaryFilesystem getTemporaryFilesystem(SessionId id) throws IOException {
    try {
      return tempFileSystems.get(id, () -> TemporaryFilesystem.getTmpFsBasedOn(
        TemporaryFilesystem.getDefaultTmpFS().createTempDir("session", id.toString())));
    } catch (ExecutionException e) {
      throw new IOException(e);
    }
  }

  @Override
  public HttpResponse executeWebDriverCommand(HttpRequest req) {
    // True enough to be good enough
    SessionId id = getSessionId(req.getUri()).map(SessionId::new)
      .orElseThrow(() -> new NoSuchSessionException("Cannot find session: " + req));

    SessionSlot slot = currentSessions.getIfPresent(id);
    if (slot == null) {
      throw new NoSuchSessionException("Cannot find session with id: " + id);
    }

    SauceDockerSession session = (SauceDockerSession) slot.getSession();
    SauceCommandInfo.Builder builder = new SauceCommandInfo.Builder();
    builder.setStartTime(Instant.now().getEpochSecond());
    HttpResponse toReturn = slot.execute(req);

    if (req.getMethod() == DELETE && req.getUri().equals("/session/" + id)) {
      stop(id);
      builder.setScreenshotId(-1);
    } else {
      // Only taking screenshots after a url has been loaded
      if (!session.canTakeScreenshot() && req.getMethod() == POST
          && req.getUri().endsWith("/url")) {
        session.enableScreenshots();
      }
      int screenshotId = takeScreenshot(session, req, slot);
      builder.setScreenshotId(screenshotId);
    }
    Map<String, Object> parsedResponse =
      JSON.toType(new InputStreamReader(toReturn.getContent().get()), MAP_TYPE);
    builder.setEndTime(Instant.now().getEpochSecond())
      .setRequest(getRequestContents(req))
      .setResult(parsedResponse)
      .setPath(req.getUri().replace(String.format("/session/%s", id), ""))
      .setHttpStatus(toReturn.getStatus())
      .setHttpMethod(req.getMethod().name())
      .setStatusCode(0);
    if (parsedResponse.containsKey("value") && parsedResponse.get("value") != null
        && parsedResponse.get("value").toString().contains("error")) {
      builder.setStatusCode(1);
    }
    session.addSauceCommandInfo(builder.build());
    return toReturn;
  }

  private Object getRequestContents(HttpRequest httpRequest) {
    String reqContents = string(httpRequest);
    if (reqContents.isEmpty()) {
      return Collections.emptyMap();
    }
    return JSON.toType(reqContents, MAP_TYPE);
  }

  private int takeScreenshot(SauceDockerSession session, HttpRequest req, SessionSlot slot) {
    Optional<DockerAssetsPath> path = ofNullable(session.getAssetsPath());
    if (session.canTakeScreenshot() && shouldTakeScreenshot(req.getMethod(), req.getUri())
        && path.isPresent()) {
      HttpRequest screenshotRequest =
        new HttpRequest(GET, String.format("/session/%s/screenshot", session.getId()));
      HttpResponse screenshotResponse = slot.execute(screenshotRequest);
      int screenshotId = session.increaseScreenshotCount();
      String containerPath = path.get().getContainerPath(session.getId());
      String filePathPng = String.format(
        "%s/%s%s.png",
        containerPath,
        formatScreenshotId(screenshotId),
        "screenshot");
      String screenshotContent = string(screenshotResponse).trim();
      Map<String, Object> parsed = JSON.toType(screenshotContent, MAP_TYPE);
      String pngContent;
      if (parsed.containsKey("value")) {
        pngContent = (String) parsed.get("value");
      } else {
        pngContent = JSON.toType(screenshotContent, OBJECT_TYPE);
      }
      try {
        Files.createDirectories(Paths.get(containerPath));
        Files.copy(
          FILE.convertFromBase64Png(pngContent).toPath(),
          Paths.get(filePathPng),
          StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Error saving screenshot", e);
      }
      return screenshotId;
    }
    return -1;
  }

  private String formatScreenshotId(int count) {
    String screenshotCount = String.valueOf(count);
    return ("0000" + screenshotCount).substring(screenshotCount.length());
  }

  private boolean shouldTakeScreenshot(HttpMethod httpMethod, String requestUri) {
    // https://www.w3.org/TR/webdriver1/#list-of-endpoints
    ImmutableListMultimap<String, HttpMethod> commandChangesPage =
      ImmutableListMultimap.<String, HttpMethod>builder()
        .put("/url", POST)
        .put("/forward", POST)
        .put("/back", POST)
        .put("/refresh", POST)
        .put("/execute", POST)
        .put("/click", POST)
        .put("/frame", POST)
        .put("/parent", POST)
        .put("/rect", POST)
        .put("/maximize", POST)
        .put("/minimize", POST)
        .put("/fullscreen", POST)
        .put("/clear", POST)
        .put("/value", POST)
        .put("/actions", POST)
        .put("/alert", POST)
        .put("/window", POST)
        .put("/window", DELETE)
        .build();
    return commandChangesPage
      .entries()
      .stream()
      .anyMatch(pair -> pair.getValue().equals(httpMethod) && requestUri.contains(pair.getKey()));
  }

  @Override
  public HttpResponse uploadFile(HttpRequest req, SessionId id) {
    Map<String, Object> incoming = JSON.toType(string(req), Json.MAP_TYPE);

    File tempDir;
    try {
      TemporaryFilesystem tempfs = getTemporaryFilesystem(id);
      tempDir = tempfs.createTempDir("upload", "file");

      Zip.unzip((String) incoming.get("file"), tempDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    // Select the first file
    File[] allFiles = tempDir.listFiles();
    if (allFiles == null) {
      throw new WebDriverException(
        String.format("Cannot access temporary directory for uploaded files %s", tempDir));
    }
    if (allFiles.length != 1) {
      throw new WebDriverException(
        String.format("Expected there to be only 1 file. There were: %s", allFiles.length));
    }

    ImmutableMap<String, Object> result = ImmutableMap.of(
      "value", allFiles[0].getAbsolutePath());

    return new HttpResponse().setContent(asJson(result));
  }

  @Override
  public void stop(SessionId id) throws NoSuchSessionException {
    Require.nonNull("Session ID", id);

    SessionSlot slot = currentSessions.getIfPresent(id);
    if (slot == null) {
      throw new NoSuchSessionException("Cannot find session with id: " + id);
    }

    killSession(slot);
    tempFileSystems.invalidate(id);
  }

  private Session createExternalSession(ActiveSession other, URI externalUri) {
    Capabilities toUse = ImmutableCapabilities.copyOf(other.getCapabilities());

    // Rewrite the se:options if necessary
    Object rawSeleniumOptions = other.getCapabilities().getCapability("se:options");
    if (rawSeleniumOptions instanceof Map) {
      @SuppressWarnings("unchecked") Map<String, Object> original = (Map<String, Object>) rawSeleniumOptions;
      Map<String, Object> updated = new TreeMap<>(original);

      String cdpPath = String.format("/session/%s/se/cdp", other.getId());
      updated.put("cdp", rewrite(cdpPath));

      toUse = new PersistentCapabilities(toUse).setCapability("se:options", updated);
    }

    return new Session(other.getId(), externalUri, other.getStereotype(), toUse, Instant.now());
  }

  private URI rewrite(String path) {
    try {
      return new URI(
        gridUri.getScheme(),
        gridUri.getUserInfo(),
        gridUri.getHost(),
        gridUri.getPort(),
        path,
        null,
        null);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private void  killSession(SessionSlot slot) {
    currentSessions.invalidate(slot.getSession().getId());
    // Attempt to stop the session
    if (!slot.isAvailable()) {
      slot.stop();
    }
  }

  @Override
  public NodeStatus getStatus() {
    Set<Slot> slots = factories.stream()
      .map(slot -> {
        Optional<Session> session = Optional.empty();
        if (!slot.isAvailable()) {
          ActiveSession activeSession = slot.getSession();
          session = Optional.of(
            new Session(
              activeSession.getId(),
              activeSession.getUri(),
              slot.getStereotype(),
              activeSession.getCapabilities(),
              activeSession.getStartTime()));
        }

        return new Slot(
          new SlotId(getId(), slot.getId()),
          slot.getStereotype(),
          Instant.EPOCH,
          session);
      })
      .collect(toImmutableSet());

    return new NodeStatus(
      getId(),
      externalUri,
      maxSessionCount,
      slots,
      isDraining() ? DRAINING : UP);
  }

  @Override
  public HealthCheck getHealthCheck() {
    return healthCheck;
  }

  @Override
  public void drain() {
    bus.fire(new NodeDrainStarted(getId()));
    draining = true;
    int currentSessionCount = getCurrentSessionCount();
    if (currentSessionCount == 0) {
      LOG.info("Firing node drain complete message");
      bus.fire(new NodeDrainComplete(getId()));
    } else {
      pendingSessions.set(currentSessionCount);
    }
  }

  private Map<String, Object> toJson() {
    return ImmutableMap.of(
      "id", getId(),
      "uri", externalUri,
      "maxSessions", maxSessionCount,
      "draining", isDraining(),
      "capabilities", factories.stream()
        .map(SessionSlot::getStereotype)
        .collect(Collectors.toSet()));
  }

  public static SauceNode.Builder builder(
    Tracer tracer,
    EventBus bus,
    URI uri,
    URI gridUri,
    Secret registrationSecret) {
    return new SauceNode.Builder(tracer, bus, uri, gridUri, registrationSecret);
  }

  public static class Builder {

    private final Tracer tracer;
    private final EventBus bus;
    private final URI uri;
    private final URI gridUri;
    private final Secret registrationSecret;
    private final ImmutableList.Builder<SessionSlot> factories;
    private int maxCount = Runtime.getRuntime().availableProcessors() * 5;
    private Ticker ticker = Ticker.systemTicker();
    private Duration sessionTimeout = Duration.ofMinutes(5);
    private HealthCheck healthCheck;

    private Builder(
      Tracer tracer,
      EventBus bus,
      URI uri,
      URI gridUri,
      Secret registrationSecret) {
      this.tracer = Require.nonNull("Tracer", tracer);
      this.bus = Require.nonNull("Event bus", bus);
      this.uri = Require.nonNull("Remote node URI", uri);
      this.gridUri = Require.nonNull("Grid URI", gridUri);
      this.registrationSecret = Require.nonNull("Registration secret", registrationSecret);
      this.factories = ImmutableList.builder();
    }

    public SauceNode.Builder add(Capabilities stereotype, SessionFactory factory) {
      Require.nonNull("Capabilities", stereotype);
      Require.nonNull("Session factory", factory);

      factories.add(new SessionSlot(bus, stereotype, factory));

      return this;
    }

    public SauceNode.Builder maximumConcurrentSessions(int maxCount) {
      this.maxCount = Require.positive("Max session count", maxCount);
      return this;
    }

    public SauceNode.Builder sessionTimeout(Duration timeout) {
      sessionTimeout = timeout;
      return this;
    }

    public SauceNode build() {
      return new SauceNode(
        tracer,
        bus,
        uri,
        gridUri,
        healthCheck,
        maxCount,
        ticker,
        sessionTimeout,
        factories.build(),
        registrationSecret);
    }

  }

}
