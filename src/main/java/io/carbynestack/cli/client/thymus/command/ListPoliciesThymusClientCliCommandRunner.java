/*
 * Copyright (c) 2025 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus.command;

import io.carbynestack.cli.client.thymus.command.config.ListPoliciesThymusClientCliCommandConfig;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.thymus.client.NamespacedName;
import java.text.MessageFormat;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class ListPoliciesThymusClientCliCommandRunner
    extends ThymusClientCliCommandRunner<ListPoliciesThymusClientCliCommandConfig> {

  public ListPoliciesThymusClientCliCommandRunner(ListPoliciesThymusClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    val result =
        client
            .getPolicies()
            .toTry()
            .getOrElseThrow(
                t ->
                    new CsCliRunnerException(
                        MessageFormat.format(
                            getMessages().getString("list-policies.failure"), t.toString())));
    if (result.isLeft()) {
      var error = result.getLeft();
      throw new CsCliRunnerException(
          MessageFormat.format(getMessages().getString("list-policies.failure"), error.toString()));
    }
    for (NamespacedName name : result.get()) {
      System.out.println(name.toString());
    }
  }
}
