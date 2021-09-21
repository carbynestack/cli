/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.castor.util;

import io.carbynestack.castor.common.entities.TelemetryData;
import io.carbynestack.castor.common.entities.TupleMetric;

public class TelemetryPrinter {

  public static String telemetryDataToString(TelemetryData telemetryData) {
    StringBuilder outputBuilder = new StringBuilder();
    for (TupleMetric metric : telemetryData.getMetrics()) {
      outputBuilder.append(metric.getType().toString()).append("\n");
      outputBuilder.append("\t").append("available:\t").append(metric.getAvailable()).append("\n");
      outputBuilder
          .append("\t")
          .append("consumption/s:\t")
          .append(metric.getConsumptionRate())
          .append("\n");
      outputBuilder.append("\n");
    }
    outputBuilder.append("interval: ").append(telemetryData.getInterval());
    return outputBuilder.toString();
  }
}
