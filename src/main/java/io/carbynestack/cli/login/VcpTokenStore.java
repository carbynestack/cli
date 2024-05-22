/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import static io.carbynestack.cli.login.LoginCommand.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import io.carbynestack.cli.configuration.Configuration;
import io.carbynestack.cli.configuration.VcpConfiguration;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
@Value
@Builder(builderClassName = "VcpTokenStoreBuilder", toBuilder = true)
public class VcpTokenStore {

  public interface VcpTokenStoreError extends AuthenticationError {}

  public enum VcpTokenStoreErrors implements VcpTokenStoreError {

    /** The token store could not be read */
    READING_TOKEN_STORE_FAILED,

    /** The token store could not be written. */
    PERSISTING_TOKEN_STORE_FAILED,

    /** The VCP configuration could not be fetched. */
    VCP_CONFIGURATION_UNAVAILABLE,

    /** Refreshing a token failed */
    REFRESHING_TOKEN_FAILED
  }

  @Value
  @RequiredArgsConstructor(staticName = "of")
  public static class ByTokenError implements VcpTokenStoreError {
    @NonNull HashMap<URI, VcpTokenStoreError> errors;
  }

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static Option<Path> customDefaultLocation = Option.none();
  private static final OAuth2ServiceProvider DEFAULT_OAUTH20_SERVICE_PROVIDER =
      c ->
          new ServiceBuilder(c.getOAuth2ClientId())
              .build(MicrosoftAzureActiveDirectory20Api.custom(TENANT_ID));

  public static Path getDefaultLocation() {
    return customDefaultLocation.getOrElse(
        FileUtils.getUserDirectory().toPath().resolve(".cs/access-tokens.json"));
  }

  public static void setDefaultLocation(Option<Path> defaultLocation) {
    customDefaultLocation = defaultLocation;
  }

  public static Either<VcpTokenStoreError, VcpTokenStore> load(Reader r, boolean refresh) {
    // Unfortunately type inference does not work, when putting the following two lines on the same
    // line
    Either<VcpTokenStoreError, VcpTokenStore> notRefreshedStore =
        Try.of(() -> OBJECT_MAPPER.readValue(r, VcpTokenStore.class))
            .onFailure(t -> log.error("reading token store failed", t))
            .toEither()
            .mapLeft(t -> VcpTokenStoreErrors.READING_TOKEN_STORE_FAILED);
    return refresh ? notRefreshedStore.flatMap(VcpTokenStore::refresh) : notRefreshedStore;
  }

  public static boolean exists() {
    return getDefaultLocation().toFile().exists();
  }

  public static Either<VcpTokenStoreError, VcpTokenStore> load(boolean refresh) {
    File location = getDefaultLocation().toFile();
    try (FileReader r = new FileReader(location)) {
      return load(r, refresh);
    } catch (IOException ioe) {
      log.error("reading token store failed", ioe);
      return Either.left(VcpTokenStoreErrors.READING_TOKEN_STORE_FAILED);
    }
  }

  /*
   * This is extended by Lombok. Required to make initialization work for Jackson deserialization.
   */
  public static class VcpTokenStoreBuilder {
    private OAuth2ServiceProvider oAuth20ServiceProvider = DEFAULT_OAUTH20_SERVICE_PROVIDER;
  }

  /*
   * Used to exclude oAuth20ServiceProvider from JSON serialization as @JsonIgnore does (for some reason) not work
   * together with @Value class.
   */
  @JsonIgnoreType
  public interface OAuth2ServiceProvider extends Function<VcpConfiguration, OAuth20Service> {}

  @Singular List<VcpToken> tokens;

  @JsonIgnore @EqualsAndHashCode.Exclude OAuth2ServiceProvider oAuth20ServiceProvider;

  public VcpTokenStore() {
    this.tokens = Lists.newArrayList();
    this.oAuth20ServiceProvider = DEFAULT_OAUTH20_SERVICE_PROVIDER;
  }

  public VcpTokenStore(List<VcpToken> tokens, OAuth2ServiceProvider oAuth20ServiceProvider) {
    this.tokens = tokens;
    this.oAuth20ServiceProvider =
        oAuth20ServiceProvider == null ? DEFAULT_OAUTH20_SERVICE_PROVIDER : oAuth20ServiceProvider;
  }

  public Either<VcpTokenStoreError, VcpTokenStore> persist(Writer w) {
    return Try.of(
            () -> {
              OBJECT_MAPPER.writeValue(w, this);
              return this;
            })
        .onFailure(t -> log.error("persisting token store failed: %s", t))
        .toEither()
        .mapLeft(t -> VcpTokenStoreErrors.PERSISTING_TOKEN_STORE_FAILED);
  }

