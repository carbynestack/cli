/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.Range;
import org.junit.Test;

public class RangeUtilTest {

  @Test
  public void givenNonEmptyRange_whenGetLength_thenReturnCorrectResult() {
    int l = RangeUtils.getLength(Range.between(1, 5));
    assertEquals("Range with five elements must have length 5", 5, l);
  }

  @Test
  public void givenOneElementRange_whenGetLength_thenReturnOne() {
    int l = RangeUtils.getLength(Range.between(1, 1));
    assertEquals("One element range must have length 1", 1, l);
  }

  @Test
  public void givenTwoElementRange_whenConsumeLower_thenReturnRangeWithRemainingElement() {
    Range<Integer> r = RangeUtils.consumeLower(Range.between(1, 2));
    assertThat(
        "Range must contain only remaining element",
        r.getMinimum().equals(r.getMaximum()) && r.getMinimum().equals(2));
  }
}
