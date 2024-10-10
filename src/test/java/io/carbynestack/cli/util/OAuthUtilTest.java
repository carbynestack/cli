/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.cli.util;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Objects;
import javax.net.ssl.SSLHandshakeException;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OAuthUtilTest {
  private static final String KEY_STORE_PATH =
      Objects.requireNonNull(OAuthUtilTest.class.getClassLoader().getResource("keyStoreA.jks"))
          .getPath();
  private static final String KEY_STORE_A_PASSWORD = "verysecure";
  private static final String CERTIFICATE_PATH =
      Objects.requireNonNull(OAuthUtilTest.class.getClassLoader().getResource("certA.pem"))
          .getPath();
  private static final String GOOGLE_DIRECTIONS_REST_URI =
      "https://maps.googleapis.com/maps/api/directions/json";
  private static final String TEST_ENDPOINT = "/test";
  private static final String SUCCESS_RESPONSE_STRING = "success";

  private boolean initialized = false;
  private URI testUri;

  @Rule
  public WireMockRule wireMockRule =
      new WireMockRule(
          options()
              .dynamicHttpsPort()
              .keystorePath(KEY_STORE_PATH)
              .keystorePassword(KEY_STORE_A_PASSWORD),
          false);

  @Before
  public void setUp() throws URISyntaxException {
    if (!initialized) {
      wireMockRule.stubFor(
          get(urlEqualTo(TEST_ENDPOINT))
              .willReturn(
                  aResponse().withStatus(HttpStatus.SC_OK).withBody(SUCCESS_RESPONSE_STRING)));
      testUri = new URI(String.format("%s%s", wireMockRule.baseUrl(), TEST_ENDPOINT));
    }
    initialized = true;
  }

  @Test
  public void givenServerCertIsUntrusted_whenRequesting_thenThrows() {
    HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.GET, testUri);
    Exception actualException = assertThrows(Exception.class, httpRequest::send);
    Assert.assertTrue(actualException.getCause() instanceof SSLHandshakeException);
  }

  @Test
  public void givenContextConfiguredTrustAll_whenRequesting_thenSucceed()
      throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
          KeyManagementException {
    HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.GET, testUri);
    OAuthUtil.setSslContextForRequestWithConfiguration(httpRequest, true, null);
    HTTPResponse response = httpRequest.send();
    assertEquals(HttpStatus.SC_OK, response.getStatusCode());
    assertEquals(SUCCESS_RESPONSE_STRING, response.getBody().trim());
  }

  @Test
  public void givenServerCertIsTrusted_whenRequesting_thenSucceed()
      throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
          KeyManagementException {
    HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.GET, testUri);
    OAuthUtil.setSslContextForRequestWithConfiguration(
        httpRequest, false, Collections.singletonList(Paths.get(CERTIFICATE_PATH)));
    HTTPResponse response = httpRequest.send();
    assertEquals(HttpStatus.SC_OK, response.getStatusCode());
    assertEquals(SUCCESS_RESPONSE_STRING, response.getBody().trim());
  }

  @Test
  public void givenCustomCertsDefined_whenRequestingServerWithCACert_thenSucceed()
      throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
          KeyManagementException, URISyntaxException {
    HTTPRequest httpRequest =
        new HTTPRequest(HTTPRequest.Method.GET, new URI(GOOGLE_DIRECTIONS_REST_URI));
    OAuthUtil.setSslContextForRequestWithConfiguration(
        httpRequest, false, Collections.singletonList(Paths.get(CERTIFICATE_PATH)));
    HTTPResponse response = httpRequest.send();
    // Google Directions API returns 400 for this request but still validates the certificate
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
  }
}
