package com.saucelabs.grid;

import static org.openqa.selenium.docker.ContainerConfig.image;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.PersistentCapabilities;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.UsernameAndPassword;
import org.openqa.selenium.docker.Container;
import org.openqa.selenium.docker.Docker;
import org.openqa.selenium.docker.Image;
import org.openqa.selenium.grid.docker.DockerAssetsPath;
import org.openqa.selenium.grid.node.ProtocolConvertingSession;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.Dialect;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.tracing.Tracer;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SauceDockerSession extends ProtocolConvertingSession {
  private static final Logger LOG = Logger.getLogger(SauceDockerSession.class.getName());
  private final Docker docker;
  private final Container container;
  private final Container videoContainer;
  private final AtomicInteger screenshotCount;
  private final AtomicBoolean screenshotsEnabled;
  private final DockerAssetsPath assetsPath;
  private final List<SauceCommandInfo> webDriverCommands;
  private final UsernameAndPassword usernameAndPassword;
  private final Image assetsUploaderImage;
  private final DataCenter dataCenter;
  private final AtomicBoolean tearDownTriggered;

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
    DockerAssetsPath assetsPath,
    UsernameAndPassword usernameAndPassword,
    DataCenter dataCenter,
    Image assetsUploaderImage,
    SauceCommandInfo firstCommand,
    Docker docker) {
    super(tracer, client, id, url, downstream, upstream, stereotype, capabilities, startTime);
    this.container = Require.nonNull("Container", container);
    this.videoContainer = videoContainer;
    this.assetsPath = Require.nonNull("Assets path", assetsPath);
    this.screenshotCount = new AtomicInteger(0);
    this.screenshotsEnabled = new AtomicBoolean(false);
    this.usernameAndPassword = Require.nonNull("Sauce user & key", usernameAndPassword);
    this.webDriverCommands = new ArrayList<>();
    this.webDriverCommands.add(firstCommand);
    this.assetsUploaderImage = assetsUploaderImage;
    this.dataCenter = dataCenter;
    this.docker = Require.nonNull("Docker", docker);
    this.tearDownTriggered = new AtomicBoolean(false);
  }

  public int increaseScreenshotCount() {
    return screenshotCount.getAndIncrement();
  }

  public boolean canTakeScreenshot() {
    return screenshotsEnabled.get();
  }

  public void enableScreenshots() {
    screenshotsEnabled.set(true);
  }

  public void addSauceCommandInfo(SauceCommandInfo commandInfo) {
    if (!webDriverCommands.isEmpty()) {
      // Get when the last command ended to calculate times between commands
      SauceCommandInfo lastCommand = webDriverCommands.get(webDriverCommands.size() - 1);
      long betweenCommands = commandInfo.getStartTime() - lastCommand.getEndTime();
      commandInfo.setBetweenCommands(betweenCommands);
      commandInfo.setVideoStartTime(lastCommand.getVideoStartTime());
    }
    this.webDriverCommands.add(commandInfo);
  }

  public DockerAssetsPath getAssetsPath() {
    return assetsPath;
  }

  @Override
  public void stop() {
    new Thread(() -> {
      if (!tearDownTriggered.getAndSet(true)) {
        if (videoContainer != null) {
          videoContainer.stop(Duration.ofSeconds(10));
        }
        saveLogs();
        container.stop(Duration.ofMinutes(1));
        integrateWithSauce();
      }
    }).start();
  }

  private void saveLogs() {
    List<String> logs = container.getLogs().getLogs();
    if (!logs.isEmpty()) {
      String sessionAssetsPath = assetsPath.getContainerPath(getId());
      String seleniumServerLog = String.format("%s/selenium-server.log", sessionAssetsPath);
      String logJson = String.format("%s/log.json", sessionAssetsPath);
      try {
        Files.write(Paths.get(seleniumServerLog), logs);
        Files.write(Paths.get(logJson), getProcessedWebDriverCommands());
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Error saving logs", e);
      }
    }
  }

  private void integrateWithSauce() {
    createSauceJob();
    String sauceJobId = getSauceJob();
    Container assetUploaderContainer = createAssetUploaderContainer(sauceJobId);
    assetUploaderContainer.start();
  }

  private byte[] getProcessedWebDriverCommands() {
    String webDriverCommands = new Json().toJson(this.webDriverCommands);
    webDriverCommands = webDriverCommands.replaceAll("\"null\"", "null");
    return webDriverCommands.getBytes();
  }

  private void createSauceJob() {
    String sauceOptions = "sauce:options";
    Capabilities toUse = ImmutableCapabilities.copyOf(getCapabilities());
    Object rawSauceOptions = getCapabilities().getCapability(sauceOptions);
    try {
      if (rawSauceOptions instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> original = (Map<String, Object>) rawSauceOptions;
        Map<String, Object> updated = new TreeMap<>(original);
        // Adding the local session id, so we can match it when retrieving the job from Sauce
        updated.put("diySessionId", getId());
        updated.put("accessKey", usernameAndPassword.password());
        toUse = new PersistentCapabilities(toUse).setCapability(sauceOptions, updated);
        URL sauceUrl = new URL(String.format("%s/wd/hub", dataCenter.onDemandUrl));
        new RemoteWebDriver(sauceUrl, toUse);
      }
    } catch (SessionNotCreatedException e) {
      LOG.log(Level.FINE, "Error creating session in Sauce Labs", e);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Error creating session in Sauce Labs", e);
    }
  }

  private String getSauceJob() {
    try {
      String apiUrl = String.format(
        dataCenter.apiUrl,
        usernameAndPassword.username(),
        usernameAndPassword.password());
      HttpClient client = HttpClient.Factory.createDefault().createClient(new URL(apiUrl));
      // TODO Might need to increase the JOB limit in case too many tests are run in parallel
      HttpRequest httpRequest = new HttpRequest(
        HttpMethod.GET,
        String.format("/rest/v1/%s/jobs?limit=10", usernameAndPassword.username()));
      HttpResponse httpResponse = client.execute(httpRequest);
      Json json = new Json();
      ArrayList<Map<String, Object>> jobs = json.toType(Contents.string(httpResponse), Json.LIST_OF_MAPS_TYPE);
      Optional<Map<String, Object>> firstMatch = jobs
        .stream()
        .filter(job -> String.valueOf(job).contains(getId().toString()))
        .findFirst();
      return firstMatch.map(stringObjectMap -> stringObjectMap.get("id").toString()).orElse(null);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Error getting job id from Sauce Labs", e);
    }
    return null;
  }

  private Container createAssetUploaderContainer(String sauceJobId) {
    String hostSessionAssetsPath = assetsPath.getHostPath(getId());
    Map<String, String> assetUploaderContainerEnvVars = getAssetUploaderContainerEnvVars(sauceJobId);
    Map<String, String> assetsPath =
      Collections.singletonMap(hostSessionAssetsPath, "/opt/selenium/assets");
    return docker.create(
      image(assetsUploaderImage)
        .env(assetUploaderContainerEnvVars)
        .bind(assetsPath));
  }

  private Map<String, String> getAssetUploaderContainerEnvVars(String sauceJobId) {
    Map<String, String> envVars = new HashMap<>();
    envVars.put("SAUCE_JOB_ID", sauceJobId);
    envVars.put("SAUCE_API_HOST", dataCenter.apiHost);
    envVars.put("SAUCE_USERNAME", usernameAndPassword.username());
    envVars.put("SAUCE_ACCESS_KEY", usernameAndPassword.password());
    return envVars;
  }
}
