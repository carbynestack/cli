/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.util;

import static net.minidev.json.parser.JSONParser.DEFAULT_PERMISSIVE_MODE;

import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.experimental.UtilityClass;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

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
      throw new RuntimeException(e);
    }
  }
}
