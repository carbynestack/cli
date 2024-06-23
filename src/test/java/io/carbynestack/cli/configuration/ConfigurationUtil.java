/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.configuration;

import io.carbynestack.amphora.common.AmphoraServiceUri;
import io.carbynestack.castor.common.CastorServiceUri;
import lombok.SneakyThrows;

import java.net.URI;
import java.util.UUID;

public class ConfigurationUtil {

    private static final String VCP_1_BASE_URL = "http://apollo.example.com";
    private static final String VCP_2_BASE_URL = "http://starbuck.example.com";

    @SneakyThrows
    public static Configuration getConfiguration(String vcp1BaseUrl, String vcp2BaseUrl) {
        Configuration configuration = new Configuration();
        configuration.getProvider(1).baseUrl = URI.create(VCP_1_BASE_URL);
        configuration.getProvider(1).amphoraServiceUri =
                new AmphoraServiceUri(String.format("%s/amphora", vcp1BaseUrl));
        configuration.getProvider(1).castorServiceUri =
                new CastorServiceUri(String.format("%s/castor", vcp1BaseUrl));
        configuration.getProvider(1).ephemeralServiceUrl = URI.create(String.format("%s", vcp1BaseUrl));
        configuration.getProvider(1).oAuth2clientId = UUID.randomUUID().toString();
        configuration.getProvider(1).oAuth2CallbackUrl = URI.create("http://localhost/vcp-1");
        configuration.getProvider(1).oAuth2AuthEndpointUri =
                URI.create(String.format("%s/auth2/auth", vcp1BaseUrl));
        configuration.getProvider(1).oAuth2TokenEndpointUri =
                URI.create(String.format("%s/auth2/token", vcp1BaseUrl));
        configuration.getProvider(2).baseUrl = URI.create(VCP_2_BASE_URL);
        configuration.getProvider(2).amphoraServiceUri =
                new AmphoraServiceUri(String.format("%s/amphora", vcp2BaseUrl));
        configuration.getProvider(2).castorServiceUri =
                new CastorServiceUri(String.format("%s/castor", vcp2BaseUrl));
        configuration.getProvider(2).ephemeralServiceUrl = URI.create(String.format("%s", vcp2BaseUrl));
        configuration.getProvider(2).oAuth2clientId = UUID.randomUUID().toString();
        configuration.getProvider(2).oAuth2CallbackUrl = URI.create("http://localhost/vcp-2");
        configuration.getProvider(2).oAuth2AuthEndpointUri =
                URI.create(String.format("%s/auth2/auth", vcp2BaseUrl));
        configuration.getProvider(2).oAuth2TokenEndpointUri =
                URI.create(String.format("%s/auth2/token", vcp2BaseUrl));
        return configuration;
    }

    public static Configuration getConfiguration() {
        return getConfiguration(VCP_1_BASE_URL, VCP_2_BASE_URL);
    }
}
