/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command;

import io.carbynestack.amphora.client.Secret;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.cli.client.amphora.command.config.CreateSecretAmphoraClientCliCommandConfig;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import java.math.BigInteger;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateSecretAmphoraClientCliCommandRunner
    extends AmphoraClientCliCommandRunner<CreateSecretAmphoraClientCliCommandConfig> {

  public CreateSecretAmphoraClientCliCommandRunner(CreateSecretAmphoraClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    try {
      UUID result =
          this.getAmphoraClient()
              .createSecret(
                  Secret.of(
                      this.getConfig().getSecretId(),
                      this.getConfig().getTags(),
                      this.getConfig().getSecrets().toArray(new BigInteger[0])));
      log.debug(getMessages().getString("create.log.success"), result);
      System.out.println(result);
    } catch (AmphoraClientException e) {
      throw new CsCliRunnerException("Failed to upload secret.", e);
    }
  }
}
