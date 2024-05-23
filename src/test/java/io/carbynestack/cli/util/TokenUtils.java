/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.util;

import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TokenUtils {

  public final int VALIDITY = 24 * 60 * 60;

  public OIDCTokens createToken() {
    return new OIDCTokens(null, null);
  }
}
