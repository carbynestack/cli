/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.castor.command;

import io.carbynestack.castor.common.entities.TupleChunk;
import io.carbynestack.castor.common.exceptions.CastorClientException;
import io.carbynestack.cli.client.castor.command.config.UploadTupleCastorClientCliCommandConfig;
import io.carbynestack.cli.client.castor.util.TupleFileParser;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.carbynestack.cli.util.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UploadTupleCastorClientCliCommandRunner
    extends CastorClientCliCommandRunner<UploadTupleCastorClientCliCommandConfig> {

  public UploadTupleCastorClientCliCommandRunner(UploadTupleCastorClientCliCommandConfig config)
      throws CsCliConfigurationException, CsCliRunnerException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    try {
      TupleChunk chunk =
          TupleFileParser.parse(
              this.getConfig().getTupleType(),
              this.getConfig().getTupleFile(),
              this.getConfig().getChunkId());
      Retry.retry(
          3,
          () -> {
            castorUploadClient.connectWebSocket(CASTOR_COMMUNICATION_TIMEOUT);
            log.debug(
                String.format(
                    "Uploading %d tuples to chunk #%s...",
                    chunk.getNumberOfTuples(), chunk.getChunkId()));
            if (castorUploadClient.uploadTupleChunk(chunk)) {
              log.debug(getMessages().getString("upload.log.success"));
              System.out.println(getMessages().getString("upload.log.success"));
              return null;
            } else {
              throw new CsCliRunnerException("Failed uploading tuples.");
            }
          });
    } catch (CastorClientException e) {
      log.error(getMessages().getString("upload.log.failure"));
      throw new CsCliRunnerException(e.getMessage(), e);
    } finally {
      castorUploadClient.disconnectWebSocket();
    }
  }
}
