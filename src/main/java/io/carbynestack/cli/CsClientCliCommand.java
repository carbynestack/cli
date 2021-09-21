/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli;

import io.carbynestack.cli.config.CsClientCliCommandConfig;

public class CsClientCliCommand {
  private CsClientCliCommandConfig config;
  private Class<? extends CsClientCliCommandRunner> runner;

  public CsClientCliCommand(
      CsClientCliCommandConfig config, Class<? extends CsClientCliCommandRunner> runner) {
    this.config = config;
    this.runner = runner;
  }

  public CsClientCliCommandConfig getConfig() {
    return config;
  }

  public Class<? extends CsClientCliCommandRunner> getRunner() {
    return runner;
  }
}
