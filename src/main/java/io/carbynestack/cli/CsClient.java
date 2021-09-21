/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli;

import io.carbynestack.cli.config.CsClientCliConfig;

public class CsClient {
  private CsClientCliConfig config;
  private Class<? extends CsClientCli> clientCli;

  public CsClient(CsClientCliConfig config, Class<? extends CsClientCli> clientCli) {
    this.config = config;
    this.clientCli = clientCli;
  }

  public CsClientCliConfig getConfig() {
    return config;
  }

  public Class<? extends CsClientCli> getClientCli() {
    return clientCli;
  }
}
