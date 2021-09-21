/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;

import io.undertow.Undertow;
import io.undertow.util.Headers;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.io.Closeable;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OAuth2AuthenticationCodeCallbackHttpServer implements Closeable {

  public static final long DEFAULT_TIMEOUT_MILLIS = 120L * 1000L;

  public enum CallbackError implements AuthenticationError {
    /** The callback request didn't contain the request code. */
    MISSING_AUTHENTICATION_CODE,

    /** Interrupted while waiting for callback to be invoked. */
    INTERRUPTED,

    /** Time out while waiting for callback. */
    TIME_OUT,

    /** Some unexpected error condition occurred fetching the authorization code. */
    UNEXPECTED
  }

  private final TransferQueue<Either<CallbackError, String>> queue = new LinkedTransferQueue<>();
  private final Undertow server;
  private final long timeout;
  private boolean transferred;

  public OAuth2AuthenticationCodeCallbackHttpServer(URI callbackUrl) {
    this(callbackUrl, DEFAULT_TIMEOUT_MILLIS);
  }

  public OAuth2AuthenticationCodeCallbackHttpServer(URI callbackUrl, long timeout) {
    transferred = false;
    this.timeout = timeout;
    server =
        Undertow.builder()
            .addHttpListener(callbackUrl.getPort(), callbackUrl.getHost())
            .setHandler(
                exchange -> {
                  synchronized (OAuth2AuthenticationCodeCallbackHttpServer.this) {
                    Either<CallbackError, String> code =
                        Option.of(
                                exchange
                                    .getQueryParameters()
                                    .getOrDefault("code", new ArrayDeque<>())
                                    .peek())
                            .toEither(CallbackError.MISSING_AUTHENTICATION_CODE);
                    if (!transferred && queue.tryTransfer(code, timeout, TimeUnit.MILLISECONDS)) {
                      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                      exchange
                          .getResponseSender()
                          .send(
                              code.fold(Object::toString, c -> "Authentication code received.")
                                  + "Please return to CLI.");
                      transferred = true;
                    } else {
                      exchange.setStatusCode(500);
                      exchange.endExchange();
                    }
                  }
                })
            .build();
    server.start();
  }

  public Either<CallbackError, String> getCode() {
    return Try.of(
            () -> {
              Either<CallbackError, String> r = queue.poll(timeout, TimeUnit.MILLISECONDS);
              return r == null ? Either.<CallbackError, String>left(CallbackError.TIME_OUT) : r;
            })
        .onFailure(e -> log.error("failed fetching authorization code", e))
        .getOrElseGet(
            t ->
                Either.left(
                    Match(t)
                        .of(
                            Case(
                                $(instanceOf(InterruptedException.class)),
                                CallbackError.INTERRUPTED),
                            Case($(), CallbackError.UNEXPECTED))));
  }

  @Override
  public void close() {
    server.getWorker().shutdownNow();
    server.stop();
  }
}
