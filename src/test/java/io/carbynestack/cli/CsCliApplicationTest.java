/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli;

import static io.carbynestack.cli.config.CsCliConfig.APPLICATION_TITLE;
import static io.carbynestack.cli.configuration.ConfigurationCommand.COMMAND_NAME;
import static io.carbynestack.cli.configuration.ConfigurationCommand.CONFIGURATION_MESSAGE_BUNDLE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;
import static org.mockito.Mockito.*;

import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.ParameterException;
import io.carbynestack.cli.client.amphora.AmphoraClientCli;
import io.carbynestack.cli.client.amphora.command.config.GetSecretAmphoraClientCliCommandConfig;
import io.carbynestack.cli.client.amphora.config.AmphoraClientCliConfig;
import io.carbynestack.cli.client.castor.command.config.GetCastorTelemetryCliCommandConfig;
import io.carbynestack.cli.client.castor.config.CastorClientCliConfig;
import io.carbynestack.cli.configuration.ConfigurationCommand;
import io.carbynestack.cli.configuration.ConfigurationCommandFactory;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.vavr.control.Option;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.UUID;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsCliApplicationTest {

  @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

  @Rule public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

  @Rule public TemporaryConfiguration temporaryConfiguration = new TemporaryConfiguration();

  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  @Rule public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

  @Mock ConfigurationCommand configurationCommand = mock(ConfigurationCommand.class);

  @Before
  public void prepareCleanTest() {
    LogUtils.clearLogs();
  }

  private final ConfigurationCommandFactory configurationCommandFactory =
      () -> configurationCommand;

  private CsCliApplication getCsCliApplication(boolean mockedConfigurationCommand) {
    CsCliApplication csCliApplication =
        new CsCliApplication(
            mockedConfigurationCommand
                ? Option.some(configurationCommandFactory)
                : Option.some(ConfigurationCommand::new),
            Option.none());
    csCliApplication.addClient(new CsClient(new AmphoraClientCliConfig(), AmphoraClientCli.class));
    return csCliApplication;
  }

  @Test
  public void showHelpTest()
      throws CsCliRunnerException, CsCliException, CsCliConfigurationException,
          CsCliLoginException {
    getCsCliApplication(true).run("--help");
    assertThat(
        systemOutRule.getLog(),
        CoreMatchers.startsWith(String.format("Usage: %s", APPLICATION_TITLE)));
  }

  @Test
  public void setDebugTest()
      throws CsCliRunnerException, CsCliException, CsCliConfigurationException,
          CsCliLoginException {
    try {
      getCsCliApplication(true).run("--debug");
    } catch (ParameterException e) {
      // expected at this point
    }

    Assert.assertTrue(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).isDebugEnabled());
  }

  @Test
  public void setConfigFile()
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException,
          CsCliException {
    getCsCliApplication(true).run("configure");
    verify(configurationCommand, times(1)).configure();
  }

  @Test
  public void invalidCommandTest()
      throws CsCliRunnerException, CsCliException, CsCliConfigurationException,
          CsCliLoginException {
    String invalidCommandName = "invalidCommand";
    try {
      getCsCliApplication(true).run(invalidCommandName);
    } catch (MissingCommandException mce) {
      Assert.assertEquals(
          mce.getMessage(), String.format("Expected a command, got %s", invalidCommandName));
    }
  }

  @Test
  public void detailedConfigurationHelpTest()
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException,
          CsCliException {
    ResourceBundle configurationMessages = ResourceBundle.getBundle(CONFIGURATION_MESSAGE_BUNDLE);
    getCsCliApplication(false).run("configure", "--help");
    Assert.assertThat(
        systemOutRule.getLog(),
        containsString(
            StringEscapeUtils.unescapeJava(configurationMessages.getString("detailed-help"))));
  }

  @Test
  public void printsConfigurationInUseTest()
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException,
          CsCliException {
    getCsCliApplication(false)
        .run(
            "--config-file",
            temporaryConfiguration.getConfigFile().getAbsolutePath(),
            "configure",
            "show");
    Assert.assertThat(
        systemOutRule.getLog(),
        CoreMatchers.startsWith(temporaryConfiguration.getConfiguration().toPrettyString()));
  }

  @Test
  public void commandExecutionTest()
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    try {
      getCsCliApplication(true).run(AmphoraClientCliConfig.CLIENT_NAME, "some", "dummy", "args");
    } catch (CsCliException sce) {
      assertThat(
          sce.getHelpMessage(),
          CoreMatchers.startsWith(
              String.format(
                  "Usage: %s %s", APPLICATION_TITLE, AmphoraClientCliConfig.CLIENT_NAME)));
    }
  }

  @Test
  public void mainInvalidCommandTest() {
    exit.expectSystemExitWithStatus(1);
    exit.checkAssertionAfterwards(
        () ->
            assertThat(LogUtils.getLog("WARNINGS"), CoreMatchers.startsWith("No command defined")));
    exit.checkAssertionAfterwards(
        () ->
            assertThat(
                systemOutRule.getLog(),
                CoreMatchers.startsWith(String.format("Usage: %s", APPLICATION_TITLE))));
    getCsCliApplication(true).main("--config-file", "\\/\\some invalid %& Path!°/");
  }

  @Test
  public void mainUnexpectedErrorTest() {
    exit.expectSystemExitWithStatus(2);
    exit.checkAssertionAfterwards(
        () ->
            assertThat(
                LogUtils.getLog("STDERR"), CoreMatchers.startsWith("Unexpected error occurred")));
    exit.checkAssertionAfterwards(
        () ->
            assertThat(
                LogUtils.getLog("STDERR"),
                CoreMatchers.containsString(
                    String.format(
                        "Usage: %s %s", APPLICATION_TITLE, AmphoraClientCliConfig.CLIENT_NAME))));
    getCsCliApplication(true).main(AmphoraClientCliConfig.CLIENT_NAME);
  }

  @Test
  public void mainCommandExecutionErrorTest() {
    exit.expectSystemExitWithStatus(3);
    getCsCliApplication(true)
        .main(
            "--config-file",
            temporaryConfiguration.getConfigFile().getAbsolutePath(),
            AmphoraClientCliConfig.CLIENT_NAME,
            GetSecretAmphoraClientCliCommandConfig.COMMAND_NAME,
            UUID.randomUUID().toString());
  }

  @Test
  public void mainInvalidConfigurationTest() {
    exit.expectSystemExitWithStatus(4);
    exit.checkAssertionAfterwards(
        () ->
            assertThat(
                LogUtils.getLog("STDERR"),
                CoreMatchers.containsString("Configuration file does not exist")));
    getCsCliApplication(true).main("--config-file", "/nonexistent.file", COMMAND_NAME);
  }

  @Test
  public void mainInvalidAccessTokensTest() throws IOException {
    File invalidAccessTokens = File.createTempFile("empty-access-tokens", ".json");
    exit.expectSystemExitWithStatus(5);
    exit.checkAssertionAfterwards(
        () ->
            assertThat(
                LogUtils.getLog("STDERR"),
                CoreMatchers.containsString("reading token store failed")));
    getCsCliApplication(true)
        .main(
            "--config-file",
            temporaryConfiguration.getConfigFile().getAbsolutePath(),
            "--access-token-file",
            invalidAccessTokens.getAbsolutePath(),
            CastorClientCliConfig.CLIENT_NAME,
            GetCastorTelemetryCliCommandConfig.COMMAND_NAME,
            "1");
  }

  public static CsCliApplication getCsCliApplication(
      Option<ConfigurationCommandFactory> configurationCommandFactory) {
    return new CsCliApplication(configurationCommandFactory, Option.none());
  }
}
