/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class VcpToken {

  String vcpBaseUrl;
  String accessToken;
  String idToken;
  String refreshToken;
  Date expires;

  public static VcpToken from(Date created, URI vcpBaseUrl, OIDCTokens tokens) {
    return new VcpToken(
        vcpBaseUrl.toString(),
        tokens.getAccessToken().getValue(),
        tokens.getIDTokenString(),
        tokens.getRefreshToken().getValue(),
        Date.from(
            created.toInstant().plus(tokens.getAccessToken().getLifetime(), ChronoUnit.SECONDS)));
  }

  public static VcpToken from(URI vcpBaseUrl, OIDCTokens tokens) {
    return from(new Date(), vcpBaseUrl, tokens);
  }

  @JsonIgnore
  public boolean isExpired() {
    return new Date().after(expires);
  }
}
