package com.saucelabs.grid;

public class Main {
  public static void main(String[] args) {
    org.openqa.selenium.grid.Main.main(
      new String[]{
        "--ext",
        // TODO Use resource loader
        "/Users/diegomolina/Projects/github.com/saucelabs/sauce-togo/sauce-grid/target/sauce-grid-1.0-SNAPSHOT.jar",
        "standalone",
        "--relax-checks",
        "true",
        "--detect-drivers",
        "false",
        "--config",
        "/Users/diegomolina/Projects/github.com/seleniumhq/config_host_sauce.toml"
      }
    );
  }
}

