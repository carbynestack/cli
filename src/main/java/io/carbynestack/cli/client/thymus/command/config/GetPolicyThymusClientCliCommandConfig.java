/*
 * Copyright (c) 2025 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus.command.config;

import static io.carbynestack.cli.client.thymus.ThymusClientCli.THYMUS_MESSAGE_BUNDLE;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.carbynestack.cli.client.thymus.ThymusClientFactory;
import io.carbynestack.cli.client.thymus.config.ThymusClientCliCommandConfig;
import io.carbynestack.cli.client.thymus.converter.PolicyIDConverter;
import io.carbynestack.thymus.client.NamespacedName;
import io.vavr.control.Option;
import java.util.List;
import lombok.Getter;

@Getter
@Parameters(
    resourceBundle = THYMUS_MESSAGE_BUNDLE,
    commandDescriptionKey = "get-policy.command-description")
public class GetPolicyThymusClientCliCommandConfig extends ThymusClientCliCommandConfig {

  public static final String COMMAND_NAME = "get-policy";

  @Parameter(
      descriptionKey = "get-policy.parameter.policy-id-description",
      required = true,
      converter = PolicyIDConverter.class,
      arity = 1)
  // unnamed parameter must be of type list to collect all unnamed parameters and apply the
  // converter correctly
  private List<NamespacedName> policyId;

  public NamespacedName getPolicyId() {
    return policyId.get(0);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  public GetPolicyThymusClientCliCommandConfig(Option<ThymusClientFactory> customClientFactory) {
    super(customClientFactory);
  }
}
