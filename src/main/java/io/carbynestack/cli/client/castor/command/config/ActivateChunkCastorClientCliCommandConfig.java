/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.castor.command.config;

import static io.carbynestack.cli.client.castor.CastorClientCli.*;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.carbynestack.cli.client.castor.CastorIntraVcpClientFactory;
import io.carbynestack.cli.client.castor.CastorUploadClientFactory;
import io.carbynestack.cli.client.castor.config.CastorClientCliCommandConfig;
import io.carbynestack.cli.converter.UUIDTypeConverter;
import io.vavr.control.Option;
import java.util.UUID;

@Parameters(
    resourceBundle = CASTOR_MESSAGE_BUNDLE,
    commandDescriptionKey = "upload.command-description")
public class ActivateChunkCastorClientCliCommandConfig extends CastorClientCliCommandConfig {
  public static final String COMMAND_NAME = "activate-chunk";

  @Parameter(
      names = {"-i", "--chunk-id"},
      descriptionKey = "activate.option.chunk-id",
      converter = UUIDTypeConverter.class,
      required = true)
  private UUID chunkId;

  public ActivateChunkCastorClientCliCommandConfig(
      Option<CastorUploadClientFactory> customUploadClientFactory,
      Option<CastorIntraVcpClientFactory> customTelemetryClientFactory) {
    super(customUploadClientFactory, customTelemetryClientFactory);
  }

  public UUID getChunkId() {
    return chunkId;
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }
}
