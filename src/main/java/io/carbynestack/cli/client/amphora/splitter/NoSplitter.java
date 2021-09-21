/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.splitter;

import com.beust.jcommander.converters.IParameterSplitter;
import java.util.Collections;
import java.util.List;

/** This custom splitter makes sure that list attributes are not split on commas. */
public class NoSplitter implements IParameterSplitter {
  public List<String> split(String value) {
    return Collections.singletonList(value);
  }
}
