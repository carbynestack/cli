/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command;

import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.cli.client.amphora.command.config.GetTagsAmphoraClientCliCommandConfig;
import io.carbynestack.cli.client.amphora.util.SecretPrinter;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetTagsAmphoraClientCliCommandRunner
    extends AmphoraClientCliCommandRunner<GetTagsAmphoraClientCliCommandConfig> {

  public GetTagsAmphoraClientCliCommandRunner(GetTagsAmphoraClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    try {
      List<Tag> result = this.getAmphoraClient().getTags(this.getConfig().getSecretId());
      log.debug(getMessages().getString("get-tags.log.success"), result);
      System.out.println(SecretPrinter.tagsToString(result));
    } catch (AmphoraClientException ace) {
      throw new CsCliRunnerException(ace.getMessage(), ace);
    }
  }
}