  public Either<VcpTokenStoreError, VcpTokenStore> persist() {
    return Try.of(
            () -> {
              try (FileWriter w = new FileWriter(getDefaultLocation().toFile())) {
                return persist(w);
              }
            })
        .onFailure(t -> log.error("persisting token store failed", t))
        .toEither()
        .mapLeft(t -> (VcpTokenStoreError) VcpTokenStoreErrors.PERSISTING_TOKEN_STORE_FAILED)
        .flatMap(e -> e);
  }

  public Either<VcpTokenStoreError, VcpTokenStore> refresh() {
    return Try.of(Configuration::getInstance)
        .onFailure(t -> log.error("failed to read VC configuration", t))
        .toEither()
        .mapLeft(t -> (VcpTokenStoreError) VcpTokenStoreErrors.READING_TOKEN_STORE_FAILED)
        .flatMap(
            c -> {
              Map<Boolean, List<Tuple2<URI, Either<VcpTokenStoreError, VcpToken>>>> grouped =
                  this.getTokens().stream()
                      .map(t -> new Tuple2<>(URI.create(t.getVcpBaseUrl()), refresh(t, c)))
                      .collect(Collectors.groupingBy(a -> a._2.isRight()));
              return grouped.getOrDefault(Boolean.FALSE, Lists.newArrayList()).isEmpty()
                  ? Either.right(
                      this.toBuilder()
                          .clearTokens()
                          .tokens(
                              grouped.get(Boolean.TRUE).stream()
                                  .map(a -> a._2.get())
                                  .collect(Collectors.toList()))
                          .build())
                  : Either.left(
                      ByTokenError.of(
                          new HashMap<>(
                              grouped.get(Boolean.FALSE).stream()
                                  .filter(a -> a._2.isLeft())
                                  .collect(Collectors.toMap(a -> a._1, a -> a._2.getLeft())))));
            });
  }

  private Either<VcpTokenStoreError, VcpToken> refresh(
      VcpToken token, Configuration configuration) {
    return Option.ofOptional(
            Arrays.stream(configuration.getProviders())
                .filter(c -> c.getBaseUrl().equals(URI.create(token.getVcpBaseUrl())))
                .findFirst())
        .onEmpty(
            () ->
                log.error(
                    "no matching configuration for provider with base URL {}",
                    token.getVcpBaseUrl()))
        .toEither((VcpTokenStoreError) VcpTokenStoreErrors.VCP_CONFIGURATION_UNAVAILABLE)
        .flatMap(
            c -> {
              Date expiration = token.getExpires();
              Duration stillValidFor = Duration.between(Instant.now(), expiration.toInstant());
              if (stillValidFor.toHours() < 1) {

                log.debug("refreshing token for VCP with base URL {}", token.getVcpBaseUrl());
                OAuth20Service oAuth20Service = getOAuth20ServiceProvider().apply(c);
                return Try.of(
                        () -> {
                          ClientID clientID = new ClientID(c.getOAuth2ClientId());
                          RefreshToken receivedRefreshToken =
                              new RefreshToken(token.getRefreshToken());
                          AuthorizationGrant refreshTokenGrant =
                              new RefreshTokenGrant(receivedRefreshToken);
                          URI tokenEndpoint = c.getOauth2TokenEndpointUri();

                          Scope authScope = new Scope();
                          authScope.add("offline");
                          authScope.add("openid");

                          TokenRequest request =
                              new TokenRequest(
                                  tokenEndpoint, clientID, refreshTokenGrant, authScope);
                          var oidcTokenResponse =
                              OIDCTokenResponse.parse(request.toHTTPRequest().send());
                          if (oidcTokenResponse.indicatesSuccess()) {
                            return oidcTokenResponse.toSuccessResponse().getOIDCTokens();
                          } else {
                            throw new CsCliRunnerException(
                                oidcTokenResponse.toErrorResponse().getErrorObject().toString());
                          }
                        })
                    .onFailure(
                        t ->
                            log.error(
                                String.format(
                                    "refreshing token for provider with URL %s failed",
                                    token.getVcpBaseUrl()),
                                t))
                    .toEither()
                    .bimap(
                        t -> (VcpTokenStoreError) VcpTokenStoreErrors.REFRESHING_TOKEN_FAILED,
                        t -> VcpToken.from(c.getBaseUrl(), t));
              } else {
                return Either.right(token);
              }
            });
  }
}
