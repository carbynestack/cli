/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.config;

import com.beust.jcommander.Parameter;
import java.util.List;

public abstract class CsClientCliConfig {
  @Parameter(description = "<args>")
  private List<String> args;

  public String[] getArgs() {
    return args == null ? new String[0] : args.toArray(new String[args.size()]);
  }

  /**
   * @return Carbyne Stack Service name
   */
  public abstract String getClientName();
}
