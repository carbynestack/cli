/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.config;

import static io.carbynestack.cli.client.amphora.AmphoraClientCli.*;

import com.beust.jcommander.Parameters;
import io.carbynestack.cli.config.CsClientCliConfig;

@Parameters(resourceBundle = AMPHORA_MESSAGE_BUNDLE, commandDescriptionKey = "client-description")
public class AmphoraClientCliConfig extends CsClientCliConfig {

  public static final String CLIENT_NAME = "amphora";

  @Override
  public String getClientName() {
    return CLIENT_NAME;
  }
}
