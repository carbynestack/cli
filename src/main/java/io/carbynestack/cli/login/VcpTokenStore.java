/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Value
@Builder(builderClassName = "VcpTokenStoreBuilder", toBuilder = true)
public class VcpTokenStore {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static Option<Path> customDefaultLocation = Option.none();

    @Singular
    List<VcpToken> tokens;
    public VcpTokenStore() {
        this.tokens = Lists.newArrayList();

    }
    public VcpTokenStore(List<VcpToken> tokens) {
        this.tokens = tokens;
    }

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
        return refresh ? notRefreshedStore.flatMap(VcpTokenStore::refresh).peek(VcpTokenStore::persist) : notRefreshedStore;
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

    OIDCTokenResponse sendRefreshToken(VcpToken token, VcpConfiguration c) throws ParseException, IOException {

        ClientID clientID = new ClientID(c.getOAuth2ClientId());
        AuthorizationGrant refreshTokenGrant = new RefreshTokenGrant(new RefreshToken(token.getRefreshToken()));
        Scope authScope = new Scope("offline", "openid");

        TokenRequest request =
                new TokenRequest(
                        c.getOauth2TokenEndpointUri(), clientID, refreshTokenGrant, authScope);
        return OIDCTokenResponse.parse(request.toHTTPRequest().send());

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
                                return Try.of(
                                                () -> {
                                                    var oidcTokenResponse = sendRefreshToken(token, c);
                                                    if (oidcTokenResponse.indicatesSuccess()) {
                                                        return oidcTokenResponse.toSuccessResponse().getOIDCTokens();
                                                    } else {
                                                        throw new CsCliRunnerException(oidcTokenResponse.toErrorResponse().getErrorObject().toString());
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
                                                t -> VcpTokenStoreErrors.REFRESHING_TOKEN_FAILED,
                                                t -> VcpToken.from(c.getBaseUrl(), t));
                            } else {
                                return Either.right(token);
                            }
                        });
    }

    public enum VcpTokenStoreErrors implements VcpTokenStoreError {

        /**
         * The token store could not be read
         */
        READING_TOKEN_STORE_FAILED,

        /**
         * The token store could not be written.
         */
        PERSISTING_TOKEN_STORE_FAILED,

        /**
         * The VCP configuration could not be fetched.
         */
        VCP_CONFIGURATION_UNAVAILABLE,

        /**
         * Refreshing a token failed
         */
        REFRESHING_TOKEN_FAILED
    }

    public interface VcpTokenStoreError extends AuthenticationError {
    }



    @Value
    @RequiredArgsConstructor(staticName = "of")
    public static class ByTokenError implements VcpTokenStoreError {
        @NonNull
        HashMap<URI, VcpTokenStoreError> errors;
    }

}
