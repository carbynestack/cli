/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import static io.carbynestack.cli.login.BrowserLauncher.BrowserLaunchError.NOT_SUPPORTED;
import static io.carbynestack.cli.login.BrowserLauncher.browse;
import static io.carbynestack.cli.login.LoginCommand.DEFAULT_CALLBACK_PORTS;
import static io.carbynestack.cli.login.LoginCommand.LoginCommandError;
import static io.carbynestack.cli.login.VcpTokenStore.VcpTokenStoreError;
import static io.carbynestack.cli.login.VcpTokenStore.load;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.carbynestack.cli.TemporaryConfiguration;
import io.carbynestack.cli.util.TokenUtils;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.net.BindException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Range;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class LoginCommandTest {

  @Rule public final TemporaryConfiguration temporaryConfiguration = new TemporaryConfiguration();

  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  private OIDCTokens oidcTokens;

  @Before
  public void configureMocks() {
    oidcTokens = TokenUtils.createToken();
  }

  @Test
  public void whenLogin_thenStoreHasBeenCreated() throws Exception {
    try (MockedStatic<BrowserLauncher> ms = mockStatic(BrowserLauncher.class)) {
      ms.when(() -> browse(any())).thenReturn(Option.none());
      OAuth2AuthenticationCodeCallbackHttpServer callbackServer =
          mock(OAuth2AuthenticationCodeCallbackHttpServer.class);
      doReturn(Either.right(new AuthorizationCode())).when(callbackServer).getAuthorizationCode();

      LoginCommand command =
          spy(new LoginCommand(DEFAULT_CALLBACK_PORTS, (url, state) -> callbackServer));
      doReturn(new OIDCTokenResponse(oidcTokens))
          .when(command)
          .sendTokenRequest(Mockito.any(), anyBoolean(), anyList());

      command.login();
      Either<VcpTokenStoreError, VcpTokenStore> store = load(false);
      assertThat("store has not been created", store.isRight());
      for (VcpToken t : store.get().getTokens()) {
        assertEquals(
            "stored access token does not equal expected token",
            oidcTokens.getAccessToken().getValue(),
            t.getAccessToken());
      }
    }
  }

  @Test
  public void givenLaunchingBrowserFails_whenLogin_thenThrows() throws Exception {
    try (MockedStatic<BrowserLauncher> ms = mockStatic(BrowserLauncher.class)) {
      ms.when(() -> browse(any())).thenReturn(Option.some(NOT_SUPPORTED));
      OAuth2AuthenticationCodeCallbackHttpServer callbackServer =
          mock(OAuth2AuthenticationCodeCallbackHttpServer.class);
      doReturn(Either.right(RandomStringUtils.randomAlphanumeric(10)))
          .when(callbackServer)
          .getAuthorizationCode();
      LoginCommand command =
          new LoginCommand(DEFAULT_CALLBACK_PORTS, (cfg, state) -> callbackServer);
      try {
        command.login();
        fail("expected exception has not been thrown");
      } catch (CsCliLoginException scle) {
        assertEquals("unexpected error returned", NOT_SUPPORTED, scle.getAuthenticationError());
      }
    }
  }

  @Test
  public void givenPortIsInUse_whenLogin_thenSucceedOnNextPort() throws Exception {
    try (MockedStatic<BrowserLauncher> ms = mockStatic(BrowserLauncher.class)) {
      ms.when(() -> browse(any())).thenReturn(Option.none());
      OAuth2AuthenticationCodeCallbackHttpServer callbackServer =
          mock(OAuth2AuthenticationCodeCallbackHttpServer.class);
      doReturn(Either.right(new AuthorizationCode())).when(callbackServer).getAuthorizationCode();
      AtomicInteger attempt = new AtomicInteger(0);
      int rounds = 5;
      LoginCommand command =
          spy(
              new LoginCommand(
                  DEFAULT_CALLBACK_PORTS,
                  (cfg, state) -> {
                    if (attempt.getAndAdd(1) < rounds) {
                      throw new RuntimeException(new BindException());
                    } else {
                      return callbackServer;
                    }
                  }));
      doReturn(new OIDCTokenResponse(oidcTokens))
          .when(command)
          .sendTokenRequest(Mockito.any(), anyBoolean(), anyList());

      command.login();
      assertEquals("not enough attempts detected", rounds + 2, attempt.get());
    }
  }

  @Test
  public void givenAllPortsAreInUse_whenLogin_thenThrow() throws Exception {
    try (MockedStatic<BrowserLauncher> ms = mockStatic(BrowserLauncher.class)) {
      ms.when(() -> browse(any())).thenReturn(Option.none());
      OAuth2AuthenticationCodeCallbackHttpServer callbackServer =
          mock(OAuth2AuthenticationCodeCallbackHttpServer.class);
      doReturn(Either.right(RandomStringUtils.randomAlphanumeric(10)))
          .when(callbackServer)
          .getAuthorizationCode();
      int basePort = 32768;
      Range<Integer> portRange = Range.between(basePort, basePort + 7);
      AtomicInteger attempts = new AtomicInteger(0);
      LoginCommand command =
          new LoginCommand(
              portRange,
              (cfg, state) -> {
                attempts.getAndIncrement();
                throw new RuntimeException(new BindException());
              });
      try {
        command.login();
        fail("expected exception has not been thrown");
      } catch (CsCliLoginException scle) {
        assertEquals(
            "unexpected error returned",
            LoginCommandError.PORT_RANGE_EXHAUSTION,
            scle.getAuthenticationError());
        assertEquals("wrong number of attempts", RangeUtils.getLength(portRange), attempts.get());
      }
    }
  }

  @Test
  public void givenRuntimeExceptionDuringCallbackServerCreation_whenLogin_thenThrow()
      throws Exception {
    try (MockedStatic<BrowserLauncher> ms = mockStatic(BrowserLauncher.class)) {
      ms.when(() -> browse(any())).thenReturn(Option.none());
      OAuth2AuthenticationCodeCallbackHttpServer callbackServer =
          mock(OAuth2AuthenticationCodeCallbackHttpServer.class);
      doReturn(Either.right(RandomStringUtils.randomAlphanumeric(10)))
          .when(callbackServer)
          .getAuthorizationCode();
      LoginCommand command =
          new LoginCommand(
              DEFAULT_CALLBACK_PORTS,
              (cfg, state) -> {
                throw new RuntimeException(new SocketException());
              });
      try {
        command.login();
        fail("expected exception has not been thrown");
      } catch (CsCliLoginException scle) {
        assertEquals(
            "unexpected error returned",
            LoginCommandError.UNEXPECTED,
            scle.getAuthenticationError());
      }
    }
  }
}
