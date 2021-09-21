/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.exceptions;

/**
 * This Exception type is thrown if something went wrong during parsing the command line options.
 */
public class CsCliException extends Exception {
  private String helpMessage;

  public CsCliException(String message, Throwable cause, String helpMessage) {
    super(message, cause);

    this.helpMessage = helpMessage;
  }

  public String getHelpMessage() {
    return helpMessage;
  }
}
