/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command;

import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.cli.client.amphora.command.config.ListSecretsAmphoraClientCliCommandConfig;
import io.carbynestack.cli.client.amphora.util.SecretPrinter;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ListSecretsAmphoraClientCliCommandRunner
    extends AmphoraClientCliCommandRunner<ListSecretsAmphoraClientCliCommandConfig> {

  public ListSecretsAmphoraClientCliCommandRunner(ListSecretsAmphoraClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    try {
      List<Metadata> result = this.getAmphoraClient().getSecrets();
      log.debug(getMessages().getString("list.log.success"), result);
      System.out.println(SecretPrinter.metadataListToString(result, getConfig().isIdsOnly()));
    } catch (AmphoraClientException ace) {
      throw new CsCliRunnerException(ace.getMessage(), ace);
    }
  }
}
