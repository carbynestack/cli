/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.util;

import com.github.scribejava.core.model.OAuth2AccessToken;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TokenUtils {

  public final int VALIDITY = 24 * 60 * 60;

  public OAuth2AccessToken createToken(String tag) {
    return new OAuth2AccessToken(
        String.format("%s-access-token", tag),
        String.format("%s-token-type", tag),
        VALIDITY,
        String.format("%s-refresh-token", tag),
        String.format("%s-scope", tag),
        String.format("%s-raw-response", tag));
  }
}
