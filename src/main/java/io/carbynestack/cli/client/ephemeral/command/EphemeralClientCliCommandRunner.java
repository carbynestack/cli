/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.ephemeral.command;

import static io.carbynestack.cli.client.ephemeral.EphemeralClientCli.EPHEMERAL_MESSAGE_BUNDLE;

import com.google.common.collect.Maps;
import io.carbynestack.cli.CsClientCliCommandRunner;
import io.carbynestack.cli.client.ephemeral.config.EphemeralClientCliCommandConfig;
import io.carbynestack.cli.configuration.Configuration;
import io.carbynestack.cli.configuration.VcpConfiguration;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.carbynestack.cli.login.VcpToken;
import io.carbynestack.cli.login.VcpTokenStore;
import io.carbynestack.cli.util.KeyStoreUtil;
import io.carbynestack.ephemeral.client.EphemeralEndpoint;
import io.carbynestack.ephemeral.client.EphemeralMultiClient;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

abstract class EphemeralClientCliCommandRunner<T extends EphemeralClientCliCommandConfig>
    extends CsClientCliCommandRunner<T> {

  protected EphemeralMultiClient client;
  protected Configuration configuration;

  private EphemeralMultiClient createEphemeralClient(T config)
      throws CsCliRunnerException, CsCliLoginException {
    Option<Map<EphemeralEndpoint, VcpToken>> tokens =
        getVcpTokens(configuration, config.getApplication());
    return config
        .getCustomClientFactory()
        .map(factory -> Try.success(factory.create(config)))
        .getOrElse(
            Try.of(
                () -> {
                  List<EphemeralEndpoint> endpoints =
                      Arrays.stream(configuration.getProviders())
                          .map(VcpConfiguration::getEphemeralServiceUrl)
                          .map(url -> new EphemeralEndpoint(url, config.getApplication()))
                          .collect(Collectors.toList());
                  EphemeralMultiClient.Builder builder =
                      new EphemeralMultiClient.Builder()
                          .withEndpoints(endpoints)
                          .withSslCertificateValidation(!configuration.isNoSslValidation());
                  tokens.forEach(
                      t -> builder.withBearerTokenProvider(uri -> t.get(uri).getIdToken()));
                  KeyStoreUtil.tempKeyStoreForPems(configuration.getTrustedCertificates())
                      .peek(builder::withTrustedCertificate);
                  return builder.build();
                }))
        .getOrElseThrow(
            t ->
                new CsCliRunnerException(
                    getMessages().getString("client-instantiation-failed"), t));
  }

  public EphemeralClientCliCommandRunner(T config)
      throws CsCliRunnerException, CsCliConfigurationException {
    super(config);
    initializeMessageBundle(EPHEMERAL_MESSAGE_BUNDLE);
    this.configuration = Configuration.getInstance();
    try {
      client = createEphemeralClient(config);
    } catch (Exception e) {
      throw new CsCliRunnerException(getMessages().getString("client-instantiation-failed"), e);
    }
  }

  private Option<Map<EphemeralEndpoint, VcpToken>> getVcpTokens(
      Configuration configuration, String application) throws CsCliLoginException {
    if (!VcpTokenStore.exists()) {
      return Option.none();
    } else {
      VcpTokenStore vcpTokenStore =
          VcpTokenStore.load(true).getOrElseThrow(CsCliLoginException::new);
      return Option.some(
          Maps.uniqueIndex(
              vcpTokenStore.getTokens(),
              t -> getEphemeralEndpoint(t.getVcpBaseUrl(), configuration, application)));
    }
  }

  private EphemeralEndpoint getEphemeralEndpoint(
      String vcpBaseUrl, Configuration configuration, String application) {
    return Option.ofOptional(
            Arrays.stream(configuration.getProviders())
                .filter(c -> c.getBaseUrl().toString().equals(vcpBaseUrl))
                .findFirst()
                .map(
                    c ->
                        EphemeralEndpoint.Builder()
                            .withServiceUri(c.getEphemeralServiceUrl())
                            .withApplication(application)
                            .build()))
        .getOrElseThrow(
            () -> new IllegalStateException("mismatch between configuration and token store"));
  }
}
