package com.saucelabs.grid;

import org.openqa.selenium.grid.config.Config;
import org.openqa.selenium.grid.log.LoggingOptions;
import org.openqa.selenium.grid.node.Node;
import org.openqa.selenium.grid.node.config.NodeOptions;
import org.openqa.selenium.grid.node.relay.RelayOptions;
import org.openqa.selenium.grid.security.SecretOptions;
import org.openqa.selenium.grid.server.BaseServerOptions;
import org.openqa.selenium.grid.server.EventBusOptions;
import org.openqa.selenium.grid.server.NetworkOptions;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.tracing.Tracer;

@SuppressWarnings("unused")
public class SauceNodeFactory {

  public static Node create(Config config) {
    LoggingOptions loggingOptions = new LoggingOptions(config);
    EventBusOptions eventOptions = new EventBusOptions(config);
    BaseServerOptions serverOptions = new BaseServerOptions(config);
    NodeOptions nodeOptions = new NodeOptions(config);
    NetworkOptions networkOptions = new NetworkOptions(config);
    SecretOptions secretOptions = new SecretOptions(config);

    Tracer tracer = loggingOptions.getTracer();
    HttpClient.Factory clientFactory = networkOptions.getHttpClientFactory(tracer);

    SauceDockerOptions sauceDockerOptions = new SauceDockerOptions(config);

    SauceNode.Builder builder = SauceNode.builder(
      tracer,
      eventOptions.getEventBus(),
      serverOptions.getExternalUri(),
      nodeOptions.getPublicGridUri().orElseGet(serverOptions::getExternalUri),
      secretOptions.getRegistrationSecret())
      .maximumConcurrentSessions(nodeOptions.getMaxSessions())
      .sessionTimeout(nodeOptions.getSessionTimeout())
      .heartbeatPeriod(nodeOptions.getHeartbeatPeriod());

    sauceDockerOptions.getDockerSessionFactories(tracer, clientFactory)
      .forEach((caps, factories) -> factories.forEach(factory -> builder.add(caps, factory)));

    if (config.getAll("relay", "configs").isPresent()) {
      new RelayOptions(config).getSessionFactories(tracer, clientFactory)
        .forEach((caps, factories) -> factories.forEach(factory -> builder.add(caps, factory)));
    }

    return builder.build();
  }
}
