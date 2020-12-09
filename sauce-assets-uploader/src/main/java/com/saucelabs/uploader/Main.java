package com.saucelabs.uploader;

import org.apache.http.HttpEntity;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main {

  private static final Logger LOG = Logger.getLogger(Main.class.getName());

  private static final String ASSETS_PATH =
    System.getenv().getOrDefault("ASSETS_PATH", "/opt/selenium/assets");
  private static final String SAUCE_API_HOST =
    System.getenv().getOrDefault("SAUCE_API_HOST", "api.us-west-1.saucelabs.com");
  private static final String SAUCE_JOB_ID = System.getenv("SAUCE_JOB_ID");

  private static final String SAUCE_API_URL =
    System.getenv()
      .getOrDefault(
        "SAUCE_API_URL",
        String.format("https://%s/v1/testrunner/jobs/%s/assets", SAUCE_API_HOST, SAUCE_JOB_ID));

  private static final String LOCAL_SESSION_ID = System.getenv("LOCAL_SESSION_ID");
  private static final String SAUCE_USER_NAME = System.getenv("SAUCE_USERNAME");
  private static final String SAUCE_ACCESS_KEY = System.getenv("SAUCE_ACCESS_KEY");

  public static void main(String[] args) {
    String sessionAssetsPath = ASSETS_PATH + File.separator + LOCAL_SESSION_ID;
    try {
      Files.list(Paths.get(sessionAssetsPath))
        .filter(path -> path.toFile().isFile())
        .forEach(path -> uploadFile(path.toFile(), getFileContentType(path.toFile().getName())));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void uploadFile(File fileToUpload, ContentType fileContentType) {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      LOG.info(String.format("Uploading %s to %s", fileToUpload.getAbsolutePath(), SAUCE_API_URL));
      HttpPut httpPut = new HttpPut(SAUCE_API_URL);
      HttpEntity httpEntity = MultipartEntityBuilder
        .create()
        .addBinaryBody("file[]", fileToUpload, fileContentType, fileToUpload.getName())
        .build();
      httpPut.setEntity(httpEntity);
      UsernamePasswordCredentials credentials =
        new UsernamePasswordCredentials(SAUCE_USER_NAME, SAUCE_ACCESS_KEY);
      httpPut.setHeader(new BasicScheme().authenticate(credentials, httpPut, null));
      try (CloseableHttpResponse response = client.execute(httpPut)) {
        System.out.println(response.getStatusLine().getStatusCode());
        String collect = new BufferedReader(
          new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))
          .lines()
          .collect(Collectors.joining("\n"));
        System.out.println(collect);
      }
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Issue while uploading " + fileToUpload.getAbsolutePath(), e);
    }
  }

  private static ContentType getFileContentType(String fileName) {
    if (fileName.endsWith(".json")) {
      return ContentType.create("application/json");
    }
    if (fileName.endsWith(".png")) {
      return ContentType.create("image/png");
    }
    if (fileName.endsWith(".mp4")) {
      return ContentType.create("video/mp4");
    }
    return ContentType.create("text/plain");
  }
}