/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command;

import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.cli.client.amphora.command.config.OverwriteTagsAmphoraClientCliCommandConfig;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OverwriteTagsAmphoraClientCliCommandRunner
    extends AmphoraClientCliCommandRunner<OverwriteTagsAmphoraClientCliCommandConfig> {

  public OverwriteTagsAmphoraClientCliCommandRunner(
      OverwriteTagsAmphoraClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    try {
      this.getAmphoraClient()
          .overwriteTags(this.getConfig().getSecretId(), this.getConfig().getTags());
      log.debug(
          getMessages().getString("overwrite-tags.log.success"), this.getConfig().getSecretId());
      System.out.println(getMessages().getString("overwrite-tags.print.success"));
    } catch (AmphoraClientException ace) {
      throw new CsCliRunnerException(ace.getMessage(), ace);
    }
  }
}
