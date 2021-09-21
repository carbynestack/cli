/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.Range;

@UtilityClass
public class RangeUtils {

  public static Range<Integer> consumeLower(Range<Integer> r) {
    return Range.between(r.getMinimum() + 1, r.getMaximum());
  }

  public static int getLength(Range<Integer> r) {
    return r.getMaximum() - r.getMinimum() + 1;
  }
}
