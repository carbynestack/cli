/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus.config;

import com.beust.jcommander.Parameters;
import io.carbynestack.cli.config.CsClientCliConfig;

import static io.carbynestack.cli.client.thymus.ThymusClientCli.THYMUS_MESSAGE_BUNDLE;

@Parameters(resourceBundle = THYMUS_MESSAGE_BUNDLE, commandDescriptionKey = "client-description")
public class ThymusClientCliConfig extends CsClientCliConfig {

    public static final String CLIENT_NAME = "thymus";

    @Override
    public String getClientName() {
        return CLIENT_NAME;
    }

}
