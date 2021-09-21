/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.configuration;

import static io.carbynestack.cli.config.CsCliConfig.APPLICATION_TITLE;
import static io.carbynestack.cli.configuration.ConfigurationCommand.*;
import static io.carbynestack.cli.util.ConsoleReader.readOrDefault;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableList;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Setter
@Accessors(chain = true)
@Slf4j
public class Configuration {
  private static final BigInteger DEFAULT_PRIME =
      new BigInteger("198766463529478683931867765928436695041");
  private static final BigInteger DEFAULT_R =
      new BigInteger("141515903391459779531506841503331516415");
  private static final BigInteger DEFAULT_R_INV =
      new BigInteger("133854242216446749056083838363708373830");
  private static final String PRIME_ENV_KEY = "CS_PRIME";
  private static final String R_ENV_KEY = "CS_R";
  private static final String R_INV_ENV_KEY = "CS_R_INV";
  private static final String NO_SSL_VALIDATION_ENV_KEY = "CS_NO_SSL_VALIDATION";
  static final String CONFIG_FOLDER_PATH = ".cs";
  static final String CONFIG_FILE_NAME = "config";
  private static final ResourceBundle MESSAGES =
      ResourceBundle.getBundle(CONFIGURATION_MESSAGE_BUNDLE);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final ObjectWriter OBJECT_WRITER =
      OBJECT_MAPPER.writer(new DefaultPrettyPrinter());

  private static Configuration singletonInstance = null;

  static Path configFilePath;

  private BigInteger prime = DEFAULT_PRIME;
  private BigInteger r = DEFAULT_R;
  private BigInteger rInv = DEFAULT_R_INV;

  private boolean noSslValidation = false;

  @JsonProperty(required = true, index = 5)
  private List<Path> trustedCertificates = new ArrayList<>();

  @Setter(value = AccessLevel.NONE)
  @Getter(value = AccessLevel.NONE)
  @JsonProperty(required = true, index = 6)
  private VcpConfiguration[] providers;

  Configuration() {
    providers = new VcpConfiguration[] {new VcpConfiguration(1), new VcpConfiguration(2)};
  }

  public static void setConfigFilePath(Path configFilePath) {
    Configuration.configFilePath = configFilePath;
  }

  public static synchronized Configuration getInstance() throws CsCliConfigurationException {
    if (singletonInstance == null) {
      Configuration config = loadFromFile();
      if (config == null) {
        throw new CsCliConfigurationException(
            MessageFormat.format(
                MESSAGES.getString("failure.not-configured"), APPLICATION_TITLE, COMMAND_NAME));
      }
      singletonInstance = config;
    }
    return singletonInstance;
  }

  static Configuration loadFromFile() {
    File configFile = getConfigFile();
    Configuration configuration = null;
    log.debug("Using config file \"{}\"", configFile.getAbsolutePath());
    if (configFile.exists()) {
      if (configFile.canRead()) {
        try {
          configuration = OBJECT_MAPPER.readValue(configFile, Configuration.class);
        } catch (IOException e) {
          log.error(MESSAGES.getString("read-config.failure.cannot-be-parsed"), e);
        }
      } else {
        log.error(MESSAGES.getString("read-config.failure.cannot-be-read"));
      }
    } else {
      log.debug(MESSAGES.getString("read-config.log.does-not-exist"));
    }
    log.debug("Parsed configuration from file: {}", configuration);
    return configuration;
  }

  private static File getConfigFile() {
    if (configFilePath == null) {
      Path userHome = Paths.get(System.getProperty("user.home"));
      if (!userHome.toFile().isDirectory()) {
        log.error(
            MessageFormat.format(
                MESSAGES.getString("read-config.failure.home-does-not-exist"),
                userHome.toString()));
      }
      configFilePath =
          Paths.get(System.getProperty("user.home"), CONFIG_FOLDER_PATH, CONFIG_FILE_NAME);
    }
    return configFilePath.toFile();
  }

  public void configure() throws CsCliConfigurationException {
    try {
      System.out.print(
          MessageFormat.format(
              MESSAGES.getString("configuration.request.prime"), getPrime().toString()));
      prime = new BigInteger(readOrDefault(getPrime().toString()));
      System.out.print(
          MessageFormat.format(MESSAGES.getString("configuration.request.r"), getR().toString()));
      r = new BigInteger(readOrDefault(getR().toString()));
      System.out.print(
          MessageFormat.format(
              MESSAGES.getString("configuration.request.r-inv"), getRInv().toString()));
      rInv = new BigInteger(readOrDefault(getRInv().toString()));
      System.out.print(
          MessageFormat.format(
              MESSAGES.getString("configuration.request.no-ssl-validation"),
              isNoSslValidation() ? "yes" : "no"));
      noSslValidation = inputToBool(readOrDefault(isNoSslValidation() ? "yes" : "no"));
      System.out.print(
          MessageFormat.format(
              MESSAGES.getString("configuration.request.trusted-certificates"),
              getTrustedCertificatesAsString()));
      trustedCertificates =
          inputToList(readOrDefault(getTrustedCertificatesAsString())).stream()
              .map(Paths::get)
              .collect(Collectors.toList());
      System.out.print(
          MessageFormat.format(
              MESSAGES.getString("configuration.request.number-of-players"),
              providers.length < 2 ? 2 : providers.length));
      setNumberOfPlayers(
          Integer.valueOf(
              readOrDefault(Integer.toString(providers.length < 2 ? 2 : providers.length))));
      for (VcpConfiguration provider : providers) {
        provider.configure();
      }
    } catch (NoSuchElementException | IllegalStateException e) {
      throw new CsCliConfigurationException(MESSAGES.getString("configuration.failed"), e);
    }
  }

