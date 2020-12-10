package com.saucelabs.grid;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PersistentCapabilities;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.docker.Container;
import org.openqa.selenium.grid.docker.DockerAssetsPath;
import org.openqa.selenium.grid.node.ProtocolConvertingSession;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.Dialect;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.tracing.Tracer;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SauceDockerSession extends ProtocolConvertingSession {
  private static final Logger LOG = Logger.getLogger(SauceDockerSession.class.getName());
  private final Container container;
  private final Container videoContainer;
  private final AtomicInteger screenshotCount;
  private final DockerAssetsPath assetsPath;
  private final List<SauceCommandInfo> webDriverCommands;

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
    SauceCommandInfo firstCommand) {
    super(tracer, client, id, url, downstream, upstream, stereotype, capabilities, startTime);
    this.container = Require.nonNull("Container", container);
    this.videoContainer = videoContainer;
    this.assetsPath = Require.nonNull("Assets path", assetsPath);
    this.screenshotCount = new AtomicInteger(0);
    this.webDriverCommands = new ArrayList<>();
    this.webDriverCommands.add(firstCommand);
  }

  public int increaseScreenshotCount() {
    return screenshotCount.getAndIncrement();
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
    if (videoContainer != null) {
      videoContainer.stop(Duration.ofSeconds(10));
    }
    List<String> logs = container.getLogs().getLogs();
    if (!logs.isEmpty()) {
      String sessionAssetsPath = assetsPath.getContainerPath(getId());
      String seleniumServerLog = String.format("%s/selenium-server.log", sessionAssetsPath);
      String logJson = String.format("%s/log.json", sessionAssetsPath);
      try {
        Files.write(Paths.get(seleniumServerLog), logs);
        Files.write(Paths.get(logJson), new Json().toJson(webDriverCommands).getBytes());
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Error saving logs", e);
      }
    }
    container.stop(Duration.ofMinutes(1));
    if (!logs.isEmpty()) {
      try {
        String errorRemovalCapability = "devX";
        String sauceOptions = "sauce:options";
        Capabilities toUse = ImmutableCapabilities.copyOf(getCapabilities());
        Object rawSeleniumOptions = getCapabilities().getCapability(sauceOptions);
        if (rawSeleniumOptions instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> original = (Map<String, Object>) rawSeleniumOptions;
          Map<String, Object> updated = new TreeMap<>(original);
          if (!updated.containsKey(errorRemovalCapability)) {
            updated.put(errorRemovalCapability, true);
          }
          toUse = new PersistentCapabilities(toUse).setCapability(sauceOptions, updated);
        }
        URL sauceUrl = new URL("https://ondemand.us-west-1.saucelabs.com:443/wd/hub");
        new RemoteWebDriver(sauceUrl, new MutableCapabilities(toUse));
        // TODO Get session ID from Sauce Labs
      } catch (SessionNotCreatedException e) {
        LOG.log(Level.FINE, "Error creating session in Sauce Labs", e);
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Error creating session in Sauce Labs", e);
      }
    }
  }
}
