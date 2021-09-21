/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.castor;

import io.carbynestack.castor.client.download.CastorIntraVcpClient;

public interface CastorIntraVcpClientFactory {
  CastorIntraVcpClient create();
}
