/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.config;

import static io.carbynestack.cli.CsCliApplication.*;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.carbynestack.cli.converter.PathTypeConverter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import lombok.Getter;

@Parameters(resourceBundle = CS_CLI_MESSAGE_BUNDLE)
public class CsCliConfig {

  public static final String APPLICATION_TITLE = "cs";

  private static final ResourceBundle messages = ResourceBundle.getBundle(CS_CLI_MESSAGE_BUNDLE);

  @Parameter(names = "--help", descriptionKey = "option.help-description", help = true)
  private boolean help;

  @Parameter(names = "--debug", descriptionKey = "option.debug-description", help = true)
  private boolean debug = false;

  @Parameter(
      names = {"--config-file"},
      converter = PathTypeConverter.class,
      descriptionKey = "option.config-file")
  private Path configFilePath;

  @Getter
  @Parameter(
      names = {"--access-token-file"},
      converter = PathTypeConverter.class,
      descriptionKey = "option.access-token-file")
  private Path accessTokenFilePath;

  public boolean isHelp() {
    return help;
  }

  public boolean isDebug() {
    return debug;
  }

  public Path getConfigPath() throws IOException {
    if (configFilePath != null) {
      try {
        if (!configFilePath.isAbsolute()) {
          configFilePath = Paths.get(System.getProperty("user.dir")).resolve(configFilePath);
        }
      } catch (Exception e) {
        throw new IOException(
            messages.getString("read-config.failure.custom-config-cannot-be-read"), e);
      }
    }
    return configFilePath;
  }
}
