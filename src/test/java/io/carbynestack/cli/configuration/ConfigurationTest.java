/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.configuration;

import static io.carbynestack.cli.configuration.Configuration.*;
import static io.carbynestack.cli.configuration.ConfigurationCommand.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import io.carbynestack.cli.CsCliApplication;
import io.carbynestack.cli.CsCliApplicationTest;
import io.carbynestack.cli.LogUtils;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.vavr.control.Option;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.*;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

public class ConfigurationTest {
  private static final ResourceBundle MESSAGES =
      ResourceBundle.getBundle(CONFIGURATION_MESSAGE_BUNDLE);

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

  @Mock private final ConfigurationCommand configurationCommand = mock(ConfigurationCommand.class);

  private final ConfigurationCommandFactory configurationCommandFactory =
      () -> configurationCommand;

  private CsCliApplication csCliApplication;

  @Before
  public void prepareCleanTest() throws CsCliConfigurationException {
    LogUtils.clearLogs();
    doAnswer(invocation -> loadFromFile()).when(configurationCommand).configure();
    csCliApplication =
        CsCliApplicationTest.getCsCliApplication(Option.some(configurationCommandFactory));
  }

  @Test
  public void givenCustomConfigDefinedAbsolute_whenStartCli_thenSetConfigPath()
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException,
          CsCliException {
    String configFile = String.format("/%s", RandomStringUtils.random(10));
    csCliApplication.run("--config-file", configFile, "configure");
    Assert.assertEquals(Paths.get(configFile), configFilePath);
  }

  @Test
  public void givenCustomConfigDefinedRelative_whenStartCli_thenConvertToAbsoluteConfigPath()
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException,
          CsCliException {
    String configFile = String.format("%s", RandomStringUtils.random(10));
    csCliApplication.run("--config-file", configFile, "configure");
    Assert.assertEquals(Paths.get(System.getProperty("user.dir"), configFile), configFilePath);
  }

  @Test
  public void givenConfigIsInvalid_whenStartCli_thenLogError()
      throws IOException, CsCliRunnerException, CsCliConfigurationException, CsCliLoginException,
          CsCliException {
    File tempFile = temporaryFolder.newFile();
    csCliApplication.run("--config-file", tempFile.getAbsolutePath(), "configure");
    Assert.assertThat(
        LogUtils.getLog("STDERR"),
        containsString(MESSAGES.getString("read-config.failure.cannot-be-parsed")));
  }

  @Test
  public void givenConfigCannotBeRead_whenStartCli_thenLogError()
      throws IOException, CsCliRunnerException, CsCliConfigurationException, CsCliLoginException,
          CsCliException {
    File tempFile = temporaryFolder.newFile();
    //noinspection ResultOfMethodCallIgnored
    tempFile.setReadable(false);
    csCliApplication.run("--config-file", tempFile.getAbsolutePath(), "configure");
    Assert.assertThat(
        LogUtils.getLog("STDERR"),
        containsString(MESSAGES.getString("read-config.failure.cannot-be-read")));
  }

  @Test
  public void givenConfigDoesNotExist_whenStartCli_thenLogError()
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException,
          CsCliException {
    String configFile = String.format("/%s", RandomStringUtils.random(10));
    csCliApplication.run("--config-file", configFile, "configure");
    Assert.assertThat(
        LogUtils.getLog("STDERR"),
        containsString(MESSAGES.getString("read-config.log.does-not-exist")));
  }

  @Test
  public void givenNoCustomConfigDefined_whenStartCli_thenReadFromDefaultPath()
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException,
          CsCliException {
    setConfigFilePath(null);
    csCliApplication.run("configure");
    Assert.assertThat(LogUtils.getLog("STDERR"), containsString("Using config file"));
    Assert.assertThat(
        LogUtils.getLog("STDERR"),
        containsString(
            Paths.get(System.getProperty("user.home"), CONFIG_FOLDER_PATH, CONFIG_FILE_NAME)
                .toString()));
  }
}
