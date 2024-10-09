/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.util;

import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import lombok.experimental.UtilityClass;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

import java.nio.file.Files;
import java.nio.file.Paths;

import static net.minidev.json.parser.JSONParser.DEFAULT_PERMISSIVE_MODE;

@UtilityClass
public class TokenUtils {

  public final int VALIDITY = 24 * 60 * 60;

  public OIDCTokens createToken() {
    try {
      String jsonString =
          new String(Files.readAllBytes(Paths.get("src/test/resources/id_token.json")));
      JSONObject jsonObject =
          (JSONObject) new JSONParser(DEFAULT_PERMISSIVE_MODE).parse(jsonString);
      return OIDCTokens.parse(jsonObject);
    } catch (Exception e) {
      throw new TokenCreationException(e);
    }
  }
}

/*
 * Avoiding throwing raw exception types.
 */
class TokenCreationException extends RuntimeException {
  public TokenCreationException(Throwable cause) {
    super(cause);
  }
}
