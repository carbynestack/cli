/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.scribejava.core.model.OAuth2AccessToken;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class VcpToken {

  public static VcpToken from(Date created, URI vcpBaseUrl, OAuth2AccessToken token) {
    return new VcpToken(
        vcpBaseUrl.toString(),
        token.getAccessToken(),
        token.getRefreshToken(),
        Date.from(created.toInstant().plus(token.getExpiresIn(), ChronoUnit.SECONDS)));
  }

  public static VcpToken from(URI vcpBaseUrl, OAuth2AccessToken token) {
    return from(new Date(), vcpBaseUrl, token);
  }

  String vcpBaseUrl;
  String accessToken;
  String refreshToken;
  Date expires;

  @JsonIgnore
  public boolean isExpired() {
    return new Date().after(expires);
  }
}
