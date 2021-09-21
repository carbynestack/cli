/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.ephemeral.config;

import static io.carbynestack.cli.client.ephemeral.EphemeralClientCli.EPHEMERAL_MESSAGE_BUNDLE;

import com.beust.jcommander.Parameters;
import io.carbynestack.cli.config.CsClientCliConfig;

@Parameters(resourceBundle = EPHEMERAL_MESSAGE_BUNDLE, commandDescriptionKey = "client-description")
public class EphemeralClientCliConfig extends CsClientCliConfig {

  public static final String CLIENT_NAME = "ephemeral";

  @Override
  public String getClientName() {
    return CLIENT_NAME;
  }
}
