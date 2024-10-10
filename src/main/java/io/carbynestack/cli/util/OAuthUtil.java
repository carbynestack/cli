/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.cli.util;

import static java.util.stream.Collectors.toList;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import io.carbynestack.httpclient.CompositeX509TrustManager;
import io.carbynestack.httpclient.ExtendedSSLContextBuilder;
import io.carbynestack.httpclient.X509TrustManagerUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;

public class OAuthUtil {
  private OAuthUtil() {}

  public static void setSslContextForRequestWithConfiguration(
      HTTPRequest httpRequest, boolean insecure, List<Path> trustedCertificates)
      throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException,
          CertificateException, IOException {
    if (insecure) {
      SSLContext sslContext =
          new SSLContextBuilder().loadTrustMaterial(null, new TrustAllStrategy()).build();
      httpRequest.setSSLSocketFactory(sslContext.getSocketFactory());
      httpRequest.setHostnameVerifier(new NoopHostnameVerifier());
    } else if (trustedCertificates != null && !trustedCertificates.isEmpty()) {
      List<File> certFiles = trustedCertificates.stream().map(Path::toFile).collect(toList());
      List<Optional<X509TrustManager>> custom =
          Collections.singletonList(X509TrustManagerUtils.getX509TrustManager(certFiles));
      List<X509TrustManager> allTrustManagers =
          Stream.concat(
                  custom.stream(), Stream.of(X509TrustManagerUtils.getDefaultX509TrustManager()))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(toList());
      SSLContextBuilder sslContextBuilder =
          ExtendedSSLContextBuilder.create(new CompositeX509TrustManager(allTrustManagers));
      httpRequest.setSSLSocketFactory(sslContextBuilder.build().getSocketFactory());
    }
  }
}
