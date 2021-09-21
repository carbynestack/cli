/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.configuration;

import static io.carbynestack.cli.configuration.ConfigurationCommand.COMMAND_NAME;
import static io.carbynestack.cli.configuration.ConfigurationCommand.CONFIGURATION_MESSAGE_BUNDLE;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Scanner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

@Parameters(
    resourceBundle = CONFIGURATION_MESSAGE_BUNDLE,
    commandDescriptionKey = "command-description",
    commandNames = {COMMAND_NAME})
@Slf4j
public class ConfigurationCommand {
  @Getter
  @Parameter(names = "--help", descriptionKey = "option.help-description", help = true)
  private boolean help;

  @Getter
  @Parameter(names = "show", descriptionKey = "option.show")
  private boolean show;

  public static final String CONFIGURATION_MESSAGE_BUNDLE = "ConfigurationMessageBundle";
  public static final String COMMAND_NAME = "configure";

  private static final ResourceBundle MESSAGES =
      ResourceBundle.getBundle(CONFIGURATION_MESSAGE_BUNDLE);

  private enum FinalizationStatus {
    SAVE,
    DISCARD
  }

  public void configure() throws CsCliConfigurationException {
    if (this.isHelp()) {
      System.out.println(StringEscapeUtils.unescapeJava(MESSAGES.getString("detailed-help")));
      return;
    }
    Configuration configuration = Configuration.loadFromFile();
    if (this.isShow()) {
      System.out.println(configuration.toPrettyString());
      return;
    }
    if (configuration == null) {
      configuration = new Configuration();
      log.debug(MESSAGES.getString("read-config.log.fallback"));
    }
    try {
      configuration.configure();
      finalizeConfiguration(configuration);
    } catch (CsCliConfigurationException scce) {
      log.error(
          String.format(
              "%s%s",
              scce.getMessage(),
              scce.getCause() != null ? String.format(": %s", scce.getCause()) : ""));
      throw scce;
    }
  }

  void finalizeConfiguration(Configuration configuration) throws CsCliConfigurationException {
    System.out.println(String.format("%n**********************************"));
    System.out.println("Configuration:");
    System.out.println(configuration.toPrettyString());
    System.out.println(String.format("%n**********************************"));
    switch (requestFinalizeStatus()) {
      case SAVE:
        log.debug("Writing configuration to file: {}", configuration);
        configuration.writeToFile();
        break;
      case DISCARD:
        log.debug("Discard configuration");
        break;
    }
  }

  private FinalizationStatus requestFinalizeStatus() throws CsCliConfigurationException {
    System.out.print(
        String.format(
            "%n%s",
            MessageFormat.format(
                MESSAGES.getString("configuration.finalize.request.complete"), "yes")));
    Scanner scanner = new Scanner(System.in);
    String in = "";
    for (int i = 0; i < 3; i++) {
      try {
        in = scanner.nextLine().toLowerCase();
        if (StringUtils.equalsAny(in, "", "yes", "y")) { // where "" is default
          return FinalizationStatus.SAVE;
        } else if (StringUtils.equalsAny(in, "no", "n")) {
          return FinalizationStatus.DISCARD;
        }
        System.out.print(
            MessageFormat.format(
                MESSAGES.getString("configuration.finalize.invalid-input.request-complete"),
                "yes"));
      } catch (Exception e) {
        log.error(MESSAGES.getString("read-input.failed"));
      }
    }
    throw new CsCliConfigurationException(
        MessageFormat.format(
            MESSAGES.getString("configuration.finalize.failed.invalid-input-request-complete"),
            in));
  }
}
