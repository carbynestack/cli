/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus.command.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.carbynestack.cli.client.thymus.ThymusClientFactory;
import io.carbynestack.cli.client.thymus.config.ThymusClientCliCommandConfig;
import io.carbynestack.cli.client.thymus.converter.PolicyIDConverter;
import io.carbynestack.thymus.client.NamespacedName;
import io.vavr.control.Option;
import lombok.Getter;

import static io.carbynestack.cli.client.thymus.ThymusClientCli.THYMUS_MESSAGE_BUNDLE;

@Getter
@Parameters(
        resourceBundle = THYMUS_MESSAGE_BUNDLE,
        commandDescriptionKey = "get-policy.command-description")
public class GetPolicyThymusClientCliCommandConfig extends ThymusClientCliCommandConfig {

    public static final String COMMAND_NAME = "get-policy";

    @Parameter(
            descriptionKey = "get.parameter.policyid-description", // TODO: add this key to the resource bundle
            required = true,
            converter = PolicyIDConverter.class,
            arity = 1)
    private NamespacedName policyId;

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    public GetPolicyThymusClientCliCommandConfig(
            Option<ThymusClientFactory> customClientFactory) {
        super(customClientFactory);
    }

}
