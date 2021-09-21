/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.util;

import static io.carbynestack.cli.configuration.ConfigurationCommand.CONFIGURATION_MESSAGE_BUNDLE;

import java.util.ResourceBundle;
import java.util.Scanner;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.NONE)
@Slf4j
public class ConsoleReader {

  private static final ResourceBundle MESSAGES =
      ResourceBundle.getBundle(CONFIGURATION_MESSAGE_BUNDLE);

  public static String readOrDefault(String def) {
    Scanner scanner = new Scanner(System.in);
    String in = "";
    try {
      in = scanner.nextLine();
    } catch (Exception e) {
      log.error(MESSAGES.getString("read-input.failed"), e);
    }
    return in.isEmpty() ? def : in;
  }
}
