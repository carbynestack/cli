/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.config;

import static io.carbynestack.cli.client.ephemeral.EphemeralClientCli.EPHEMERAL_MESSAGE_BUNDLE;

import com.beust.jcommander.Parameters;
import io.carbynestack.cli.client.amphora.AmphoraClientFactory;
import io.carbynestack.cli.config.CsClientCliCommandConfig;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Parameters(resourceBundle = EPHEMERAL_MESSAGE_BUNDLE)
public abstract class AmphoraClientCliCommandConfig extends CsClientCliCommandConfig {
  private final Option<AmphoraClientFactory> customClientFactory;
}
