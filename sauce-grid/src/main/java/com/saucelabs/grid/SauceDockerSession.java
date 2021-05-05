package com.saucelabs.grid;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.saucelabs.grid.Common.JSON;
import static com.saucelabs.grid.Common.getSauceCapability;
import static java.time.Instant.ofEpochMilli;
import static org.openqa.selenium.docker.ContainerConfig.image;
import static org.openqa.selenium.json.Json.JSON_UTF_8;
import static org.openqa.selenium.remote.http.Contents.asJson;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.UsernameAndPassword;
import org.openqa.selenium.docker.Container;
import org.openqa.selenium.docker.Docker;
import org.openqa.selenium.docker.Image;
import org.openqa.selenium.grid.docker.DockerAssetsPath;
import org.openqa.selenium.grid.node.ProtocolConvertingSession;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.Dialect;
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
    String sessionAssetsPath = assetsPath.getContainerPath(getId());
    String seleniumServerLog = String.format("%s/selenium-server.log", sessionAssetsPath);
    String logJson = String.format("%s/log.json", sessionAssetsPath);
    try {
      List<String> logs = container.getLogs().getLogLines();
      Files.write(Paths.get(logJson), getProcessedWebDriverCommands());
      Files.write(Paths.get(seleniumServerLog), logs);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Error saving logs", e);
    }
  }

  private void integrateWithSauce() {
    String sauceJobId = createSauceJob();
    Container assetUploaderContainer = createAssetUploaderContainer(sauceJobId);
    assetUploaderContainer.start();
  }

  private byte[] getProcessedWebDriverCommands() {
    String webDriverCommands = JSON.toJson(this.webDriverCommands);
    webDriverCommands = webDriverCommands.replaceAll("\"null\"", "null");
    return webDriverCommands.getBytes();
  }

  private String createSauceJob() {
    MutableCapabilities jobInfo = new MutableCapabilities();
    jobInfo.setCapability(CapabilityType.BROWSER_NAME, getCapabilities().getBrowserName());
    jobInfo.setCapability(CapabilityType.PLATFORM_NAME, getCapabilities().getPlatformName());
    jobInfo.setCapability(CapabilityType.BROWSER_VERSION, getCapabilities().getBrowserVersion());
    jobInfo.setCapability("framework", "webdriver");
    getSauceCapability(getCapabilities(), "build")
      .ifPresent(build -> jobInfo.setCapability("build", build.toString()));
    getSauceCapability(getCapabilities(), "name")
      .ifPresent(name -> jobInfo.setCapability("name", name.toString()));
    getSauceCapability(getCapabilities(), "tags")
      .ifPresent(tags -> jobInfo.setCapability("tags", tags));
    SauceCommandInfo firstCommand = webDriverCommands.get(0);
    String startTime = ofEpochMilli(firstCommand.getStartTime()).toString();
    jobInfo.setCapability("startTime", startTime);
    SauceCommandInfo lastCommand = webDriverCommands.get(webDriverCommands.size() - 1);
    String endTime = ofEpochMilli(lastCommand.getStartTime()).toString();
    jobInfo.setCapability("endTime", endTime);
    // TODO: We cannot always know if the test passed or not
    jobInfo.setCapability("passed", true);

    String apiUrl = String.format(
      dataCenter.apiUrl,
      usernameAndPassword.username(),
      usernameAndPassword.password());
    String responseContents = "";
    try {
      HttpClient client = HttpClient.Factory.createDefault().createClient(new URL(apiUrl));
      HttpRequest request = new HttpRequest(HttpMethod.POST, "/v1/testcomposer/reports");
      request.setContent(asJson(jobInfo));
      request.setHeader(CONTENT_TYPE, JSON_UTF_8);
      HttpResponse response = client.execute(request);
      responseContents = Contents.string(response);
      Map<String, String> jobId = JSON.toType(responseContents, Map.class);
      return jobId.get("ID");
    } catch (Exception e) {
      LOG.log(Level.WARNING, String.format("Error creating job in Sauce Labs. %s", responseContents), e);
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
