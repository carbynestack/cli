/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import io.undertow.Undertow;
import io.undertow.util.Headers;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.net.URI;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;


@Slf4j
public class OAuth2AuthenticationCodeCallbackHttpServer implements Closeable {

    public static final long DEFAULT_TIMEOUT_MILLIS = 120L * 1000L;
    private final TransferQueue<Either<CallbackError, AuthorizationCode>> queue = new LinkedTransferQueue<>();
    private final Undertow server;
    private final long timeout;
    private boolean transferred;
    public OAuth2AuthenticationCodeCallbackHttpServer(URI callbackUrl, State state) {
        this(callbackUrl, state, DEFAULT_TIMEOUT_MILLIS);
    }

    public OAuth2AuthenticationCodeCallbackHttpServer(URI callbackUrl, State state, long timeout) {
        transferred = false;
        this.timeout = timeout;
        server = Undertow.builder()
                .addHttpListener(callbackUrl.getPort(), callbackUrl.getHost())
                .setHandler(handler -> {
                    synchronized (OAuth2AuthenticationCodeCallbackHttpServer.this) {

                        // example
                        AuthenticationResponse response = AuthenticationResponseParser.parse(URI.create(handler.getRequestURL() + "?" + handler.getQueryString()));

                        // Check the state to ensure the response is valid (not a CSRF attack)
                        if (response instanceof AuthenticationErrorResponse || !response.getState().equals(state)) {
                            handler.setStatusCode(500);
                            handler.endExchange();
                            return;
                        }

                        // Convert to a successful response to get the access token
                        var accessToken = response.toSuccessResponse().getAuthorizationCode();
                        Either<CallbackError, AuthorizationCode> transferElement = accessToken == null
                                ? Either.left(CallbackError.MISSING_AUTHENTICATION_CODE)
                                : Either.right(accessToken);

                        // transfer it to the concurrent.TransferQueue to be picked up by the getAccessToken method
                        if (!transferred && queue.tryTransfer(transferElement, timeout, TimeUnit.MILLISECONDS)) {
                            handler.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            handler.getResponseSender().send("Authorization code received. You can close this window now.");

                            transferred = true;
                        } else {
                            handler.setStatusCode(500);
                            handler.endExchange();
                        }
                    }
                })
                .build();
        server.start();
    }

    public Either<CallbackError, AuthorizationCode> getAuthorizationCode() {
        return Try.of(() -> {
                    Either<CallbackError, AuthorizationCode> r = queue.poll(timeout, TimeUnit.MILLISECONDS);
                    return r == null ? Either.<CallbackError, AuthorizationCode>left(CallbackError.TIME_OUT) : r;
                })
                .onFailure(e -> log.error("failed fetching authorization code", e))
                .getOrElseGet(t -> Either.left(
                        Match(t).of(
                                Case($(instanceOf(InterruptedException.class)), CallbackError.INTERRUPTED),
                                Case($(), CallbackError.UNEXPECTED)
                        )
                ));
    }

    @Override
    public void close() {
        server.getWorker().shutdownNow();
        server.stop();
    }

    public enum CallbackError implements AuthenticationError {
        /**
         * The callback request didn't contain the request code.
         */
        MISSING_AUTHENTICATION_CODE,

        /**
         * Interrupted while waiting for callback to be invoked.
         */
        INTERRUPTED,

        /**
         * Time out while waiting for callback.
         */
        TIME_OUT,

        /**
         * Some unexpected error condition occurred fetching the authorization code.
         */
        UNEXPECTED
    }
}
