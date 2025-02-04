/*
 * Copyright (c) 2025 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus;

import io.carbynestack.cli.CsClientCli;
import io.carbynestack.cli.CsClientCliCommand;
import io.carbynestack.cli.client.thymus.command.GetPolicyThymusClientCliCommandRunner;
import io.carbynestack.cli.client.thymus.command.ListPoliciesThymusClientCliCommandRunner;
import io.carbynestack.cli.client.thymus.command.config.GetPolicyThymusClientCliCommandConfig;
import io.carbynestack.cli.client.thymus.command.config.ListPoliciesThymusClientCliCommandConfig;
import io.carbynestack.cli.client.thymus.config.ThymusClientCliConfig;
import io.vavr.control.Option;
import java.util.Arrays;

public class ThymusClientCli extends CsClientCli {

  public static final String THYMUS_MESSAGE_BUNDLE = "ThymusMessageBundle";

  public ThymusClientCli(
      ThymusClientCliConfig config,
      boolean debug,
      Option<ThymusClientFactory> customClientFactory) {
    super(config, debug);
    Arrays.asList(
            new CsClientCliCommand(
                new ListPoliciesThymusClientCliCommandConfig(customClientFactory),
                ListPoliciesThymusClientCliCommandRunner.class),
            new CsClientCliCommand(
                new GetPolicyThymusClientCliCommandConfig(customClientFactory),
                GetPolicyThymusClientCliCommandRunner.class))
        .forEach(this::addCommand);
  }

  public ThymusClientCli(ThymusClientCliConfig config, boolean debug) {
    this(config, debug, Option.none());
  }
}
