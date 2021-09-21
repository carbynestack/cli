/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.castor.command;

import io.carbynestack.castor.common.exceptions.CastorClientException;
import io.carbynestack.cli.client.castor.command.config.ActivateChunkCastorClientCliCommandConfig;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActivateChunkCastorClientCliCommandRunner
    extends CastorClientCliCommandRunner<ActivateChunkCastorClientCliCommandConfig> {

  public ActivateChunkCastorClientCliCommandRunner(ActivateChunkCastorClientCliCommandConfig config)
      throws CsCliConfigurationException, CsCliRunnerException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    try {
      log.debug(String.format("Activating tuple chunk #%s...", getConfig().getChunkId()));
      castorUploadClient.activateTupleChunk(getConfig().getChunkId());
      log.error(getMessages().getString("activate.log.success"));
      System.out.println(getMessages().getString("activate.log.success"));

    } catch (CastorClientException cce) {
      log.error(getMessages().getString("activate.log.failure"));
      System.out.println(getMessages().getString("activate.log.failure"));
      throw new CsCliRunnerException(cce.getMessage(), cce);
    }
  }
}
