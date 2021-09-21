/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.cli.util;

import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.vavr.control.Option;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public class KeyStoreUtil {
  public static Option<File> tempKeyStoreForPems(List<Path> certs) throws CsCliRunnerException {
    if (certs.isEmpty()) {
      return Option.none();
    }
    try {
      KeyStore tmpKeyStore = KeyStore.getInstance("JKS");
      CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
      tmpKeyStore.load(null);
      for (Path certificate : certs) {
        X509Certificate x509Certificate =
            (X509Certificate)
                certificateFactory.generateCertificate(new FileInputStream(certificate.toFile()));
        tmpKeyStore.setCertificateEntry(
            x509Certificate.getSubjectX500Principal().getName(), x509Certificate);
      }
      File tempFile = File.createTempFile("cs_keystore", ".jks");
      try (FileOutputStream fos = new FileOutputStream(tempFile)) {
        tmpKeyStore.store(fos, "".toCharArray());
      }
      tempFile.deleteOnExit();
      return Option.some(tempFile);
    } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
      throw new CsCliRunnerException("Failed to generate temporary keystore.", e);
    }
  }
}
