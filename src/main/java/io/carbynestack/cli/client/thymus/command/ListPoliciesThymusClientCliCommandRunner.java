/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus.command;

import io.carbynestack.cli.client.thymus.command.config.ListPoliciesThymusClientCliCommandConfig;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.text.MessageFormat;

@Slf4j
public class ListPoliciesThymusClientCliCommandRunner
        extends ThymusClientCliCommandRunner<ListPoliciesThymusClientCliCommandConfig> {

    public ListPoliciesThymusClientCliCommandRunner(ListPoliciesThymusClientCliCommandConfig config)
            throws CsCliRunnerException, CsCliConfigurationException {
        super(config);
    }

    @Override
    public void run() throws CsCliRunnerException {
        ListPoliciesThymusClientCliCommandConfig c = this.getConfig();
        val result = client.getPolicies().toTry().getOrElseThrow(
                t -> new CsCliRunnerException(getMessages().getString("list-policies.failure.invoke"), t)); // TODO Check
        if (result.isLeft()) {
            throw new CsCliRunnerException(
                    MessageFormat.format(
                            getMessages().getString("list-policies.failure.invoke-status-code"),
                            result.getLeft().getResponseCode(),
                            result.getLeft().getMessage()),
                    null);
        }
        val policyNames = result.get();
        System.out.println(policyNames);
    }

}
