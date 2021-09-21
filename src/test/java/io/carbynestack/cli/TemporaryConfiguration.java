/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Lists;
import io.carbynestack.cli.configuration.Configuration;
import io.carbynestack.cli.configuration.ConfigurationUtil;
import io.carbynestack.cli.login.VcpToken;
import io.carbynestack.cli.login.VcpTokenStore;
import io.carbynestack.cli.util.TokenUtils;
import io.vavr.control.Option;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.rules.TemporaryFolder;

@Slf4j
public class TemporaryConfiguration extends TemporaryFolder {

  private static final ObjectWriter WRITER = new ObjectMapper().writer(new DefaultPrettyPrinter());

  private final Field configurationSingletonField;
  private final Supplier<Configuration> configurationProvider;
  private Configuration configuration;
  private File configurationFile;

  public TemporaryConfiguration(Supplier<Configuration> configurationProvider) {
    this.configurationProvider = configurationProvider;
    try {
      configurationSingletonField = Configuration.class.getDeclaredField("singletonInstance");
      configurationSingletonField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      log.error("Cannot access configuration's instance field", e);
      throw new RuntimeException(e);
    }
  }

  public TemporaryConfiguration() {
    this(ConfigurationUtil::getConfiguration);
  }

  @Override
  protected void before() throws Throwable {
    super.before();
    Configuration configuration = this.getConfiguration();
    configurationSingletonField.set(null, null);
    configurationFile = this.newFile();
    Configuration.setConfigFilePath(configurationFile.toPath());
    WRITER.writeValue(configurationFile, configuration);
    File accessTokensFile = this.newFile();
    VcpTokenStore.setDefaultLocation(Option.of(accessTokensFile.toPath()));
    VcpTokenStore.VcpTokenStoreBuilder builder = VcpTokenStore.builder();
    Lists.newArrayList(configuration.getProviders())
        .forEach(
            p ->
                builder.token(
                    VcpToken.from(
                        p.getBaseUrl(),
                        TokenUtils.createToken(RandomStringUtils.randomAlphabetic(10)))));
    VcpTokenStore s = builder.build();
    s.persist();
  }

  @Override
  protected void after() {
    super.after();
    VcpTokenStore.setDefaultLocation(Option.none());
    Configuration.setConfigFilePath(null);
  }

  public File getConfigFile() {
    return configurationFile;
  }

  public Configuration getConfiguration() {
    if (configuration == null) {
      configuration = configurationProvider.get();
    }
    return configuration;
  }

  public void applyConfigUpdate(Function<Configuration, Void> configUpdater) throws IOException {
    Configuration configuration = this.getConfiguration();
    configUpdater.apply(configuration);
    WRITER.writeValue(configurationFile, configuration);
  }
}
