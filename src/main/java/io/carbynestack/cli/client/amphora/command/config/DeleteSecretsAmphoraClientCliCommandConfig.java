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
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import io.carbynestack.cli.client.amphora.AmphoraClientFactory;
import io.carbynestack.cli.client.amphora.config.AmphoraClientCliCommandConfig;
import io.carbynestack.cli.converter.UUIDTypeConverter;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

@Parameters(
    resourceBundle = AMPHORA_MESSAGE_BUNDLE,
    commandDescriptionKey = "delete-secret.command-description")
public class DeleteSecretsAmphoraClientCliCommandConfig extends AmphoraClientCliCommandConfig {
  public static final String COMMAND_NAME = "delete-secrets";

  @Parameter(
      descriptionKey = "delete-secret.parameter.secret-id-description",
      converter = UUIDTypeConverter.class)
  private List<UUID> secretIds = Lists.newArrayList();

  public DeleteSecretsAmphoraClientCliCommandConfig(
      Option<AmphoraClientFactory> customClientFactory) {
    super(customClientFactory);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  public List<UUID> getSecretIds() {
    if (secretIds.isEmpty()) {
      ResourceBundle messages = ResourceBundle.getBundle(AMPHORA_MESSAGE_BUNDLE);
      System.out.println(messages.getString("delete-secret.info.secret-ids"));
      secretIds =
          readSecretsFromStdIn()
              .getOrElseThrow(
                  e ->
                      new IllegalArgumentException(
                          messages.getString("delete-secret.failure.secret-ids"), e));
    }
    return secretIds;
  }

  private Either<Throwable, List<UUID>> readSecretsFromStdIn() {
    return Try.of(
            () -> {
              String data = new String(ByteStreams.toByteArray(System.in), StandardCharsets.UTF_8);
              if (data.isEmpty()) {
                throw new IllegalArgumentException("No input provided");
              }
              return Arrays.stream(data.split(System.lineSeparator()))
                  .map(String::trim)
                  .map(UUID::fromString)
                  .collect(Collectors.toList());
            })
        .toEither();
  }
}
