/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command;

import static io.carbynestack.cli.client.amphora.AmphoraClientCli.*;

import com.google.common.collect.Maps;
import io.carbynestack.amphora.client.AmphoraClient;
import io.carbynestack.amphora.client.BearerTokenProvider;
import io.carbynestack.amphora.client.DefaultAmphoraClient;
import io.carbynestack.amphora.client.DefaultAmphoraClientBuilder;
import io.carbynestack.amphora.common.AmphoraServiceUri;
import io.carbynestack.cli.CsClientCliCommandRunner;
import io.carbynestack.cli.client.amphora.config.AmphoraClientCliCommandConfig;
import io.carbynestack.cli.configuration.Configuration;
import io.carbynestack.cli.configuration.VcpConfiguration;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.carbynestack.cli.login.VcpToken;
import io.carbynestack.cli.login.VcpTokenStore;
import io.carbynestack.cli.util.KeyStoreUtil;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.Map;

public abstract class AmphoraClientCliCommandRunner<T extends AmphoraClientCliCommandConfig>
    extends CsClientCliCommandRunner<T> {

  private final AmphoraClient amphoraClient;

  public AmphoraClientCliCommandRunner(T config)
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    super(config);
    initializeMessageBundle(AMPHORA_MESSAGE_BUNDLE);
    Configuration configuration = Configuration.getInstance();
    Option<Map<AmphoraServiceUri, VcpToken>> tokens = getVcpTokens(configuration);
    amphoraClient =
        config
            .getCustomClientFactory()
            .map(factory -> Try.of(factory::create))
            .getOrElse(
                Try.of(
                    () -> {
                      DefaultAmphoraClientBuilder amphoraClientBuilder =
                          DefaultAmphoraClient.builder()
                              .prime(configuration.getPrime())
                              .r(configuration.getR())
                              .rInv(configuration.getRInv());
                      Arrays.stream(configuration.getProviders())
                          .map(VcpConfiguration::getAmphoraServiceUri)
                          .forEach(amphoraClientBuilder::addEndpoint);
                      if (tokens.isDefined()) {
                        tokens
                            .map(
                                map ->
                                    (BearerTokenProvider<AmphoraServiceUri>)
                                        amphoraServiceUri ->
                                            map.get(amphoraServiceUri).getIdToken())
                            .map(amphoraClientBuilder::bearerTokenProvider);
                      }
                      KeyStoreUtil.tempKeyStoreForPems(configuration.getTrustedCertificates())
                          .peek(amphoraClientBuilder::addTrustedCertificate);
                      if (configuration.isNoSslValidation()) {
                        amphoraClientBuilder.withoutSslCertificateValidation();
                      }
                      return amphoraClientBuilder.build();
                    }))
            .getOrElseThrow(
                exception ->
                    new CsCliRunnerException(
                        getMessages().getString("client-instantiation-failed"), exception));
  }

  public AmphoraClient getAmphoraClient() {
    return amphoraClient;
  }

  private Option<Map<AmphoraServiceUri, VcpToken>> getVcpTokens(Configuration configuration)
      throws CsCliLoginException {
    if (!VcpTokenStore.exists()) {
      return Option.none();
    } else {
      VcpTokenStore vcpTokenStore =
          VcpTokenStore.load(true).getOrElseThrow(CsCliLoginException::new);
      return Option.some(
          Maps.uniqueIndex(
              vcpTokenStore.getTokens(),
              t -> getAmphoraServiceUri(t.getVcpBaseUrl(), configuration)));
    }
  }

  private AmphoraServiceUri getAmphoraServiceUri(String vcpBaseUrl, Configuration configuration) {
    return Option.ofOptional(
            Arrays.stream(configuration.getProviders())
                .filter(c -> c.getBaseUrl().toString().equals(vcpBaseUrl))
                .findFirst()
                .map(VcpConfiguration::getAmphoraServiceUri))
        .getOrElseThrow(
            () -> new IllegalStateException("mismatch between configuration and token store"));
  }
}
