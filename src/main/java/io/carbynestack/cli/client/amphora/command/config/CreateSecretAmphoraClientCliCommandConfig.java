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
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.cli.client.amphora.AmphoraClientFactory;
import io.carbynestack.cli.client.amphora.config.AmphoraClientCliCommandConfig;
import io.carbynestack.cli.client.amphora.converter.TagTypeConverter;
import io.carbynestack.cli.client.amphora.splitter.NoSplitter;
import io.carbynestack.cli.converter.UUIDTypeConverter;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

@Parameters(
    resourceBundle = AMPHORA_MESSAGE_BUNDLE,
    commandDescriptionKey = "create.command-description")
public class CreateSecretAmphoraClientCliCommandConfig extends AmphoraClientCliCommandConfig {
  public static final String COMMAND_NAME = "create-secret";

  @Parameter(descriptionKey = "create.parameter.secret-description")
  private List<String> secrets = Lists.newArrayList();

  @Parameter(
      names = {"-i", "--secret-id"},
      descriptionKey = "create.option.secretid-description",
      converter = UUIDTypeConverter.class,
      order = 1)
  private UUID secretId;

  @Parameter(
      names = {"-t", "--tag"},
      descriptionKey = "create.option.tag-description",
      converter = TagTypeConverter.class,
      splitter = NoSplitter.class,
      order = 2)
  private List<Tag> tags;

  public CreateSecretAmphoraClientCliCommandConfig(
      Option<AmphoraClientFactory> customClientFactory) {
    super(customClientFactory);
  }

  public List<BigInteger> getSecrets() {
    if (secrets.isEmpty()) {
      ResourceBundle messages = ResourceBundle.getBundle(AMPHORA_MESSAGE_BUNDLE);
      System.out.println(messages.getString("create.info.insert-secrets"));
      secrets =
          readSecretsFromStdIn()
              .getOrElseThrow(
                  e ->
                      new IllegalArgumentException(
                          messages.getString("create.failure.read-secrets"), e));
    }
    secrets.removeIf(String::isEmpty);
    return secrets.stream().map(BigInteger::new).collect(Collectors.toList());
  }

  public UUID getSecretId() {
    if (secretId == null) {
      this.secretId = UUID.randomUUID();
    }
    return secretId;
  }

  public List<Tag> getTags() {
    return tags;
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  private Either<Throwable, List<String>> readSecretsFromStdIn() {
    return Try.of(
            () -> {
              String data = new String(ByteStreams.toByteArray(System.in), StandardCharsets.UTF_8);
              if (data.isEmpty()) {
                throw new IllegalArgumentException("No input provided");
              }
              return Arrays.stream(data.split(System.lineSeparator()))
                  .map(String::trim)
                  .collect(Collectors.toList());
            })
        .toEither();
  }
}
