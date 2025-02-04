/*
 * Copyright (c) 2025 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus.command.config;

import static io.carbynestack.cli.client.thymus.ThymusClientCli.THYMUS_MESSAGE_BUNDLE;

import com.beust.jcommander.Parameters;
import io.carbynestack.cli.client.thymus.ThymusClientFactory;
import io.carbynestack.cli.client.thymus.config.ThymusClientCliCommandConfig;
import io.vavr.control.Option;
import lombok.Getter;

@Getter
@Parameters(
    resourceBundle = THYMUS_MESSAGE_BUNDLE,
    commandDescriptionKey = "list-policies.command-description")
public class ListPoliciesThymusClientCliCommandConfig extends ThymusClientCliCommandConfig {

  public static final String COMMAND_NAME = "list-policies";

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  public ListPoliciesThymusClientCliCommandConfig(Option<ThymusClientFactory> customClientFactory) {
    super(customClientFactory);
  }
}
