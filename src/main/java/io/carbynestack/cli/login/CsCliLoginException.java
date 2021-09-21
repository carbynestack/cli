/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

/** Indicates that something went wrong during login command execution. */
public class CsCliLoginException extends Exception {

  private final AuthenticationError authenticationError;

  public CsCliLoginException(AuthenticationError authenticationError) {
    super(authenticationError.toString());
    this.authenticationError = authenticationError;
  }

  public AuthenticationError getAuthenticationError() {
    return authenticationError;
  }
}
