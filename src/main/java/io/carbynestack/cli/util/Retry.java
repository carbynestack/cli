/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.util;

import io.carbynestack.cli.exceptions.CsCliRunnerException;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Retry {

  public static <T> T retry(int numberOfTries, Callable<T> method) throws CsCliRunnerException {
    if (numberOfTries <= 0) {
      throw new IllegalArgumentException("Number of tries must be larger 0.");
    }
    for (int i = 1; i <= numberOfTries; i++) {
      try {
        return method.call();
      } catch (Exception e) {
        log.debug("Failed on attempt #{}: {}", i, e.getMessage());
        if (i == numberOfTries) {
          throw new CsCliRunnerException("Failed on retry.", e);
        }
      }
    }
    throw new CsCliRunnerException("Unexpected behaviour on method execution");
  }
}
