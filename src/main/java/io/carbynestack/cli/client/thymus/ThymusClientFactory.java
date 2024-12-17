/*
 * Copyright (c) 2025 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus;

import io.carbynestack.cli.client.thymus.config.ThymusClientCliCommandConfig;
import io.carbynestack.thymus.client.ThymusVCClient;

public interface ThymusClientFactory {
  ThymusVCClient create(ThymusClientCliCommandConfig config);
}
