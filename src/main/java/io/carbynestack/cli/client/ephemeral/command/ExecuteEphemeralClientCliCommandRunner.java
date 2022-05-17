/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.ephemeral.command;

import com.google.common.io.ByteStreams;
import io.carbynestack.cli.client.ephemeral.command.config.ExecuteEphemeralClientCliCommandConfig;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.ephemeral.client.ActivationError;
import io.carbynestack.ephemeral.client.ActivationResult;
import io.vavr.concurrent.Future;
import io.vavr.control.Either;
import io.vavr.control.Try;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteEphemeralClientCliCommandRunner
    extends EphemeralClientCliCommandRunner<ExecuteEphemeralClientCliCommandConfig> {

  public ExecuteEphemeralClientCliCommandRunner(ExecuteEphemeralClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException {
    super(config);
  }

  private Either<Throwable, String> readCodeFromStdIn() {
    return Try.of(
            () -> {
              byte[] data = ByteStreams.toByteArray(System.in);
              return new String(data, StandardCharsets.UTF_8);
            })
        .toEither();
  }

  @Override
  public void run() throws CsCliRunnerException {
    System.out.println("Provide program to execute. Press Ctrl+D to submit.");
    ExecuteEphemeralClientCliCommandConfig c = this.getConfig();

    // Both input options provided
    if (c.getTagFilters().size() != 0 && c.getInputs().size() != 0) {
      throw new CsCliRunnerException(
          getMessages().getString("execute.failure.both-inputs-provided"));
    }

    String code =
        readCodeFromStdIn()
            .getOrElseThrow(
                e ->
                    new CsCliRunnerException(
                        getMessages().getString("execute.failure.read-code"), e));

    Future<Either<ActivationError, List<ActivationResult>>> execute;
    if (c.getTagFilters().size() != 0) {
      execute = client.executeWithTags(code, c.getTagFilters());
    } else {
      execute = client.execute(code, c.getInputs());
    }

    Either<ActivationError, List<ActivationResult>> result =
        execute
            .toTry()
            .getOrElseThrow(
                t ->
                    new CsCliRunnerException(getMessages().getString("execute.failure.invoke"), t));
    if (result.isLeft()) {
      throw new CsCliRunnerException(
          MessageFormat.format(
              getMessages().getString("execute.failure.invoke-status-code"),
              result.getLeft().getResponseCode(),
              result.getLeft().getMessage()),
          null);
    } else {
      try {
        List<UUID> reducedResults =
            result.get().stream()
                .map(ActivationResult::getResponse)
                .reduce(
                    result.get().get(0).getResponse(),
                    (a, b) -> {
                      if (a.size() != b.size() || !a.containsAll(b)) {
                        throw new RuntimeException(
                            getMessages().getString("execute.failure.invoke-different-results"));
                      }
                      return a;
                    });
        log.debug(
            MessageFormat.format(getMessages().getString("execute.log.success"), reducedResults));
        System.out.println(reducedResults);
      } catch (Exception e) {
        throw new CsCliRunnerException(e.getMessage(), e);
      }
    }
  }
}
