/*
 * Copyright (c) 2021-2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command;

import io.carbynestack.amphora.client.Secret;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.amphora.common.exceptions.IntegrityVerificationException;
import io.carbynestack.cli.client.amphora.command.config.GetSecretAmphoraClientCliCommandConfig;
import io.carbynestack.cli.client.amphora.util.SecretPrinter;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetSecretAmphoraClientCliCommandRunner
    extends AmphoraClientCliCommandRunner<GetSecretAmphoraClientCliCommandConfig> {

  public GetSecretAmphoraClientCliCommandRunner(GetSecretAmphoraClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    try {
      Secret result = this.getAmphoraClient().getSecret(this.getConfig().getSecretId());
      log.debug(getMessages().getString("get.log.success"), result);
      System.out.println(SecretPrinter.secretToString(result));
    } catch (AmphoraClientException | IntegrityVerificationException e) {
      throw new CsCliRunnerException("Failed fetching secret.", e);
    }
  }
}
