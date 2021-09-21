/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;

public class LogUtils {

  public static ListAppender getAppender(String name) {
    LoggerContext context = LoggerContext.getContext(false);
    org.apache.logging.log4j.core.Logger logger = context.getRootLogger();
    return (ListAppender) logger.getAppenders().get(name);
  }

  public static void clearLogs() {
    Stream.of("STDERR", "WARNINGS").forEach(n -> getAppender(n).clear());
  }

  public static String getLog(String name) {
    return getAppender(name).getEvents().stream()
        .map(e -> e.getMessage().getFormattedMessage())
        .collect(Collectors.joining("\n"));
  }
}
