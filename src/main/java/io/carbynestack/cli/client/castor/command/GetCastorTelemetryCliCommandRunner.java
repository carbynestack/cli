/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.castor.command;

import io.carbynestack.castor.common.entities.TelemetryData;
import io.carbynestack.castor.common.exceptions.CastorClientException;
import io.carbynestack.cli.client.castor.command.config.GetCastorTelemetryCliCommandConfig;
import io.carbynestack.cli.client.castor.util.TelemetryPrinter;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import java.text.MessageFormat;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetCastorTelemetryCliCommandRunner
    extends CastorClientCliCommandRunner<GetCastorTelemetryCliCommandConfig> {

  public GetCastorTelemetryCliCommandRunner(GetCastorTelemetryCliCommandConfig config)
      throws CsCliConfigurationException, CsCliRunnerException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    try {
      TelemetryData result;
      String interval = this.getConfig().getInterval();
      if (interval != null && 0 < Long.parseLong(interval)) {
        result = castorIntraVcpClient.getTelemetryData(Long.parseLong(interval));
      } else {
        result = castorIntraVcpClient.getTelemetryData();
      }
      log.debug(
          MessageFormat.format(getMessages().getString("download.telemetry.log.success"), result));
      System.out.println(TelemetryPrinter.telemetryDataToString(result));
    } catch (CastorClientException e) {
      log.error(getMessages().getString("download.telemetry.log.failure"), e);
      throw new CsCliRunnerException("Failed fetching telemetry data", e);
    }
  }
}
