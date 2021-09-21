/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.config;

import static io.carbynestack.cli.CsCliApplication.*;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = CS_CLI_MESSAGE_BUNDLE)
public abstract class CsClientCliCommandConfig {
  // all fields are hidden since help is already displayed on top level but should also be available
  // on command level

  @Parameter(
      names = "--help",
      descriptionKey = "option.help-description",
      help = true,
      hidden = true)
  private boolean help;

  public boolean isHelp() {
    return help;
  }

  public abstract String getCommandName();
}
