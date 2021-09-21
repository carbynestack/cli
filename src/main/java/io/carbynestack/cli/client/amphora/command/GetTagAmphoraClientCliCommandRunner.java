/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command;

import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.cli.client.amphora.command.config.GetTagAmphoraClientCliCommandConfig;
import io.carbynestack.cli.client.amphora.util.SecretPrinter;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetTagAmphoraClientCliCommandRunner
    extends AmphoraClientCliCommandRunner<GetTagAmphoraClientCliCommandConfig> {

  public GetTagAmphoraClientCliCommandRunner(GetTagAmphoraClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    try {
      Tag result =
          this.getAmphoraClient()
              .getTag(this.getConfig().getSecretId(), this.getConfig().getTagKey());
      log.debug(getMessages().getString("get-tag.log.success"), result);
      System.out.println(SecretPrinter.tagToString(result));
    } catch (AmphoraClientException ace) {
      throw new CsCliRunnerException(ace.getMessage(), ace);
    }
  }
}
