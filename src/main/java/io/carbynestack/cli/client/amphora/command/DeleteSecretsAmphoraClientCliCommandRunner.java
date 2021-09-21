/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command;

import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.cli.client.amphora.command.config.DeleteSecretsAmphoraClientCliCommandConfig;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.vavr.control.Either;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteSecretsAmphoraClientCliCommandRunner
    extends AmphoraClientCliCommandRunner<DeleteSecretsAmphoraClientCliCommandConfig> {

  public DeleteSecretsAmphoraClientCliCommandRunner(
      DeleteSecretsAmphoraClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    List<UUID> failedIds =
        this.getConfig().getSecretIds().parallelStream()
            .map(
                id -> {
                  try {
                    this.getAmphoraClient().deleteSecret(id);
                    System.out.println(
                        MessageFormat.format(
                            getMessages().getString("delete-secret.print.success-id"), id));
                    log.debug(getMessages().getString("delete-secret.print.success-id"), id);
                    return Either.right(id);
                  } catch (AmphoraClientException ace) {
                    System.out.println(
                        MessageFormat.format(
                            getMessages().getString("delete-secret.print.fail-id"),
                            id,
                            ace.getMessage()));
                    log.debug(
                        MessageFormat.format(
                            getMessages().getString("delete-secret.print.fail-id"),
                            id,
                            ace.getMessage()),
                        ace);
                  } catch (Exception e) {
                    System.out.println(
                        MessageFormat.format(
                            getMessages().getString("delete-secret.print.fail-unexp-id"),
                            id,
                            "Unexpected exception " + e.getMessage()));
                    log.debug(
                        MessageFormat.format(
                            getMessages().getString("delete-secret.print.fail-unexp-id"),
                            id,
                            "Unexpected exception " + e.getMessage()),
                        e);
                  }
                  return Either.left(id);
                })
            .filter(Either::isLeft)
            .map(Either::getLeft)
            .map(Object::toString)
            .map(UUID::fromString)
            .collect(Collectors.toList());

    if (!failedIds.isEmpty()) {
      throw new CsCliRunnerException("Failed to delete secrets", null);
    } else {
      System.out.println(getMessages().getString("delete-secret.print.success-all"));
      log.debug(getMessages().getString("delete-secret.print.success-all"));
    }
  }
}
