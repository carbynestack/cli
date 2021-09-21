/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class RetryTest {

  @Test(expected = IllegalArgumentException.class)
  public void givenNonPositiveNumberOfTries_whenRetrying_thenThrows() throws Exception {
    Retry.retry(-1, Object::new);
  }

  @Test
  public void givenCalleeFailsNTimes_whenRetryingNMinus1Times_thenThrowAfterNMinus1Tries() {
    AtomicInteger tried = new AtomicInteger(0);
    int tries = 5;
    try {
      Retry.retry(
          tries,
          () -> {
            if (tried.getAndIncrement() < tries) {
              throw new RuntimeException();
            }
            return null;
          });
      fail("expected exception was not thrown");
    } catch (Exception e) {
      assertEquals("wrong number of tries", tries, tried.get());
    }
  }

  @Test
  public void givenCalleeFailsNMinus1Times_whenRetryingNTimes_thenReturnAfterNTries()
      throws Exception {
    AtomicInteger tried = new AtomicInteger(0);
    int tries = 5;
    Object retVal = new Object();
    Object result =
        Retry.retry(
            tries,
            () -> {
              if (tried.getAndIncrement() < tries - 1) {
                throw new RuntimeException();
              }
              return retVal;
            });
    assertEquals("secret returned does not match returned secret", retVal, result);
    assertEquals("wrong number of tries", tries, tried.get());
  }
}
