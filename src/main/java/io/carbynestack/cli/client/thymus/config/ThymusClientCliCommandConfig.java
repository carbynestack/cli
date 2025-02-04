/*
 * Copyright (c) 2025 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus.config;

import static io.carbynestack.cli.client.thymus.ThymusClientCli.THYMUS_MESSAGE_BUNDLE;

import com.beust.jcommander.Parameters;
import io.carbynestack.cli.client.thymus.ThymusClientFactory;
import io.carbynestack.cli.config.CsClientCliCommandConfig;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Parameters(resourceBundle = THYMUS_MESSAGE_BUNDLE)
public abstract class ThymusClientCliCommandConfig extends CsClientCliCommandConfig {

  private final Option<ThymusClientFactory> customClientFactory;
}
