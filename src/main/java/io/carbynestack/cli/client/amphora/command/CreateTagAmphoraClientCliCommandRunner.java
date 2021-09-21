/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command;

import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.cli.client.amphora.command.config.CreateTagAmphoraClientCliCommandConfig;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateTagAmphoraClientCliCommandRunner
    extends AmphoraClientCliCommandRunner<CreateTagAmphoraClientCliCommandConfig> {

  public CreateTagAmphoraClientCliCommandRunner(CreateTagAmphoraClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    try {
      this.getAmphoraClient().createTag(this.getConfig().getSecretId(), this.getConfig().getTag());
      log.debug(
          getMessages().getString("create-tag.log.success"),
          this.getConfig().getTag().getKey(),
          this.getConfig().getTag().getValue(),
          this.getConfig().getSecretId());
      System.out.println(getMessages().getString("create-tag.print.success"));
    } catch (AmphoraClientException e) {
      throw new CsCliRunnerException(e.getMessage(), e);
    }
  }
}
