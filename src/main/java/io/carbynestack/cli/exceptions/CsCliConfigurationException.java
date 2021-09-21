/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.exceptions;

public class CsCliConfigurationException extends Exception {
  public CsCliConfigurationException(String msg) {
    super(msg);
  }

  public CsCliConfigurationException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
