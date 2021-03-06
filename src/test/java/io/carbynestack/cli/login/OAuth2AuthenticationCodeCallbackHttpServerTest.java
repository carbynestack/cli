/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import static io.carbynestack.cli.login.OAuth2AuthenticationCodeCallbackHttpServer.*;
import static io.carbynestack.cli.login.OAuth2AuthenticationCodeCallbackHttpServer.CallbackError.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

public class OAuth2AuthenticationCodeCallbackHttpServerTest {

  private static final int EPHEMERAL_PORT = 32900;
  private static final URI CALLBACK_URL =
      Try.of(
              () ->
                  new URIBuilder()
                      .setScheme("http")
                      .setHost("localhost")
                      .setPort(EPHEMERAL_PORT)
                      .build())
          .get();

  @Test
  public void givenCallbackIsNotCalled_whenGetCode_thenTimesOut() {
    try (OAuth2AuthenticationCodeCallbackHttpServer server =
        new OAuth2AuthenticationCodeCallbackHttpServer(CALLBACK_URL, 1000)) {
      Either<CallbackError, String> r = server.getCode();
      assertThat("getting code didn't fail", r.isLeft());
      assertEquals(
          "getting code did not time out when callback was not invoked", TIME_OUT, r.getLeft());
    }
  }

  @Test
  public void givenCallbackIsCalledWithCodeParameter_whenGetCode_thenReturnsCode()
      throws Exception {
    try (OAuth2AuthenticationCodeCallbackHttpServer server =
        new OAuth2AuthenticationCodeCallbackHttpServer(CALLBACK_URL)) {
      String code = RandomStringUtils.randomAlphanumeric(20);
      CompletableFuture<Try<HttpResponse>> f = invokeCallback(Option.some(code));
      Either<CallbackError, String> r = server.getCode();
      assertThat("invoking callback failed", f.get().get().getStatusLine().getStatusCode() == 200);
      assertThat("getting code didn't succeed", r.isRight());
      assertEquals("incorrect code delivered", code, r.get());
    }
  }

  @Test
  public void givenCallbackIsCalledWithoutCodeParameter_whenGetCode_thenFailsWithError()
      throws Exception {
    try (OAuth2AuthenticationCodeCallbackHttpServer server =
        new OAuth2AuthenticationCodeCallbackHttpServer(CALLBACK_URL)) {
      CompletableFuture<Try<HttpResponse>> f = invokeCallback(Option.none());
      Either<CallbackError, String> r = server.getCode();
      assertThat("invoking callback failed", f.get().get().getStatusLine().getStatusCode() == 200);
      assertThat("getting code succeeded although it should fail", r.isLeft());
      assertEquals("incorrect code delivered", MISSING_AUTHENTICATION_CODE, r.left().get());
    }
  }

  @Test
  public void givenCallbackIsCalledTwice_whenGetCode_thenDoesNotBlock() throws Exception {
    try (OAuth2AuthenticationCodeCallbackHttpServer server =
        new OAuth2AuthenticationCodeCallbackHttpServer(CALLBACK_URL)) {
      String code = RandomStringUtils.randomAlphanumeric(20);
      invokeCallback(Option.some(code));
      server.getCode();
      HttpResponse response = invokeCallback(Option.none()).get(1, TimeUnit.SECONDS).get();
      assertEquals("Expected internal server error", 500, response.getStatusLine().getStatusCode());
    }
  }

  private CompletableFuture<Try<HttpResponse>> invokeCallback(Option<String> code) {
    return CompletableFuture.supplyAsync(
        () ->
            Try.of(
                () -> {
                  URIBuilder builder = new URIBuilder(CALLBACK_URL);
                  code.forEach(c -> builder.addParameter("code", c));
                  URI callbackUrlWithCode = builder.build();
                  HttpClient client = HttpClients.custom().disableAutomaticRetries().build();
                  HttpGet httpGet = new HttpGet(callbackUrlWithCode);
                  return client.execute(httpGet);
                }));
  }
}
