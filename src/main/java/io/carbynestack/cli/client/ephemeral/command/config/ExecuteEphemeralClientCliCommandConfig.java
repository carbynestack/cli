/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.ephemeral.command.config;

import static io.carbynestack.cli.client.ephemeral.EphemeralClientCli.EPHEMERAL_MESSAGE_BUNDLE;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Lists;
import io.carbynestack.cli.client.ephemeral.EphemeralClientFactory;
import io.carbynestack.cli.client.ephemeral.config.EphemeralClientCliCommandConfig;
import io.carbynestack.cli.converter.UUIDTypeConverter;
import io.vavr.control.Option;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
@Parameters(
    resourceBundle = EPHEMERAL_MESSAGE_BUNDLE,
    commandDescriptionKey = "execute.command-description")
public class ExecuteEphemeralClientCliCommandConfig extends EphemeralClientCliCommandConfig {

  public static final String COMMAND_NAME = "execute";

  @Parameter(
      names = {"-i", "--input"},
      descriptionKey = "execute.option.input-description",
      converter = UUIDTypeConverter.class,
      order = 1)
  private List<UUID> inputs = Lists.newArrayList();

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  public ExecuteEphemeralClientCliCommandConfig(
      Option<EphemeralClientFactory> customClientFactory) {
    super(customClientFactory);
  }
}
