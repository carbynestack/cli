/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.exceptions;

/**
 * This Exception type is thrown if something went wrong during command execution. (service client
 * internal exception occurred).
 */
public class CsCliRunnerException extends Exception {
  public CsCliRunnerException(String msg) {
    super(msg);
  }

  public CsCliRunnerException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
