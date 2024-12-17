/*
 * Copyright (c) 2025 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus.command;

import io.carbynestack.cli.client.thymus.command.config.GetPolicyThymusClientCliCommandConfig;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.thymus.client.NamespacedName;
import java.text.MessageFormat;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class GetPolicyThymusClientCliCommandRunner
    extends ThymusClientCliCommandRunner<GetPolicyThymusClientCliCommandConfig> {

  public GetPolicyThymusClientCliCommandRunner(GetPolicyThymusClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    GetPolicyThymusClientCliCommandConfig c = this.getConfig();
    NamespacedName policyId = c.getPolicyId();
    val result =
        client
            .getPolicy(policyId)
            .toTry()
            .getOrElseThrow(
                t ->
                    new CsCliRunnerException(
                        MessageFormat.format(
                            getMessages().getString("get-policy.failure"), t.toString())));
    if (result.isLeft()) {
      var error = result.getLeft();
      throw new CsCliRunnerException(
          MessageFormat.format(getMessages().getString("get-policy.failure"), error.toString()));
    }
    val policy = result.get();
    System.out.println(policy.getSource());
  }
}
