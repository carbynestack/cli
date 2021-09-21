/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import static io.carbynestack.cli.login.BrowserLauncher.*;
import static io.carbynestack.cli.login.BrowserLauncher.BrowserLaunchError.*;
import static io.carbynestack.cli.login.LoginCommand.*;
import static io.carbynestack.cli.login.VcpTokenStore.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.AccessTokenRequestParams;
import com.github.scribejava.core.oauth.OAuth20Service;
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
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BrowserLauncher.class})
public class LoginCommandTest {

  @Rule public final TemporaryConfiguration temporaryConfiguration = new TemporaryConfiguration();

  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  private OAuth20Service oAuth20Service;
  private OAuth2AccessToken token;

  @Before
  public void configureMocks() throws Exception {
    PowerMockito.mockStatic(BrowserLauncher.class);
    oAuth20Service = mock(OAuth20Service.class);
    token = TokenUtils.createToken("test");
    doReturn(token).when(oAuth20Service).getAccessToken(any(AccessTokenRequestParams.class));
  }

  @Test
  public void whenLogin_thenStoreHasBeenCreated() throws Exception {
    doReturn("http://authorize.test.com").when(oAuth20Service).getAuthorizationUrl();
    when(browse(any())).thenReturn(Option.none());
    OAuth2AuthenticationCodeCallbackHttpServer callbackServer =
        mock(OAuth2AuthenticationCodeCallbackHttpServer.class);
    doReturn(Either.right(RandomStringUtils.randomAlphanumeric(10))).when(callbackServer).getCode();
    LoginCommand command =
        new LoginCommand(
            DEFAULT_CALLBACK_PORTS, (cfg, url) -> oAuth20Service, cfg -> callbackServer);
    command.login();
    Either<VcpTokenStoreError, VcpTokenStore> store = load(false);
    assertThat("store has not been created", store.isRight());
    for (VcpToken t : store.get().getTokens()) {
      assertEquals(
          "stored access token does not equal expected token",
          token.getAccessToken(),
          t.getAccessToken());
    }
  }

  @Test
  public void givenLaunchingBrowserFails_whenLogin_thenThrows() throws Exception {
    doReturn("http://authorize.test.com").when(oAuth20Service).getAuthorizationUrl();
    when(browse(any())).thenReturn(Option.some(NOT_SUPPORTED));
    OAuth2AuthenticationCodeCallbackHttpServer callbackServer =
        mock(OAuth2AuthenticationCodeCallbackHttpServer.class);
    doReturn(Either.right(RandomStringUtils.randomAlphanumeric(10))).when(callbackServer).getCode();
    LoginCommand command =
        new LoginCommand(
            DEFAULT_CALLBACK_PORTS, (cfg, url) -> oAuth20Service, cfg -> callbackServer);
    try {
      command.login();
      fail("expected exception has not been thrown");
    } catch (CsCliLoginException scle) {
      assertEquals("unexpected error returned", NOT_SUPPORTED, scle.getAuthenticationError());
    }
  }

  @Test
  public void givenPortIsInUse_whenLogin_thenSucceedOnNextPort() throws Exception {
    doReturn("http://authorize.test.com").when(oAuth20Service).getAuthorizationUrl();
    when(browse(any())).thenReturn(Option.none());
    OAuth2AuthenticationCodeCallbackHttpServer callbackServer =
        mock(OAuth2AuthenticationCodeCallbackHttpServer.class);
    doReturn(Either.right(RandomStringUtils.randomAlphanumeric(10))).when(callbackServer).getCode();
    AtomicInteger attempt = new AtomicInteger(0);
    int rounds = 5;
    LoginCommand command =
        new LoginCommand(
            DEFAULT_CALLBACK_PORTS,
            (cfg, url) -> oAuth20Service,
            cfg -> {
              if (attempt.getAndAdd(1) < rounds) {
                throw new RuntimeException(new BindException());
              } else {
                return callbackServer;
              }
            });
    command.login();
    assertEquals("not enough attempts detected", rounds + 2, attempt.get());
  }

  @Test
  public void givenAllPortsAreInUse_whenLogin_thenThrow() throws Exception {
    doReturn("http://authorize.test.com").when(oAuth20Service).getAuthorizationUrl();
    when(browse(any())).thenReturn(Option.none());
    OAuth2AuthenticationCodeCallbackHttpServer callbackServer =
        mock(OAuth2AuthenticationCodeCallbackHttpServer.class);
    doReturn(Either.right(RandomStringUtils.randomAlphanumeric(10))).when(callbackServer).getCode();
    int basePort = 32768;
    Range<Integer> portRange = Range.between(basePort, basePort + 7);
    AtomicInteger attempts = new AtomicInteger(0);
    LoginCommand command =
        new LoginCommand(
            portRange,
            (cfg, url) -> oAuth20Service,
            cfg -> {
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

  @Test
  public void givenRuntimeExceptionDuringCallbackServerCreation_whenLogin_thenThrow()
      throws Exception {
    doReturn("http://authorize.test.com").when(oAuth20Service).getAuthorizationUrl();
    when(browse(any())).thenReturn(Option.none());
    OAuth2AuthenticationCodeCallbackHttpServer callbackServer =
        mock(OAuth2AuthenticationCodeCallbackHttpServer.class);
    doReturn(Either.right(RandomStringUtils.randomAlphanumeric(10))).when(callbackServer).getCode();
    LoginCommand command =
        new LoginCommand(
            DEFAULT_CALLBACK_PORTS,
            (cfg, url) -> oAuth20Service,
            cfg -> {
              throw new RuntimeException(new SocketException());
            });
    try {
      command.login();
      fail("expected exception has not been thrown");
    } catch (CsCliLoginException scle) {
      assertEquals(
          "unexpected error returned", LoginCommandError.UNEXPECTED, scle.getAuthenticationError());
    }
  }
}
