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
import io.vavr.control.Option;

@Parameters(
    resourceBundle = CASTOR_MESSAGE_BUNDLE,
    commandDescriptionKey = "download.telemetry.command-description")
public class GetCastorTelemetryCliCommandConfig extends CastorClientCliCommandConfig {
  public static final String COMMAND_NAME = "get-telemetry";

  @Parameter(
      names = {"-i", "--interval"},
      descriptionKey = "download.telemetry.interval-description")
  private String interval;

  public GetCastorTelemetryCliCommandConfig(
      Option<CastorUploadClientFactory> customUploadClientFactory,
      Option<CastorIntraVcpClientFactory> customTelemetryClientFactory) {
    super(customUploadClientFactory, customTelemetryClientFactory);
  }

  public String getInterval() {
    return interval;
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }
}
