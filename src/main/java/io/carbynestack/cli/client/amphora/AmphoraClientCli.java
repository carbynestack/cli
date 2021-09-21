/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora;

import io.carbynestack.cli.CsClientCli;
import io.carbynestack.cli.CsClientCliCommand;
import io.carbynestack.cli.client.amphora.command.*;
import io.carbynestack.cli.client.amphora.command.config.*;
import io.carbynestack.cli.client.amphora.config.AmphoraClientCliConfig;
import io.vavr.control.Option;
import java.util.Arrays;

public class AmphoraClientCli extends CsClientCli {
  public static final String AMPHORA_MESSAGE_BUNDLE = "AmphoraMessageBundle";

  public AmphoraClientCli(
      AmphoraClientCliConfig config,
      boolean debug,
      Option<AmphoraClientFactory> customClientFactory) {
    super(config, debug);
    Arrays.asList(
            new CsClientCliCommand(
                new CreateSecretAmphoraClientCliCommandConfig(customClientFactory),
                CreateSecretAmphoraClientCliCommandRunner.class),
            new CsClientCliCommand(
                new GetSecretAmphoraClientCliCommandConfig(customClientFactory),
                GetSecretAmphoraClientCliCommandRunner.class),
            new CsClientCliCommand(
                new ListSecretsAmphoraClientCliCommandConfig(customClientFactory),
                ListSecretsAmphoraClientCliCommandRunner.class),
            new CsClientCliCommand(
                new CreateTagAmphoraClientCliCommandConfig(customClientFactory),
                CreateTagAmphoraClientCliCommandRunner.class),
            new CsClientCliCommand(
                new GetTagsAmphoraClientCliCommandConfig(customClientFactory),
                GetTagsAmphoraClientCliCommandRunner.class),
            new CsClientCliCommand(
                new OverwriteTagsAmphoraClientCliCommandConfig(customClientFactory),
                OverwriteTagsAmphoraClientCliCommandRunner.class),
            new CsClientCliCommand(
                new GetTagAmphoraClientCliCommandConfig(customClientFactory),
                GetTagAmphoraClientCliCommandRunner.class),
            new CsClientCliCommand(
                new UpdateTagAmphoraClientCliCommandConfig(customClientFactory),
                UpdateTagAmphoraClientCliCommandRunner.class),
            new CsClientCliCommand(
                new DeleteTagAmphoraClientCliCommandConfig(customClientFactory),
                DeleteTagAmphoraClientCliCommandRunner.class),
            new CsClientCliCommand(
                new DeleteSecretsAmphoraClientCliCommandConfig(customClientFactory),
                DeleteSecretsAmphoraClientCliCommandRunner.class))
        .forEach(this::addCommand);
  }

  public AmphoraClientCli(AmphoraClientCliConfig config, boolean debug) {
    this(config, debug, Option.none());
  }
}
