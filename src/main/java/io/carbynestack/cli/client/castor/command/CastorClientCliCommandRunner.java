/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.castor.command;

import io.carbynestack.castor.client.download.CastorIntraVcpClient;
import io.carbynestack.castor.client.download.DefaultCastorIntraVcpClient;
import io.carbynestack.castor.client.upload.CastorUploadClient;
import io.carbynestack.castor.client.upload.DefaultCastorUploadClient;
import io.carbynestack.castor.common.BearerTokenProvider;
import io.carbynestack.cli.CsClientCliCommandRunner;
import io.carbynestack.cli.client.castor.CastorClientCli;
import io.carbynestack.cli.client.castor.config.CastorClientCliCommandConfig;
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
import java.io.File;
import java.nio.file.Path;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class CastorClientCliCommandRunner<T extends CastorClientCliCommandConfig>
    extends CsClientCliCommandRunner<T> {
  static final long CASTOR_COMMUNICATION_TIMEOUT = 10000L;

  CastorUploadClient castorUploadClient;
  CastorIntraVcpClient castorIntraVcpClient;

  CastorClientCliCommandRunner(T config)
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    super(config);
    initializeMessageBundle(CastorClientCli.CASTOR_MESSAGE_BUNDLE);
    Configuration configuration = Configuration.getInstance();
    VcpConfiguration vcpConfiguration = configuration.getProvider(config.getId());
    Option<VcpToken> token = getVcpToken(vcpConfiguration);
    castorUploadClient =
        config
            .getCustomUploadClientFactory()
            .map(factory -> Try.of(factory::create))
            .getOrElse(
                Try.of(
                    () -> {
                      DefaultCastorUploadClient.Builder builder =
                          DefaultCastorUploadClient.builder(
                              vcpConfiguration
                                  .getCastorServiceUri()
                                  .getRestServiceUri()
                                  .toString());
                      for (File certificateFile :
                          configuration.getTrustedCertificates().stream()
                              .map(Path::toFile)
                              .collect(Collectors.toList())) {
                        builder.withTrustedCertificate(certificateFile);
                      }
                      if (configuration.isNoSslValidation()) {
                        builder.withoutSslCertificateValidation();
                      }
                      token
                          .map(
                              t ->
                                  BearerTokenProvider.builder()
                                      .bearerToken(
                                          vcpConfiguration.getCastorServiceUri(), t.getIdToken())
                                      .build())
                          .peek(builder::withBearerTokenProvider);
                      return builder.build();
                    }))
            .getOrElseThrow(
                exception ->
                    new CsCliRunnerException(
                        getMessages().getString("client-instantiation-failed"), exception));
    castorIntraVcpClient =
        config
            .getCustomIntraVcpClientFactory()
            .map(factory -> Try.of(factory::create))
            .getOrElse(
                Try.of(
                    () -> {
                      DefaultCastorIntraVcpClient.Builder intraVcpClientBuilder =
                          DefaultCastorIntraVcpClient.builder(
                              vcpConfiguration
                                  .getCastorServiceUri()
                                  .getRestServiceUri()
                                  .toString());
                      KeyStoreUtil.tempKeyStoreForPems(configuration.getTrustedCertificates())
                          .peek(intraVcpClientBuilder::withTrustedCertificate);
                      if (configuration.isNoSslValidation()) {
                        intraVcpClientBuilder.withoutSslCertificateValidation();
                      }
                      token
                          .map(
                              t ->
                                  BearerTokenProvider.builder()
                                      .bearerToken(
                                          vcpConfiguration.getCastorServiceUri(), t.getIdToken())
                                      .build())
                          .peek(intraVcpClientBuilder::withBearerTokenProvider);
                      return intraVcpClientBuilder.build();
                    }))
            .getOrElseThrow(
                exception ->
                    new CsCliRunnerException(
                        getMessages().getString("client-instantiation-failed"), exception));
  }

  private Option<VcpToken> getVcpToken(VcpConfiguration vcpConfiguration)
      throws CsCliLoginException, CsCliRunnerException {
    if (!VcpTokenStore.exists()) {
      return Option.none();
    } else {
      VcpTokenStore vcpTokenStore =
          VcpTokenStore.load(true).getOrElseThrow(CsCliLoginException::new);
      return Option.some(
          vcpTokenStore.getTokens().stream()
              .filter(t -> t.getVcpBaseUrl().equals(vcpConfiguration.getBaseUrl().toString()))
              .findFirst()
              .orElseThrow(
                  () -> new CsCliRunnerException("No matching OAuth2 access token found", null)));
    }
  }
}
