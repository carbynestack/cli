/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.ephemeral;

import io.carbynestack.cli.client.ephemeral.config.EphemeralClientCliCommandConfig;
import io.carbynestack.ephemeral.client.EphemeralMultiClient;

public interface EphemeralClientFactory {
  EphemeralMultiClient create(EphemeralClientCliCommandConfig config);
}
