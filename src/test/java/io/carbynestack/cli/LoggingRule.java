/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.rules.ExternalResource;

public class LoggingRule extends ExternalResource {
  private final ListAppender listAppender = new ListAppender();

  public String getLog() {
    return this.listAppender.getEvents().stream()
        .map(LoggingEvent::getRenderedMessage)
        .collect(Collectors.joining("\n"));
  }

  @Override
  protected void before() throws Throwable {
    Logger.getRootLogger().addAppender(listAppender);
  }

  @Override
  protected void after() {
    Logger.getRootLogger().removeAppender(listAppender);
    listAppender.clear();
  }

  private static class ListAppender extends AppenderSkeleton {
    private final List<LoggingEvent> events = new ArrayList<>();

    public ListAppender() {
      this.setName(RandomStringUtils.randomAlphabetic(20));
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
      this.events.add(loggingEvent);
    }

    public List<LoggingEvent> getEvents() {
      return new ArrayList<>(this.events);
    }

    public void clear() {
      this.events.clear();
    }

    @Override
    public void close() {
      // nothing to close
    }

    @Override
    public boolean requiresLayout() {
      return false;
    }
  }
}
