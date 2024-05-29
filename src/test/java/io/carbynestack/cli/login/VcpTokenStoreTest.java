/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.carbynestack.cli.TemporaryConfiguration;
import io.carbynestack.cli.configuration.Configuration;
import io.carbynestack.cli.configuration.ConfigurationUtil;
import io.carbynestack.cli.util.TokenUtils;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;

import static io.carbynestack.cli.login.VcpTokenStore.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Configuration.class, VcpTokenStore.class})
public class VcpTokenStoreTest {

  @Rule TemporaryConfiguration temporaryConfiguration = new TemporaryConfiguration();
  private OIDCTokens oidcTokens;

  private static VcpTokenStore createStore(boolean expired) throws Exception {
    Date ref =
        !expired ? new Date() : Date.from(Instant.now().minusSeconds(2 * TokenUtils.VALIDITY));
    Configuration config = ConfigurationUtil.getConfiguration();
    return builder()
        .token(
            VcpToken.from(
                ref, config.getProvider(1).getBaseUrl(), TokenUtils.createToken()))
        .token(
            VcpToken.from(
                ref, config.getProvider(2).getBaseUrl(), TokenUtils.createToken()))
        .build();
  }

  private static Writer getFailingWriter() throws IOException {
    Writer w = mock(Writer.class);
    doThrow(IOException.class).when(w).write(any(char[].class), anyInt(), anyInt());
    return w;
  }

  @Before
  public void configureMocks() throws Exception {
    oidcTokens = TokenUtils.createToken();
  }

  @Test
  public void givenDefaultDoesNotExist_whenLoad_thenFails() {
    setDefaultLocation(Option.some(Paths.get("home", RandomStringUtils.randomAlphabetic(10))));
    Either<VcpTokenStoreError, VcpTokenStore> store = load(false);
    assertThat("load succeeded although file does not exist", store.isLeft());
    assertEquals(
        "wrong error message returned",
        VcpTokenStoreErrors.READING_TOKEN_STORE_FAILED,
        store.getLeft());
  }

  @Test
  public void whenRoundTripPersistLoad_thenDataIsPreserved() throws Exception {
    VcpTokenStore store = createStore(false);
    store.persist();
    Either<VcpTokenStoreError, VcpTokenStore> restored = load(false);
    assertThat("failed to restore store", restored.isRight());
    assertEquals("store data not the same after a round trip", store, restored.get());
  }

  @Test
  public void givenWriterFails_whenPersist_thenFails() throws Exception {
    VcpTokenStore store = createStore(false);
    try (Writer w = getFailingWriter()) {
      Either<VcpTokenStoreError, VcpTokenStore> r = store.persist(w);
      assertThat("persist did not fail", r.isLeft());
      assertEquals(
          "wrong error returned", VcpTokenStoreErrors.PERSISTING_TOKEN_STORE_FAILED, r.getLeft());
    }
  }

  @Test
  public void givenValidTokens_whenRefresh_thenDoesNothing() throws Exception {
    PowerMockito.mockStatic(Configuration.class);
    when(Configuration.getInstance()).thenReturn(ConfigurationUtil.getConfiguration());
    VcpTokenStore store = createStore(false).toBuilder().build();
    assertThat(
        "tokens in store are expired", store.getTokens().stream().noneMatch(VcpToken::isExpired));
    store.refresh();
  }

  @Test
  public void givenExpiredTokens_whenRefresh_thenRefreshesTokens() throws Exception {
    PowerMockito.mockStatic(Configuration.class);
    when(Configuration.getInstance()).thenReturn(ConfigurationUtil.getConfiguration());

    VcpTokenStore store = PowerMockito.spy(createStore(true).toBuilder().build());
    doReturn(new OIDCTokenResponse(oidcTokens.toOIDCTokens())).when(store).sendRefreshToken(Mockito.any(), Mockito.any());

    assertThat(
        "tokens in store are not expired",
        store.getTokens().stream().allMatch(VcpToken::isExpired));
    Either<VcpTokenStoreError, VcpTokenStore> refreshed = store.refresh();
    assertThat("refresh failed", refreshed.isRight());
    assertThat(
        "tokens in store are expired after refresh",
        refreshed.get().getTokens().stream().noneMatch(VcpToken::isExpired));
  }

  @Test
  public void givenExpiredTokensAndFailingProvider_whenRefresh_thenRefreshFails() throws Exception {
    PowerMockito.mockStatic(Configuration.class);
    when(Configuration.getInstance()).thenReturn(ConfigurationUtil.getConfiguration());
    VcpTokenStore store =
        createStore(true).toBuilder().build();
    Either<VcpTokenStoreError, VcpTokenStore> refreshed = store.refresh();
    assertThat("refresh succeeded despite failing provider", refreshed.isLeft());
    assertThat("wrong error returned", refreshed.getLeft() instanceof ByTokenError);
  }
}
