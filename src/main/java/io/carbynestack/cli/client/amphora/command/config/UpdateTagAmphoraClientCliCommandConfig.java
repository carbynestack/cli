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
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.cli.client.amphora.AmphoraClientFactory;
import io.carbynestack.cli.client.amphora.config.AmphoraClientCliCommandConfig;
import io.carbynestack.cli.client.amphora.converter.TagTypeConverter;
import io.carbynestack.cli.converter.UUIDTypeConverter;
import io.vavr.control.Option;
import java.util.List;
import java.util.UUID;

@Parameters(
    resourceBundle = AMPHORA_MESSAGE_BUNDLE,
    commandDescriptionKey = "update-tag.command-description")
public class UpdateTagAmphoraClientCliCommandConfig extends AmphoraClientCliCommandConfig {

  public static final String COMMAND_NAME = "update-tag";

  @Parameter(
      names = {"-i", "--secret-id"},
      descriptionKey = "update-tag.option.secret-id-description",
      converter = UUIDTypeConverter.class,
      required = true)
  private UUID secretId;

  @Parameter(
      descriptionKey = "update-tag.parameter.tag-description",
      converter = TagTypeConverter.class,
      required = true,
      arity = 1)
  private List<Tag> tag;

  public UpdateTagAmphoraClientCliCommandConfig(Option<AmphoraClientFactory> customClientFactory) {
    super(customClientFactory);
  }

  public UUID getSecretId() {
    if (secretId == null) {
      this.secretId = UUID.randomUUID();
    }
    return secretId;
  }

  public Tag getTag() {
    return tag.get(0);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }
}
