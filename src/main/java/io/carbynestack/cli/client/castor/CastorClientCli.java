/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.castor;

import io.carbynestack.cli.CsClientCli;
import io.carbynestack.cli.CsClientCliCommand;
import io.carbynestack.cli.client.castor.command.ActivateChunkCastorClientCliCommandRunner;
import io.carbynestack.cli.client.castor.command.GetCastorTelemetryCliCommandRunner;
import io.carbynestack.cli.client.castor.command.UploadTupleCastorClientCliCommandRunner;
import io.carbynestack.cli.client.castor.command.config.ActivateChunkCastorClientCliCommandConfig;
import io.carbynestack.cli.client.castor.command.config.GetCastorTelemetryCliCommandConfig;
import io.carbynestack.cli.client.castor.command.config.UploadTupleCastorClientCliCommandConfig;
import io.carbynestack.cli.client.castor.config.CastorClientCliConfig;
import io.vavr.control.Option;
import java.util.Arrays;

public class CastorClientCli extends CsClientCli {

  public static final String CASTOR_MESSAGE_BUNDLE = "CastorMessageBundle";

  public CastorClientCli(
      CastorClientCliConfig config,
      boolean debug,
      Option<CastorUploadClientFactory> uploadClientFactory,
      Option<CastorIntraVcpClientFactory> telemetryClientFactory) {
    super(config, debug);
    Arrays.asList(
            new CsClientCliCommand(
                new UploadTupleCastorClientCliCommandConfig(
                    uploadClientFactory, telemetryClientFactory),
                UploadTupleCastorClientCliCommandRunner.class),
            new CsClientCliCommand(
                new GetCastorTelemetryCliCommandConfig(uploadClientFactory, telemetryClientFactory),
                GetCastorTelemetryCliCommandRunner.class),
            new CsClientCliCommand(
                new ActivateChunkCastorClientCliCommandConfig(
                    uploadClientFactory, telemetryClientFactory),
                ActivateChunkCastorClientCliCommandRunner.class))
        .forEach(this::addCommand);
  }

  public CastorClientCli(CastorClientCliConfig config, boolean debug) {
    this(config, debug, Option.none(), Option.none());
  }
}
