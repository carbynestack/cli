/*
 * Copyright (c) 2025 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus.command;

import static io.carbynestack.cli.client.thymus.ThymusClientCli.THYMUS_MESSAGE_BUNDLE;

import com.google.common.collect.Maps;
import io.carbynestack.cli.CsClientCliCommandRunner;
import io.carbynestack.cli.client.thymus.config.ThymusClientCliCommandConfig;
import io.carbynestack.cli.configuration.Configuration;
import io.carbynestack.cli.configuration.VcpConfiguration;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.carbynestack.cli.login.VcpToken;
import io.carbynestack.cli.login.VcpTokenStore;
import io.carbynestack.thymus.client.ThymusEndpoint;
import io.carbynestack.thymus.client.ThymusVCClient;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

abstract class ThymusClientCliCommandRunner<T extends ThymusClientCliCommandConfig>
    extends CsClientCliCommandRunner<T> {

  protected ThymusVCClient client;
  protected Configuration configuration;

  private ThymusVCClient createThymusClient(T config)
      throws CsCliRunnerException, CsCliLoginException {
    Option<Map<ThymusEndpoint, VcpToken>> tokens = getVcpTokens(configuration);
    return config
        .getCustomClientFactory()
        .map(factory -> Try.success(factory.create(config)))
        .getOrElse(
            Try.of(
                () -> {
                  List<ThymusEndpoint> endpoints =
                      Arrays.stream(configuration.getProviders())
                          .map(VcpConfiguration::getThymusServiceUrl)
                          .map(ThymusEndpoint::new)
                          .collect(Collectors.toList());
                  ThymusVCClient.Builder builder =
                      new ThymusVCClient.Builder()
                          .withEndpoints(endpoints)
                          .withSslCertificateValidation(!configuration.isNoSslValidation());
                  tokens.forEach(
                      t -> builder.withBearerTokenProvider(uri -> t.get(uri).getIdToken()));
                  if (!configuration.isNoSslValidation()) {
                    configuration
                        .getTrustedCertificates()
                        .forEach(p -> builder.withTrustedCertificate(p.toFile()));
                  }
                  return builder.build();
                }))
        .getOrElseThrow(
            t ->
                new CsCliRunnerException(
                    getMessages().getString("client-instantiation-failed"), t));
  }

  public ThymusClientCliCommandRunner(T config)
      throws CsCliRunnerException, CsCliConfigurationException {
    super(config);
    initializeMessageBundle(THYMUS_MESSAGE_BUNDLE);
    this.configuration = Configuration.getInstance();
    try {
      client = createThymusClient(config);
    } catch (Exception e) {
      throw new CsCliRunnerException(getMessages().getString("client-instantiation-failed"), e);
    }
  }

  private Option<Map<ThymusEndpoint, VcpToken>> getVcpTokens(Configuration configuration)
      throws CsCliLoginException {
    if (!VcpTokenStore.exists()) {
      return Option.none();
    } else {
      VcpTokenStore vcpTokenStore =
          VcpTokenStore.load(true).getOrElseThrow(CsCliLoginException::new);
      return Option.some(
          Maps.uniqueIndex(
              vcpTokenStore.getTokens(), t -> getThymusEndpoint(t.getVcpBaseUrl(), configuration)));
    }
  }

  private ThymusEndpoint getThymusEndpoint(String vcpBaseUrl, Configuration configuration) {
    return Option.ofOptional(
            Arrays.stream(configuration.getProviders())
                .filter(c -> c.getBaseUrl().toString().equals(vcpBaseUrl))
                .findFirst()
                .map(c -> ThymusEndpoint.Builder().withServiceUri(c.getThymusServiceUrl()).build()))
        .getOrElseThrow(
            () -> new IllegalStateException("mismatch between configuration and token store"));
  }
}
