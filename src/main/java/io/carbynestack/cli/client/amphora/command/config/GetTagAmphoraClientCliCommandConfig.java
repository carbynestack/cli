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
import java.util.UUID;

@Parameters(
    resourceBundle = AMPHORA_MESSAGE_BUNDLE,
    commandDescriptionKey = "get-tag.command-description")
public class GetTagAmphoraClientCliCommandConfig extends AmphoraClientCliCommandConfig {

  public static final String COMMAND_NAME = "get-tag";

  @Parameter(
      names = {"-i", "--secret-id"},
      descriptionKey = "get-tag.option.secret-id-description",
      converter = UUIDTypeConverter.class,
      required = true)
  private UUID secretId;

  @Parameter(descriptionKey = "get-tag.parameter.tag-key-description", required = true)
  private String tagKey;

  public GetTagAmphoraClientCliCommandConfig(Option<AmphoraClientFactory> customClientFactory) {
    super(customClientFactory);
  }

  public UUID getSecretId() {
    if (secretId == null) {
      this.secretId = UUID.randomUUID();
    }
    return secretId;
  }

  public String getTagKey() {
    return tagKey;
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }
}
