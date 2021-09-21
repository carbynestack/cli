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
import io.carbynestack.castor.common.entities.TupleType;
import io.carbynestack.cli.client.castor.CastorIntraVcpClientFactory;
import io.carbynestack.cli.client.castor.CastorUploadClientFactory;
import io.carbynestack.cli.client.castor.config.CastorClientCliCommandConfig;
import io.carbynestack.cli.converter.UUIDTypeConverter;
import io.vavr.control.Option;
import java.io.File;
import java.util.UUID;

@Parameters(
    resourceBundle = CASTOR_MESSAGE_BUNDLE,
    commandDescriptionKey = "upload.command-description")
public class UploadTupleCastorClientCliCommandConfig extends CastorClientCliCommandConfig {
  public static final String COMMAND_NAME = "upload-tuple";

  @Parameter(
      names = {"-f", "--tuple-file"},
      descriptionKey = "upload.tuple-file-description",
      required = true)
  private File tupleFile;

  public UploadTupleCastorClientCliCommandConfig(
      Option<CastorUploadClientFactory> customUploadClientFactory,
      Option<CastorIntraVcpClientFactory> customTelemetryClientFactory) {
    super(customUploadClientFactory, customTelemetryClientFactory);
  }

  public File getTupleFile() {
    return tupleFile;
  }

  @Parameter(
      names = {"-t", "--tuple-type"},
      descriptionKey = "upload.tuple-type-description",
      required = true)
  private String tupleType;

  public TupleType getTupleType() {
    return TupleType.valueOf(tupleType);
  }

  @Parameter(
      names = {"-i", "--chunk-id"},
      descriptionKey = "upload.option.chunk-id",
      converter = UUIDTypeConverter.class,
      order = 2)
  private UUID chunkId;

  public UUID getChunkId() {
    if (chunkId == null) {
      chunkId = UUID.randomUUID();
    }
    return chunkId;
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }
}
