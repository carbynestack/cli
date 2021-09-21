/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.converter;

import static io.carbynestack.cli.CsCliApplication.*;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class PathTypeConverter implements IStringConverter<Path> {

  private final ResourceBundle messages = ResourceBundle.getBundle(CS_CLI_MESSAGE_BUNDLE);

  @Override
  public Path convert(String value) {
    try {
      return Paths.get(value);
    } catch (Exception e) {
      throw new ParameterException(
          MessageFormat.format(messages.getString("path.conversion.exception"), value));
    }
  }
}
