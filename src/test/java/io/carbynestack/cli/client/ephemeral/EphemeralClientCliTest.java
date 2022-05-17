/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.ephemeral;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.beust.jcommander.JCommander;
import com.google.common.collect.Lists;
import io.carbynestack.cli.CsCliApplication;
import io.carbynestack.cli.CsClientCli;
import io.carbynestack.cli.TemporaryConfiguration;
import io.carbynestack.cli.client.ephemeral.command.config.ExecuteEphemeralClientCliCommandConfig;
import io.carbynestack.cli.client.ephemeral.config.EphemeralClientCliCommandConfig;
import io.carbynestack.cli.client.ephemeral.config.EphemeralClientCliConfig;
import io.carbynestack.cli.exceptions.CsCliException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.carbynestack.ephemeral.client.ActivationError;
import io.carbynestack.ephemeral.client.ActivationResult;
import io.carbynestack.ephemeral.client.EphemeralMultiClient;
import io.vavr.collection.Stream;
import io.vavr.concurrent.Future;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.input.ReaderInputStream;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class EphemeralClientCliTest {

  private static final ResourceBundle CS_CLI_MESSAGES =
      ResourceBundle.getBundle(CsCliApplication.CS_CLI_MESSAGE_BUNDLE);
  private static final ResourceBundle EPHEMERAL_CLI_MESSAGES =
      ResourceBundle.getBundle(EphemeralClientCli.EPHEMERAL_MESSAGE_BUNDLE);

  @Mock private final EphemeralMultiClient client = mock(EphemeralMultiClient.class);

  @Rule public TemporaryConfiguration temporaryConfiguration = new TemporaryConfiguration();

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  private EphemeralClientCli getCliWithArgs(boolean mockedEphemeralClient, String... args) {
    EphemeralClientCliConfig config = new EphemeralClientCliConfig();
    JCommander.newBuilder().addObject(config).build().parse(args);
    return new EphemeralClientCli(
        config, false, mockedEphemeralClient ? Option.some(conf -> client) : Option.none());
  }

  private EphemeralClientCli getCliWithArgs(String... args) {
    return getCliWithArgs(false, args);
  }

  @Test
  public void givenHelpOption_whenParsing_thenShowUsage() throws Exception {
    String helpOption = "--help";
    EphemeralClientCli ephemeralClientCli = getCliWithArgs(helpOption);
    ephemeralClientCli.parse();
    Method getProgramName = CsClientCli.class.getDeclaredMethod("getProgramName");
    getProgramName.setAccessible(true);
    Assert.assertThat(
        systemOutRule.getLog(),
        CoreMatchers.containsString(
            String.format("Usage: %s", getProgramName.invoke(ephemeralClientCli))));
  }

  @Test
  public void givenMissingCommand_whenParsing_thenExceptionWithCorrectMessageIsThrown()
      throws CsCliRunnerException, CsCliLoginException {
    try {
      String optionOnly = "--debug";
      getCliWithArgs(optionOnly).parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      Assert.assertThat(
          e.getMessage(), CoreMatchers.startsWith(CS_CLI_MESSAGES.getString("error.no-command")));
    }
  }

  @Test
  public void givenMissingApplicationOption_whenParsing_thenExceptionWithCorrectMessageIsThrown()
      throws CsCliRunnerException, CsCliLoginException {
    try {
      getCliWithArgs(ExecuteEphemeralClientCliCommandConfig.COMMAND_NAME).parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      Assert.assertThat(
          e.getMessage(),
          CoreMatchers.startsWith("Main parameters are required (\"APPLICATION_NAME\")"));
    }
  }

  @Test
  public void givenInputOptionWithInvalidUUID_whenParsing_thenExceptionWithCorrectMessageIsThrown()
      throws CsCliRunnerException, CsCliLoginException {
    String invalidUUID = "!" + UUID.randomUUID().toString().substring(1);
    try {
      getCliWithArgs(
              ExecuteEphemeralClientCliCommandConfig.COMMAND_NAME,
              "-i",
              invalidUUID,
              "app.example.com")
          .parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      Assert.assertThat(
          e.getMessage(),
          CoreMatchers.containsString(
              MessageFormat.format(
                  CS_CLI_MESSAGES.getString("uuid.conversion.exception"), invalidUUID)));
    }
  }

  @Test
  public void
      givenInputOptionWithWronglyConcatenatedUUIDs_whenParsing_thenExceptionWithCorrectMessageIsThrown()
          throws CsCliRunnerException, CsCliLoginException {
    String invalidInputParam =
        "3d908814-02a1-44f6-9e93-0d1d321eec3f:05c42ac1-7509-491e-93a4-11bc4bb28d64";
    try {
      getCliWithArgs(
              ExecuteEphemeralClientCliCommandConfig.COMMAND_NAME,
              "-i",
              invalidInputParam,
              "app.example.com")
          .parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException sce) {
      Assert.assertThat(
          sce.getMessage(),
          CoreMatchers.containsString(
              MessageFormat.format(
                  CS_CLI_MESSAGES.getString("uuid.conversion.exception"), invalidInputParam)));
    }
  }

  @Test
  public void
      givenBothTagFiltersAndInputsProvided_whenUsingEphemeralClient_thenExceptionWithCorrectMessageIsThrows()
          throws CsCliLoginException, CsCliException {
    String uuid = UUID.randomUUID().toString();
    String tagFilter = "key:value";

    try {
      getCliWithArgs(
              ExecuteEphemeralClientCliCommandConfig.COMMAND_NAME,
              "-i",
              uuid,
              "-f",
              tagFilter,
              "app.example.com")
          .parse();
      Assert.fail("Expected Exception");
    } catch (CsCliRunnerException e) {
      Assert.assertThat(
          e.getMessage(),
          CoreMatchers.containsString(
              EPHEMERAL_CLI_MESSAGES.getString("execute.failure.both-inputs-provided")));
    }
  }

  @Test
  public void givenUnreachableEphemeralUris_whenParsing_thenExceptionIsThrown()
      throws CsCliException, CsCliLoginException {
    System.setIn(new ReaderInputStream(new StringReader("a = sint(1)"), StandardCharsets.UTF_8));
    try {
      getCliWithArgs(ExecuteEphemeralClientCliCommandConfig.COMMAND_NAME, "app.example.com")
          .parse();
      Assert.fail("Expected Exception");
    } catch (CsCliRunnerException scre) {
      Assert.assertThat(
          scre.getMessage(),
          CoreMatchers.startsWith(EPHEMERAL_CLI_MESSAGES.getString("execute.failure.invoke")));
    }
  }

  @Test
  public void givenAllRequiredArguments_whenParsing_thenCallClientWithCorrectArguments()
      throws Exception {
    String code = "a = sint(1)";
    String application = "app.svc.example.com";
    List<UUID> inputs = Stream.range(0, 3).map((i) -> UUID.randomUUID()).asJava();
    UUID outputId = UUID.randomUUID();
    List<ActivationResult> results =
        Lists.newArrayList(
            new ActivationResult(Collections.singletonList(outputId)),
            new ActivationResult(Collections.singletonList(outputId)));
    when(client.execute(eq(code), eq(inputs))).thenReturn(Future.successful(Either.right(results)));
    System.setIn(new ReaderInputStream(new StringReader(code), StandardCharsets.UTF_8));
    getCliWithArgs(
            true,
            ExecuteEphemeralClientCliCommandConfig.COMMAND_NAME,
            "-i",
            inputs.stream().map(UUID::toString).collect(Collectors.joining(",")),
            application)
        .parse();
    verify(client).execute(eq(code), eq(inputs));
    Assert.assertThat(
        systemOutRule.getLog(),
        CoreMatchers.containsString(Arrays.toString(results.get(0).getResponse().toArray())));
  }

  @Test
  public void
      givenAllRequiredArgumentsWithTagFilter_whenParsing_thenCallTheClientWithCorrectArguments()
          throws Exception {
    String code = "a = sint(1)";
    String application = "app.svc.example.com";
    String tagFilter = "key:value";
    List<String> tagFilters = Collections.singletonList(tagFilter);

    UUID outputId = UUID.randomUUID();
    List<ActivationResult> results =
        Lists.newArrayList(
            new ActivationResult(Collections.singletonList(outputId)),
            new ActivationResult(Collections.singletonList(outputId)));
    when(client.executeWithTags(eq(code), eq(tagFilters)))
        .thenReturn(Future.successful(Either.right(results)));
    System.setIn(new ReaderInputStream(new StringReader(code), StandardCharsets.UTF_8));
    getCliWithArgs(
            true, ExecuteEphemeralClientCliCommandConfig.COMMAND_NAME, "-f", tagFilter, application)
        .parse();
    verify(client).executeWithTags(eq(code), eq(tagFilters));
    Assert.assertThat(
        systemOutRule.getLog(),
        CoreMatchers.containsString(Arrays.toString(results.get(0).getResponse().toArray())));
  }

  @Test
  public void givenPlayersReturnDifferentResults_whenInvokingFunction_thenThrowException() {
    String code = "a = sint(1)";
    String application = "app.svc.example.com";
    List<UUID> inputs = Stream.range(0, 3).map((i) -> UUID.randomUUID()).asJava();
    UUID outputId1 = UUID.randomUUID();
    UUID outputId2 = UUID.randomUUID();
    UUID outputId3 = UUID.randomUUID();
    List<ActivationResult> result =
        Lists.newArrayList(
            new ActivationResult(Lists.newArrayList(outputId1, outputId2)),
            new ActivationResult(Lists.newArrayList(outputId1, outputId3)));
    when(client.execute(eq(code), eq(inputs))).thenReturn(Future.successful(Either.right(result)));
    System.setIn(new ReaderInputStream(new StringReader(code), StandardCharsets.UTF_8));
    try {
      getCliWithArgs(
              true,
              ExecuteEphemeralClientCliCommandConfig.COMMAND_NAME,
              "-i",
              inputs.stream().map(UUID::toString).collect(Collectors.joining(",")),
              application)
          .parse();
      Assert.fail("Exception expected");
    } catch (Exception e) {
      verify(client).execute(eq(code), eq(inputs));
      Assert.assertEquals(
          EPHEMERAL_CLI_MESSAGES.getString("execute.failure.invoke-different-results"),
          e.getMessage());
    }
  }

  @Test
  public void
      givenClientReturnsWith500StatusCode_whenParsing_thenExceptionWithCorrectMessageIsThrown()
          throws Exception {
    String code = "a = sint(1)";
    String application = "app.svc.example.com";
    List<UUID> inputs = Stream.range(0, 3).map((i) -> UUID.randomUUID()).asJava();
    int statusCode = 500;
    String msg = "some error";
    ActivationError err = new ActivationError();
    err.setResponseCode(statusCode);
    err.setMessage(msg);
    when(client.execute(eq(code), eq(inputs))).thenReturn(Future.successful(Either.left(err)));
    System.setIn(new ReaderInputStream(new StringReader(code), StandardCharsets.UTF_8));
    try {
      getCliWithArgs(
              true,
              ExecuteEphemeralClientCliCommandConfig.COMMAND_NAME,
              "-i",
              inputs.stream().map(UUID::toString).collect(Collectors.joining(",")),
              application)
          .parse();
      Assert.fail("Expected Exception");
    } catch (CsCliRunnerException scre) {
      Assert.assertThat(
          scre.getMessage(),
          CoreMatchers.startsWith(
              MessageFormat.format(
                  EPHEMERAL_CLI_MESSAGES.getString("execute.failure.invoke-status-code"),
                  statusCode,
                  msg)));
    }
  }

  @Test
  public void givenNoTimeoutOptionDefined_whenUsingEphemeralClient_thenUse10SecondsAsDefault()
      throws Exception {
    String code = "a = sint(1)";
    EphemeralClientCliConfig config = new EphemeralClientCliConfig();
    JCommander.newBuilder()
        .addObject(config)
        .build()
        .parse(ExecuteEphemeralClientCliCommandConfig.COMMAND_NAME, "DUMMY_APP_NAME");
    EphemeralClientFactory mockedClientFactory = mock(EphemeralClientFactory.class);
    when(mockedClientFactory.create(any())).thenReturn(client);
    when(client.execute(any(), anyList()))
        .thenReturn(
            Future.successful(
                Either.right(
                    Collections.singletonList(
                        new ActivationResult(Collections.singletonList(UUID.randomUUID()))))));
    System.setIn(new ReaderInputStream(new StringReader(code), StandardCharsets.UTF_8));
    new EphemeralClientCli(config, false, Option.some(mockedClientFactory)).parse();
    ArgumentCaptor<EphemeralClientCliCommandConfig> configCaptor =
        ArgumentCaptor.forClass(EphemeralClientCliCommandConfig.class);
    verify(mockedClientFactory, times(1)).create(configCaptor.capture());
    Assert.assertEquals(10, configCaptor.getValue().getTimeout());
  }

  @Test
  public void givenTimeoutOptionSpecified_whenUsingEphemeralClient_thenUseGivenTimeout()
      throws Exception {
    String code = "a = sint(1)";
    int timeout = new Random().nextInt(100) + 1;
    EphemeralClientCliConfig config = new EphemeralClientCliConfig();
    JCommander.newBuilder()
        .addObject(config)
        .build()
        .parse(
            ExecuteEphemeralClientCliCommandConfig.COMMAND_NAME,
            "-t",
            Integer.toString(timeout),
            "DUMMY_APP_NAME");
    EphemeralClientFactory mockedClientFactory = mock(EphemeralClientFactory.class);
    when(mockedClientFactory.create(any())).thenReturn(client);
    when(client.execute(any(), anyList()))
        .thenReturn(
            Future.successful(
                Either.right(
                    Collections.singletonList(
                        new ActivationResult(Collections.singletonList(UUID.randomUUID()))))));
    System.setIn(new ReaderInputStream(new StringReader(code), StandardCharsets.UTF_8));
    new EphemeralClientCli(config, false, Option.some(mockedClientFactory)).parse();
    ArgumentCaptor<EphemeralClientCliCommandConfig> configCaptor =
        ArgumentCaptor.forClass(EphemeralClientCliCommandConfig.class);
    verify(mockedClientFactory, times(1)).create(configCaptor.capture());
    Assert.assertEquals(timeout, configCaptor.getValue().getTimeout());
  }
}
