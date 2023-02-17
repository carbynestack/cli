/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.configuration;

import static io.carbynestack.cli.configuration.ConfigurationCommand.CONFIGURATION_MESSAGE_BUNDLE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.fail;
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.carbynestack.cli.LoggingRule;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Random;
import java.util.ResourceBundle;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.junit.rules.TemporaryFolder;

public class ConfigurationCommandTest {
  private static final ResourceBundle MESSAGES =
      ResourceBundle.getBundle(CONFIGURATION_MESSAGE_BUNDLE);

  @Rule public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  @Rule public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

  @Rule public final LoggingRule loggingRule = new LoggingRule();

  private File configFile;

  private ConfigurationCommand configurationCommand = new ConfigurationCommand();

  @Before
  public void prepareCleanTest() throws IOException {
    configFile = temporaryFolder.newFile();
    Configuration.setConfigFilePath(configFile.toPath());
  }

  @Test
  public void givenUserDiscards_whenFinishConfiguration_thenDontWriteConfig()
      throws CsCliConfigurationException, IOException {
    systemInMock.provideLines("n");
    configurationCommand.finalizeConfiguration(new Configuration());
    Assert.assertEquals("", FileUtils.readFileToString(configFile, StandardCharsets.UTF_8));
  }

  @Test
  public void givenUserConfirms_whenFinishConfiguration_thenWriteConfig()
      throws CsCliConfigurationException, IOException, InterruptedException {
    Configuration configuration = ConfigurationUtil.getConfiguration();
    configuration.setPrime(BigInteger.valueOf(new Random().nextLong()));
    configuration.setNoSslValidation(true);
    systemInMock.provideLines("y");
    configurationCommand.finalizeConfiguration(configuration);
    Assert.assertEquals(
        new ObjectMapper().writer(new DefaultPrettyPrinter()).writeValueAsString(configuration),
        FileUtils.readFileToString(configFile, StandardCharsets.UTF_8));
  }

  @Test
  public void givenRepeatedInvalidInput_whenFinishConfiguration_thenThrowException()
      throws CsCliConfigurationException, IOException {
    String invalidInput = "invalid";
    Configuration configuration = ConfigurationUtil.getConfiguration();
    configuration.setPrime(BigInteger.valueOf(new Random().nextLong()));
    configuration.setNoSslValidation(true);
    long lastModified = configFile.lastModified();
    systemInMock.provideLines(invalidInput, invalidInput, invalidInput, invalidInput);
    try {
      configurationCommand.finalizeConfiguration(configuration);
      fail("Exception expected");
    } catch (Exception e) {
      Assert.assertEquals(lastModified, configFile.lastModified());
      Assert.assertThat(
          systemOutRule.getLog(),
          containsString(
              MessageFormat.format(
                  MESSAGES.getString("configuration.finalize.invalid-input.request-complete"),
                  "yes")));
      Assert.assertThat(
          e.getMessage(),
          containsString(
              MessageFormat.format(
                  MESSAGES.getString(
                      "configuration.finalize.failed.invalid-input-request-complete"),
                  invalidInput)));
    }
  }

  @Test
  public void givenConfigurationFilePathUndefined_whenPerformingConfiguration_logDebugMessage() {
    try {
      configurationCommand.configure();
      fail("Exception expected");
    } catch (Exception e) {
      Assert.assertThat(
          loggingRule.getLog(), containsString(MESSAGES.getString("read-config.log.fallback")));
    }
  }
}
