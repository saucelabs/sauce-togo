package com.saucelabs.grid;

import org.openqa.selenium.grid.Main;

public class SauceMain {
  public static void main(String[] args) {
    Main.main(
      new String[]{
        "--ext",
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

