/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import io.vavr.control.Option;
import io.vavr.control.Try;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

@Slf4j
@UtilityClass
public class BrowserLauncher {

  public enum BrowserLaunchError implements AuthenticationError {

    /** Browser launch process terminated with a non-zero exit code. */
    NON_ZERO_EXIT_CODE,

    /** Interrupted while waiting for the browser launch process to finish. */
    INTERRUPTED,

    /** No launch method available for the platform. */
    NOT_SUPPORTED,

    /** Launch failed for unexpected reason. */
    GENERIC
  }

  private static final Runtime RUNTIME = Runtime.getRuntime();
  private static final Option<Desktop> DESKTOP =
      Desktop.isDesktopSupported() ? Option.some(Desktop.getDesktop()) : Option.none();
  private static final String XDG_OPEN_COMMAND = "xdg-open";

  private static boolean isXdgOpenSupported() {
    try {
      return RUNTIME.exec(new String[] {"which", XDG_OPEN_COMMAND}).getInputStream().read() != -1;
    } catch (IOException ioe) {
      log.error("check for xdg-open supported failed", ioe);
      return false;
    }
  }

  public static Option<BrowserLaunchError> browse(URI url) {
    log.debug("launching browser with URL {}", url);
    try {
      if (SystemUtils.IS_OS_LINUX) {
        if (isXdgOpenSupported()) {
          Process process = RUNTIME.exec(new String[] {XDG_OPEN_COMMAND, url.toString()});
          return Try.of(process::waitFor)
              .onSuccess(
                  c ->
                      log.debug("launched browser using {} with exit code {}", XDG_OPEN_COMMAND, c))
              .toEither()
              .mapLeft(t -> Option.some(BrowserLaunchError.INTERRUPTED))
              .fold(
                  o -> o,
                  c -> c == 0 ? Option.none() : Option.some(BrowserLaunchError.NON_ZERO_EXIT_CODE));
        }
      } else if (DESKTOP.isDefined()) {
        DESKTOP.get().browse(url);
        log.debug("launched browser using Java Desktop facility");
        return Option.none();
      }
      return Option.some(BrowserLaunchError.NOT_SUPPORTED);
    } catch (IOException ioe) {
      log.error(String.format("Failed to open browser for URL %s", url), ioe);
      return Option.some(BrowserLaunchError.GENERIC);
    }
  }
}
