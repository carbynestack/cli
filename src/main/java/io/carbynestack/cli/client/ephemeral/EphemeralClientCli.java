/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.ephemeral;

import io.carbynestack.cli.CsClientCli;
import io.carbynestack.cli.CsClientCliCommand;
import io.carbynestack.cli.client.ephemeral.command.ExecuteEphemeralClientCliCommandRunner;
import io.carbynestack.cli.client.ephemeral.command.config.ExecuteEphemeralClientCliCommandConfig;
import io.carbynestack.cli.client.ephemeral.config.EphemeralClientCliConfig;
import io.vavr.control.Option;

public class EphemeralClientCli extends CsClientCli {

  public static final String EPHEMERAL_MESSAGE_BUNDLE = "EphemeralMessageBundle";

  public EphemeralClientCli(
      EphemeralClientCliConfig config,
      boolean debug,
      Option<EphemeralClientFactory> customClientFactory) {
    super(config, debug);
    CsClientCliCommand execute =
        new CsClientCliCommand(
            new ExecuteEphemeralClientCliCommandConfig(customClientFactory),
            ExecuteEphemeralClientCliCommandRunner.class);
    this.addCommand(execute);
  }

  public EphemeralClientCli(EphemeralClientCliConfig config, boolean debug) {
    this(config, debug, Option.none());
  }
}
