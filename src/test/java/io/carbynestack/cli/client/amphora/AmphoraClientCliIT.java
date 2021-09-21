/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.SECRET_SHARES_ENDPOINT;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.MetadataPage;
import io.carbynestack.cli.TemporaryConfiguration;
import io.carbynestack.cli.client.amphora.command.config.ListSecretsAmphoraClientCliCommandConfig;
import io.carbynestack.cli.client.amphora.config.AmphoraClientCliConfig;
import io.carbynestack.cli.client.amphora.util.SecretPrinter;
import io.carbynestack.cli.configuration.ConfigurationUtil;
import io.carbynestack.cli.exceptions.CsCliException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.carbynestack.httpclient.CsHttpClientException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;

public class AmphoraClientCliIT {

  protected static final String CERTIFICATE_A_PATH =
      Objects.requireNonNull(AmphoraClientCliIT.class.getClassLoader().getResource("certA.pem"))
          .getPath();
  protected static final String CERTIFICATE_B_PATH =
      Objects.requireNonNull(AmphoraClientCliIT.class.getClassLoader().getResource("certB.pem"))
          .getPath();
  protected static final String KEY_STORE_A_PATH =
      Objects.requireNonNull(AmphoraClientCliIT.class.getClassLoader().getResource("keyStoreA.jks"))
          .getPath();
  protected static final String KEY_STORE_A_PASSWORD = "verysecure";

  private final ObjectMapper mapper = new ObjectMapper();
  private final List<Metadata> secretMetadataList = AmphoraTestData.getMetadataList();
  private final MetadataPage secretMetadataPage = new MetadataPage(secretMetadataList, 1, 3, 3, 1);

  // Mock Server
  private final WireMockRule wireMockRuleA =
      new WireMockRule(
          wireMockConfig()
              .dynamicPort()
              .dynamicHttpsPort()
              .keystorePath(KEY_STORE_A_PATH)
              .keystorePassword(KEY_STORE_A_PASSWORD));

  private final WireMockRule wireMockRuleB =
      new WireMockRule(
          wireMockConfig()
              .dynamicPort()
              .dynamicHttpsPort()
              .keystorePath(KEY_STORE_A_PATH)
              .keystorePassword(KEY_STORE_A_PASSWORD));

  private final TemporaryConfiguration temporaryConfiguration =
      new TemporaryConfiguration(
          () ->
              ConfigurationUtil.getConfiguration(
                  String.format("https://localhost:%s", wireMockRuleA.httpsPort()),
                  String.format("https://localhost:%s", wireMockRuleB.httpsPort())));

  @Rule
  public RuleChain chain =
      RuleChain.outerRule(wireMockRuleA).around(wireMockRuleB).around(temporaryConfiguration);

  @Rule public ExpectedException expectedEx = ExpectedException.none();

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  @Before
  public void initializeMockRules() throws JsonProcessingException {
    configureAmphoraMockForRule(wireMockRuleA);
    configureAmphoraMockForRule(wireMockRuleB);
  }

  private void configureAmphoraMockForRule(WireMockRule wireMockRule)
      throws JsonProcessingException {
    wireMockRule.stubFor(
        get(urlMatching(SECRET_SHARES_ENDPOINT))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(mapper.writeValueAsString(secretMetadataPage))));
  }

  @Before
  public void prepareEnvVariables() {
    environmentVariables
        .set(
            "CS_VCP_1_AMPHORA_URL",
            String.format("https://localhost:%s", wireMockRuleA.httpsPort()))
        .set(
            "CS_VCP_2_AMPHORA_URL",
            String.format("https://localhost:%s", wireMockRuleB.httpsPort()));
  }

  private AmphoraClientCli getCliWithTempConfigAndArgs(String... args) {
    AmphoraClientCliConfig config = new AmphoraClientCliConfig();
    JCommander.newBuilder()
        .addObject(config)
        .build()
        .parse(
            ArrayUtils.addAll(
                new String[] {
                  "--config-file", temporaryConfiguration.getConfigFile().getAbsolutePath()
                },
                args));
    return new AmphoraClientCli(config, false);
  }

  @Test
  public void insecureConnectionTest() throws CsCliException, CsCliLoginException {
    try {
      getCliWithTempConfigAndArgs(ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME).parse();
      fail("Exception expected");
    } catch (CsCliRunnerException e) {
      assertThat(e.getMessage(), CoreMatchers.containsString("Fetching secret metadata failed"));
      assertThat(e.getCause().getCause(), CoreMatchers.instanceOf(CsHttpClientException.class));
      assertThat(
          e.getCause().getCause().getCause().getMessage(),
          CoreMatchers.containsString("unable to find valid certification path"));
    }
  }

  @Test
  public void disableSslValidationTest()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    temporaryConfiguration.applyConfigUpdate(
        configuration -> {
          configuration.setNoSslValidation(true);
          return null;
        });
    getCliWithTempConfigAndArgs(ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME).parse();
    Assert.assertThat(
        systemOutRule.getLog(),
        CoreMatchers.containsString(SecretPrinter.metadataListToString(secretMetadataList, false)));
  }

  @Test
  public void trustGivenCertificateTest()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    temporaryConfiguration.applyConfigUpdate(
        configuration -> {
          configuration.setTrustedCertificates(
              Collections.singletonList(Paths.get(CERTIFICATE_A_PATH)));
          return null;
        });
    getCliWithTempConfigAndArgs(ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME).parse();
    Assert.assertThat(
        systemOutRule.getLog(),
        CoreMatchers.containsString(SecretPrinter.metadataListToString(secretMetadataList, false)));
  }

  @Test
  public void wrongTrustedCertificateTest()
      throws CsCliException, IOException, CsCliLoginException {
    temporaryConfiguration.applyConfigUpdate(
        configuration -> {
          configuration.setTrustedCertificates(
              Collections.singletonList(Paths.get(CERTIFICATE_B_PATH)));
          return null;
        });
    try {
      getCliWithTempConfigAndArgs(ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME).parse();
      fail("Exception expected");
    } catch (CsCliRunnerException e) {
      assertThat(e.getMessage(), CoreMatchers.containsString("Fetching secret metadata failed"));
      assertThat(e.getCause().getCause(), CoreMatchers.instanceOf(CsHttpClientException.class));
      assertThat(
          e.getCause().getCause().getCause().getMessage(),
          CoreMatchers.containsString("unable to find valid certification path"));
    }
  }
}
