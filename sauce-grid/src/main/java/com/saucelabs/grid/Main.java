package com.saucelabs.grid;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
  public static void main(String[] args) {
    // Run "mvn clean package" before running this
    Path jarPath = Paths.get("sauce-grid/target/sauce-grid-1.0-SNAPSHOT.jar").toAbsolutePath();
    // Use the same "sauce_togo_config.toml" located under "test/resources" and add the following
    // two lines
    // Path where test assets will be stored (this path must exist, use ONLY for code debugging)
    // assets-path = "/absolute/path/to/your/assets/directory"
    Path debugToml =
      Paths
        .get("sauce-grid/src/test/resources/sauce_togo_config_debugging.toml")
        .toAbsolutePath();
    org.openqa.selenium.grid.Main.main(
      new String[]{
        "--ext",
        jarPath.toString(),
        "standalone",
        "--relax-checks",
        "true",
        "--detect-drivers",
        "false",
        "--config",
        debugToml.toString()
      }
    );
  }
}

