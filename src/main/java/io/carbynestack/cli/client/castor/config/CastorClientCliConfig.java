/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.castor.config;

import static io.carbynestack.cli.client.castor.CastorClientCli.CASTOR_MESSAGE_BUNDLE;

import com.beust.jcommander.Parameters;
import io.carbynestack.cli.config.CsClientCliConfig;

@Parameters(resourceBundle = CASTOR_MESSAGE_BUNDLE, commandDescriptionKey = "client-description")
public class CastorClientCliConfig extends CsClientCliConfig {

  public static final String CLIENT_NAME = "castor";

  @Override
  public String getClientName() {
    return CLIENT_NAME;
  }
}
