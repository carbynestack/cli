/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.converter;

import com.beust.jcommander.ParameterException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

public class PathTypeConverterTest {

  @Test(expected = ParameterException.class)
  public void givenInvalidPath_whenConverting_thenThrows() {
    PathTypeConverter c = new PathTypeConverter();
    // Anything containing the 'null' character will fail
    c.convert(RandomStringUtils.random(10) + '\0');
  }
}
