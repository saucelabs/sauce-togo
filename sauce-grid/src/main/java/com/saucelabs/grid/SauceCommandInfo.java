package com.saucelabs.grid;

import com.google.common.collect.ImmutableMap;

import org.openqa.selenium.internal.Require;

import java.util.Map;

public class SauceCommandInfo {
  private final String[] suggestionValues = {};
  private final Integer screenshotId;
  private final long startTime;
  private final long endTime;
  private final Object request;
  private final Object result;
  private final String path;
  private final int httpStatus;
  private final String httpMethod;
  private final int statusCode;
  private long videoStartTime;
  private long betweenCommands;

  public SauceCommandInfo(Builder commandBuilder) {
    this.screenshotId = commandBuilder.screenshotId;
    this.startTime = commandBuilder.startTime;
    this.videoStartTime = commandBuilder.videoStartTime;
    this.endTime = commandBuilder.endTime;
    this.request = Require.nonNull("Request object", commandBuilder.request);
    this.result = Require.nonNull("Result object", commandBuilder.result);
    this.path = Require.nonNull("Url path", commandBuilder.path);
    this.httpStatus = commandBuilder.httpStatus;
    this.httpMethod = commandBuilder.httpMethod;
    this.statusCode = commandBuilder.statusCode;
  }

  public long getEndTime() {
    return endTime;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setBetweenCommands(long betweenCommands) {
    this.betweenCommands = betweenCommands;
  }

  public long getVideoStartTime() {
    return videoStartTime;
  }

  public void setVideoStartTime(long videoStartTime) {
    this.videoStartTime = videoStartTime;
  }

  private Map<String, Object> toJson() {
    long commandDuration = this.endTime - this.startTime;
    long inVideoTimeline = this.startTime + (commandDuration / 2) - this.videoStartTime;
    return ImmutableMap.<String, Object>builder()
      .put("screenshot", this.screenshotId == -1 ? "null" : this.screenshotId)
      .put("suggestion_values", this.suggestionValues)
      .put("start_time", this.startTime)
      .put("request", this.request)
      .put("result", this.result)
      .put("duration", commandDuration)
      .put("path", this.path)
      .put("hide_from_ui", false)
      .put("between_commands", this.betweenCommands)
      .put("visual_command", false)
      .put("HTTPStatus", this.httpStatus)
      .put("suggestion", "null")
      .put("in_video_timeline", Math.max(inVideoTimeline, 0))
      .put("method", this.httpMethod)
      .put("statusCode", this.statusCode)
      .build();
  }

  public static class Builder {
    private int screenshotId;
    private long startTime;
    private long videoStartTime;
    private long endTime;
    private Object request;
    private Object result;
    private String path;
    private int httpStatus;
    private String httpMethod;
    private int statusCode;

    public Builder setScreenshotId(int screenshotId) {
      this.screenshotId = screenshotId;
      return this;
    }

    public Builder setStartTime(long startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder setVideoStartTime(long videoStartTime) {
      this.videoStartTime = videoStartTime;
      return this;
    }

    public Builder setEndTime(long endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder setRequest(Object request) {
      this.request = request;
      return this;
    }

    public Builder setResult(Object result) {
      this.result = result;
      return this;
    }

    public Builder setPath(String path) {
      this.path = path;
      return this;
    }

    public Builder setHttpStatus(int httpStatus) {
      this.httpStatus = httpStatus;
      return this;
    }

    public Builder setHttpMethod(String httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    public Builder setStatusCode(int statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    public SauceCommandInfo build() {
      return new SauceCommandInfo(this);
    }
  }
}
