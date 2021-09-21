/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.configuration;

import static io.carbynestack.cli.configuration.ConfigurationCommand.CONFIGURATION_MESSAGE_BUNDLE;
import static org.junit.Assert.assertEquals;
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;

import io.carbynestack.amphora.common.AmphoraServiceUri;
import io.carbynestack.castor.common.CastorServiceUri;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;

public class VcpConfigurationTest {
  private static final ResourceBundle MESSAGES =
      ResourceBundle.getBundle(CONFIGURATION_MESSAGE_BUNDLE);

  @Rule public EnvironmentVariables environmentVariables = new EnvironmentVariables();
  @Rule public TextFromStandardInputStream systemInMock = emptyStandardInputStream();
  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  @Test
  public void givenEnvVariableForVcp1AmphoraService_whenGetAmphoraUrl_thenReturnEnvVariableValue()
      throws CsCliConfigurationException {
    String expectedAmphoraUrl =
        String.format("http://%s", RandomStringUtils.randomAlphanumeric(15));
    environmentVariables.set(
        MessageFormat.format(VcpConfiguration.AMPHORA_URL_ENV_KEY_FORMAT, 1), expectedAmphoraUrl);
    Configuration configuration = ConfigurationUtil.getConfiguration();
    assertEquals(
        new AmphoraServiceUri(expectedAmphoraUrl),
        configuration.getProvider(1).getAmphoraServiceUri());
  }

  @Test
  public void givenEnvVariableForVcp1CastorService_whenGetCastorUrl_thenReturnEnvVariableValue()
      throws CsCliConfigurationException {
    String expectedCasorUrl = String.format("http://%s", RandomStringUtils.randomAlphanumeric(15));
    environmentVariables.set(
        MessageFormat.format(VcpConfiguration.CASTOR_URL_ENV_KEY_FORMAT, 1), expectedCasorUrl);
    Configuration configuration = ConfigurationUtil.getConfiguration();
    assertEquals(
        new CastorServiceUri(expectedCasorUrl), configuration.getProvider(1).getCastorServiceUri());
  }

  @Test
  public void
      givenEnvVariableForVcp1EphemeralService_whenGetEphemeralUrl_thenReturnEnvVariableValue()
          throws CsCliConfigurationException {
    String expectedEphemeralUrl =
        String.format("http://%s", RandomStringUtils.randomAlphanumeric(15));
    environmentVariables.set(
        MessageFormat.format(VcpConfiguration.EPHEMERAL_URL_ENV_KEY_FORMAT, 1),
        expectedEphemeralUrl);
    Configuration configuration = ConfigurationUtil.getConfiguration();
    assertEquals(
        URI.create(expectedEphemeralUrl), configuration.getProvider(1).getEphemeralServiceUrl());
  }

  @Test
  public void
      givenNewVcpConfigurationAndBaseUrlNotProvided_whenPromptServiceUrls_thenNoDefaultProvided()
          throws CsCliConfigurationException {
    String expectedAmphoraUrl =
        String.format("http://%s", RandomStringUtils.randomAlphanumeric(15));
    String expectedCasorUrl = String.format("http://%s", RandomStringUtils.randomAlphanumeric(15));
    String expectedEphemeralUrl =
        String.format("http://%s", RandomStringUtils.randomAlphanumeric(15));
    String expectedOAuth2CliendId = UUID.randomUUID().toString();
    String expectedOAuth2CallbackUrl = "http://localhost/vcp-1";
    VcpConfiguration vcpConfiguration = new VcpConfiguration(1);
    systemInMock.provideLines(
        "",
        expectedAmphoraUrl,
        expectedCasorUrl,
        expectedEphemeralUrl,
        expectedOAuth2CliendId,
        expectedOAuth2CallbackUrl);
    vcpConfiguration.configure();
    Assert.assertNull(vcpConfiguration.baseUrl);
    Assert.assertThat(
        systemOutRule.getLog(),
        CoreMatchers.containsString(
            MessageFormat.format(
                MESSAGES.getString("configuration.request.vcp.amphora-service-url"), "")));
    Assert.assertThat(
        systemOutRule.getLog(),
        CoreMatchers.containsString(
            MessageFormat.format(
                MESSAGES.getString("configuration.request.vcp.castor-service-url"), "")));
    Assert.assertThat(
        systemOutRule.getLog(),
        CoreMatchers.containsString(
            MessageFormat.format(
                MESSAGES.getString("configuration.request.vcp.ephemeral-service-url"), "")));
    Assert.assertThat(
        systemOutRule.getLog(),
        CoreMatchers.containsString(
            MessageFormat.format(
                MESSAGES.getString("configuration.request.vcp.oauth2-client-id"), "")));
    Assert.assertThat(
        systemOutRule.getLog(),
        CoreMatchers.containsString(
            MessageFormat.format(
                MESSAGES.getString("configuration.request.vcp.oauth2-callback-url"), "")));
    assertEquals(
        new AmphoraServiceUri(expectedAmphoraUrl), vcpConfiguration.getAmphoraServiceUri());
    assertEquals(new CastorServiceUri(expectedCasorUrl), vcpConfiguration.getCastorServiceUri());
    assertEquals(URI.create(expectedEphemeralUrl), vcpConfiguration.getEphemeralServiceUrl());
    assertEquals(expectedOAuth2CliendId, vcpConfiguration.getOAuth2ClientId());
    assertEquals(URI.create(expectedOAuth2CallbackUrl), vcpConfiguration.getOAuth2CallbackUrl());
  }

  @Test
  public void
      givenExistingVcpConfigurationAndNewBaseUrlProvided_whenConfigure_thenProvideNewDefaults()
          throws CsCliConfigurationException {
    String expectedBaseUrl = String.format("http://%s/", RandomStringUtils.randomAlphanumeric(15));
    VcpConfiguration vcpConfiguration = ConfigurationUtil.getConfiguration().getProvider(1);
    systemInMock.provideLines(expectedBaseUrl, "", "", "");
    vcpConfiguration.configure();
    assertEquals(URI.create(expectedBaseUrl), vcpConfiguration.baseUrl);
    assertEquals(
        new AmphoraServiceUri(
            MessageFormat.format(VcpConfiguration.AMPHORA_URL_FORMAT, expectedBaseUrl)),
        vcpConfiguration.getAmphoraServiceUri());
    assertEquals(
        new CastorServiceUri(
            MessageFormat.format(VcpConfiguration.CASTOR_URL_FORMAT, expectedBaseUrl)),
        vcpConfiguration.getCastorServiceUri());
    assertEquals(
        URI.create(MessageFormat.format(VcpConfiguration.EPHEMERAL_URL_FORMAT, expectedBaseUrl)),
        vcpConfiguration.getEphemeralServiceUrl());
  }
}
