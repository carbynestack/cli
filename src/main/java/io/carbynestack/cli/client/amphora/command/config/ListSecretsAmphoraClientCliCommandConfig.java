/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command.config;

import static io.carbynestack.cli.client.amphora.AmphoraClientCli.*;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.carbynestack.cli.client.amphora.AmphoraClientFactory;
import io.carbynestack.cli.client.amphora.config.AmphoraClientCliCommandConfig;
import io.vavr.control.Option;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Parameters(
    resourceBundle = AMPHORA_MESSAGE_BUNDLE,
    commandDescriptionKey = "list.command-description")
public class ListSecretsAmphoraClientCliCommandConfig extends AmphoraClientCliCommandConfig {
  public static final String COMMAND_NAME = "get-secrets";

  @Getter
  @Parameter(
      names = {"-l", "--list-ids-only"},
      descriptionKey = "list.option.ids-only")
  private boolean idsOnly;

  @Getter
  @Parameter(
      names = {"-f", "--tagfilter"},
      descriptionKey = "list.option.tag-filter-description",
      order = 2)
  private List<String> tagFilters;

  @Getter
  @Parameter(
      names = {"-s", "--sortBy"},
      descriptionKey = "list.option.sort-by-description",
      order = 3)
  private String sortBy;

  public ListSecretsAmphoraClientCliCommandConfig(
      Option<AmphoraClientFactory> customClientFactory) {
    super(customClientFactory);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  public List<String> getTagFilters() {
    return tagFilters != null ? tagFilters : Collections.emptyList();
  }
}