  @JsonIgnore
  public VcpConfiguration[] getProviders() {
    return Arrays.copyOf(providers, providers.length);
  }

  public VcpConfiguration getProvider(int id) throws CsCliConfigurationException {
    try {
      return providers[id - 1];
    } catch (IndexOutOfBoundsException ioobe) {
      throw new CsCliConfigurationException(
          MessageFormat.format(MESSAGES.getString("configuration.access.invalid-provider-id"), id));
    }
  }

  public BigInteger getPrime() {
    return System.getenv(PRIME_ENV_KEY) != null
        ? new BigInteger(System.getenv(PRIME_ENV_KEY))
        : prime;
  }

  @JsonProperty(value = "prime", required = true, index = 1)
  private BigInteger getActualPrime() {
    return prime;
  }

  public BigInteger getR() {
    return System.getenv(R_ENV_KEY) != null ? new BigInteger(System.getenv(R_ENV_KEY)) : r;
  }

  @JsonProperty(value = "r", required = true, index = 2)
  private BigInteger getActualR() {
    return r;
  }

  public BigInteger getRInv() {
    return System.getenv(R_INV_ENV_KEY) != null
        ? new BigInteger(System.getenv(R_INV_ENV_KEY))
        : rInv;
  }

  @JsonProperty(value = "rinv", required = true, index = 3)
  public BigInteger getActualRInv() {
    return rInv;
  }

  public boolean isNoSslValidation() {
    return System.getenv(NO_SSL_VALIDATION_ENV_KEY) != null
        ? Boolean.valueOf(System.getenv(NO_SSL_VALIDATION_ENV_KEY))
        : noSslValidation;
  }

  @JsonProperty(value = "noSslValidation", required = true, index = 4)
  public boolean isActualNoSslValidation() {
    return noSslValidation;
  }

  public List<Path> getTrustedCertificates() {
    return ImmutableList.copyOf(trustedCertificates);
  }

  public void writeToFile() throws CsCliConfigurationException {
    File configFile = getConfigFile();
    File configFolder = configFile.getParentFile();
    if (!configFolder.mkdirs() && !configFolder.isDirectory()) {
      throw new CsCliConfigurationException(
          MESSAGES.getString("configuration.finalize.failed.creating-config-directory"));
    }
    try {
      OBJECT_WRITER.writeValue(configFile, this);
    } catch (IOException e) {
      throw new CsCliConfigurationException(
          MESSAGES.getString("configuration.finalize.failed.writing-configuration"), e);
    }
  }

  public String toPrettyString() {
    try {
      return OBJECT_WRITER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return toString();
    }
  }

  @Override
  public String toString() {
    return "Configuration{"
        + "prime="
        + prime
        + ", r="
        + r
        + ", rInv="
        + rInv
        + ", noSslValidation="
        + noSslValidation
        + ", trustedCertificates="
        + trustedCertificates
        + ", providers="
        + Arrays.toString(providers)
        + '}';
  }

  private Configuration setNumberOfPlayers(int numberOfPlayers) throws CsCliConfigurationException {
    if (numberOfPlayers < 2) {
      throw new CsCliConfigurationException(
          MessageFormat.format(
              MESSAGES.getString("configuration.invalid-input.number-of-players"),
              numberOfPlayers));
    }
    providers = Arrays.copyOf(providers, numberOfPlayers);
    for (int i = 0; i < providers.length; i++) {
      if (providers[i] == null) {
        providers[i] = new VcpConfiguration(i + 1);
      }
    }
    return this;
  }

  private String getTrustedCertificatesAsString() {
    return trustedCertificates.stream().map(Path::toString).collect(Collectors.joining(";"));
  }

  private static boolean inputToBool(String in) {
    return StringUtils.equalsAny(in.toLowerCase(), "yes", "y", "true");
  }

  private static List<String> inputToList(String in) {
    return Arrays.stream(in.split(";")).filter(s -> !s.isEmpty()).collect(Collectors.toList());
  }
}
