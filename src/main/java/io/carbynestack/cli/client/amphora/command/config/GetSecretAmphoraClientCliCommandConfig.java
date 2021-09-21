/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command.config;

import static io.carbynestack.cli.client.amphora.AmphoraClientCli.*;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.carbynestack.cli.client.amphora.AmphoraClientFactory;
import io.carbynestack.cli.client.amphora.config.AmphoraClientCliCommandConfig;
import io.carbynestack.cli.converter.UUIDTypeConverter;
import io.vavr.control.Option;
import java.util.List;
import java.util.UUID;

@Parameters(
    resourceBundle = AMPHORA_MESSAGE_BUNDLE,
    commandDescriptionKey = "get.command-description")
public class GetSecretAmphoraClientCliCommandConfig extends AmphoraClientCliCommandConfig {
  public static final String COMMAND_NAME = "get-secret";

  @Parameter(
      descriptionKey = "get.parameter.secretid-description",
      converter = UUIDTypeConverter.class,
      required = true,
      arity = 1)
  private List<UUID> secretId;

  public GetSecretAmphoraClientCliCommandConfig(Option<AmphoraClientFactory> customClientFactory) {
    super(customClientFactory);
  }

  public UUID getSecretId() {
    return secretId.get(0);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }
}
