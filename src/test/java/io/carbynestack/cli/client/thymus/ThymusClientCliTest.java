/*
 * Copyright (c) 2025 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.beust.jcommander.JCommander;
import io.carbynestack.cli.CsCliApplication;
import io.carbynestack.cli.CsClientCli;
import io.carbynestack.cli.TemporaryConfiguration;
import io.carbynestack.cli.client.thymus.command.config.GetPolicyThymusClientCliCommandConfig;
import io.carbynestack.cli.client.thymus.config.ThymusClientCliConfig;
import io.carbynestack.cli.exceptions.CsCliException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.carbynestack.httpclient.CsHttpClientException;
import io.carbynestack.thymus.client.*;
import io.vavr.concurrent.Future;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Set;
import org.apache.commons.text.StringEscapeUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.mockito.Mock;

public class ThymusClientCliTest {
  private static final ResourceBundle CS_CLI_MESSAGES =
      ResourceBundle.getBundle(CsCliApplication.CS_CLI_MESSAGE_BUNDLE);
  private static final ResourceBundle THYMUS_CLI_MESSAGES =
      ResourceBundle.getBundle(ThymusClientCli.THYMUS_MESSAGE_BUNDLE);

  private static final ThymusError.ThymusServiceError serviceError =
      new ThymusError.ThymusServiceError();

  static {
    serviceError.setEndpoint(
        ThymusEndpoint.Builder().withServiceUri(URI.create("http://test-endpoint")).build());
    serviceError.setResponseCode(500);
    serviceError.setMessage("Test error message");
  }

  @Mock private final ThymusVCClient client = mock(ThymusVCClient.class);

  @Rule public TemporaryConfiguration temporaryConfiguration = new TemporaryConfiguration();

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  private ThymusClientCli getCliWithArgs(boolean mockedClient, String... args) {
    ThymusClientCliConfig config = new ThymusClientCliConfig();
    JCommander.newBuilder().addObject(config).build().parse(args);
    return new ThymusClientCli(
        config, false, mockedClient ? Option.some(conf -> client) : Option.none());
  }

  @Test
  public void givenHelpOption_whenParsing_thenShowUsage() throws Exception {
    String helpOption = "--help";
    ThymusClientCli thymusClientCli = getCliWithArgs(true, helpOption);
    thymusClientCli.parse();
    Method getProgramName = CsClientCli.class.getDeclaredMethod("getProgramName");
    getProgramName.setAccessible(true);
    assertThat(
        systemOutRule.getLog(),
        CoreMatchers.containsString(
            String.format("Usage: %s", getProgramName.invoke(thymusClientCli))));
  }

  @Test
  public void givenMissingCommand_whenParsing_thenExceptionWithCorrectMessageIsThrown()
      throws CsCliRunnerException, CsCliLoginException {
    try {
      String optionOnly = "--debug";
      getCliWithArgs(true, optionOnly).parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      assertThat(
          e.getMessage(), CoreMatchers.startsWith(CS_CLI_MESSAGES.getString("error.no-command")));
    }
  }

  @Test
  public void givenMissingPolicyId_whenParsingGetPolicy_thenExceptionWithCorrectMessageIsThrown()
      throws CsCliRunnerException, CsCliLoginException {
    try {
      getCliWithArgs(true, GetPolicyThymusClientCliCommandConfig.COMMAND_NAME).parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      assertThat(
          e.getMessage(),
          CoreMatchers.startsWith(
              String.format(
                  "Main parameters are required (\"%s\")",
                  THYMUS_CLI_MESSAGES.getString("get-policy.parameter.policy-id-description"))));
    }
  }

  @Test
  public void givenCommunicationFails_whenGetPolicy_thenExceptionWithCorrectMessageIsThrown()
      throws CsCliLoginException, CsCliException {
    NamespacedName policyName = NamespacedName.fromString("test:policy");
    CsHttpClientException communicationError = new CsHttpClientException("Test exception");
    try {
      when(client.getPolicy(policyName)).thenReturn(Future.failed(communicationError));
      ThymusClientCli cli =
          getCliWithArgs(
              true, GetPolicyThymusClientCliCommandConfig.COMMAND_NAME, policyName.toString());
      cli.parse();
      Assert.fail("Expected Exception");
    } catch (CsCliRunnerException e) {
      assertThat(
          e.getMessage(),
          CoreMatchers.startsWith(
              StringEscapeUtils.unescapeJava(
                  MessageFormat.format(
                      THYMUS_CLI_MESSAGES.getString("get-policy.failure"),
                      communicationError.toString()))));
    }
  }

  @Test
  public void givenInvalidPolicyName_whenGetPolicy_thenExceptionWithCorrectMessageIsThrown()
      throws CsCliLoginException, CsCliRunnerException {
    String invalidPolicyName = "invalid_policy";
    try {
      ThymusClientCli cli =
          getCliWithArgs(
              false, GetPolicyThymusClientCliCommandConfig.COMMAND_NAME, invalidPolicyName);
      cli.parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      assertThat(
          e.getMessage(),
          CoreMatchers.startsWith(
              MessageFormat.format(
                  THYMUS_CLI_MESSAGES.getString("policy-id.conversion.exception"),
                  invalidPolicyName)));
    }
  }

  @Test
  public void
      givenClientReturnsThymusServiceError_whenGetPolicy_thenExceptionWithCorrectMessageIsThrown()
          throws CsCliLoginException, CsCliException {
    NamespacedName policyName = NamespacedName.fromString("test:policy");
    try {
      when(client.getPolicy(policyName)).thenReturn(Future.successful(Either.left(serviceError)));
      ThymusClientCli cli =
          getCliWithArgs(
              true, GetPolicyThymusClientCliCommandConfig.COMMAND_NAME, policyName.toString());
      cli.parse();
      Assert.fail("Expected Exception");
    } catch (CsCliRunnerException e) {
      assertThat(
          e.getMessage(),
          CoreMatchers.startsWith(
              MessageFormat.format(
                  THYMUS_CLI_MESSAGES.getString("get-policy.failure"), serviceError.toString())));
    }
  }

  @Test
  public void givenClientReturnsPolicy_whenGetPolicy_thenPrintPolicySource()
      throws CsCliLoginException, CsCliException, CsCliRunnerException {
    NamespacedName policyName = NamespacedName.fromString("test:policy");
    Policy policy = new Policy(policyName, "test source");
    when(client.getPolicy(policyName)).thenReturn(Future.successful(Either.right(policy)));
    ThymusClientCli cli =
        getCliWithArgs(
            true, GetPolicyThymusClientCliCommandConfig.COMMAND_NAME, policyName.toString());
    cli.parse();
    assertThat(systemOutRule.getLog(), CoreMatchers.containsString(policy.getSource()));
  }

  @Test
  public void givenCommunicationFails_whenListPolicies_thenExceptionWithCorrectMessageIsThrown()
      throws CsCliLoginException, CsCliException {
    CsHttpClientException communicationError = new CsHttpClientException("Test exception");
    try {
      when(client.getPolicies()).thenReturn(Future.failed(communicationError));
      ThymusClientCli cli = getCliWithArgs(true, "list-policies");
      cli.parse();
      Assert.fail("Expected Exception");
    } catch (CsCliRunnerException e) {
      assertThat(
          e.getMessage(),
          CoreMatchers.startsWith(
              StringEscapeUtils.unescapeJava(
                  MessageFormat.format(
                      THYMUS_CLI_MESSAGES.getString("list-policies.failure"),
                      communicationError.toString()))));
    }
  }

  @Test
  public void
      givenClientReturnsThymusServiceError_whenListPolicies_thenExceptionWithCorrectMessageIsThrown()
          throws CsCliLoginException, CsCliException {
    try {
      when(client.getPolicies()).thenReturn(Future.successful(Either.left(serviceError)));
      ThymusClientCli cli = getCliWithArgs(true, "list-policies");
      cli.parse();
      Assert.fail("Expected Exception");
    } catch (CsCliRunnerException e) {
      assertThat(
          e.getMessage(),
          CoreMatchers.startsWith(
              MessageFormat.format(
                  THYMUS_CLI_MESSAGES.getString("list-policies.failure"),
                  serviceError.toString())));
    }
  }

  @Test
  public void givenClientReturnsPolicies_whenListPolicies_thenPrintPolicyNames()
      throws CsCliLoginException, CsCliException, CsCliRunnerException {
    NamespacedName policyName1 = NamespacedName.fromString("test:policy1");
    NamespacedName policyName2 = NamespacedName.fromString("test:policy2");
    when(client.getPolicies())
        .thenReturn(Future.successful(Either.right(Set.of(policyName1, policyName2))));
    ThymusClientCli cli = getCliWithArgs(true, "list-policies");
    cli.parse();
    assertThat(
        systemOutRule.getLog(),
        CoreMatchers.allOf(
            CoreMatchers.containsString(policyName1.toString()),
            CoreMatchers.containsString(policyName2.toString())));
  }
}
