/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.command;

import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.TagFilter;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.amphora.common.paging.Sort;
import io.carbynestack.cli.client.amphora.command.config.ListSecretsAmphoraClientCliCommandConfig;
import io.carbynestack.cli.client.amphora.util.SecretPrinter;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.vavr.control.Try;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ListSecretsAmphoraClientCliCommandRunner
    extends AmphoraClientCliCommandRunner<ListSecretsAmphoraClientCliCommandConfig> {

  public ListSecretsAmphoraClientCliCommandRunner(ListSecretsAmphoraClientCliCommandConfig config)
      throws CsCliRunnerException, CsCliConfigurationException, CsCliLoginException {
    super(config);
  }

  @Override
  public void run() throws CsCliRunnerException {
    Sort sort = getSorting();
    List<TagFilter> tagFilters = getTagFilters(getConfig().getTagFilters());
    try {
      List<Metadata> result = this.getAmphoraClient().getSecrets(tagFilters, sort);
      log.debug(getMessages().getString("list.log.success"), result);
      System.out.println(SecretPrinter.metadataListToString(result, getConfig().isIdsOnly()));
    } catch (AmphoraClientException ace) {
      throw new CsCliRunnerException(ace.getMessage(), ace);
    }
  }

  private List<TagFilter> getTagFilters(List<String> tagFilterStrings) throws CsCliRunnerException {
    List<TagFilter> tagFilters = new ArrayList<>();
    for (String filterString : tagFilterStrings) {
      try {
        tagFilters.add(TagFilter.fromString(filterString));
      } catch (Exception e) {
        log.debug(getMessages().getString("list.failure.invalid-tag-filter"), e.getMessage());
        throw new CsCliRunnerException(
            MessageFormat.format(
                getMessages().getString("list.failure.invalid-tag-filter"), filterString));
      }
    }
    return tagFilters;
  }

  /** @throws CsCliRunnerException if given sort config cannot be parsed */
  private Sort getSorting() throws CsCliRunnerException {
    Sort sort = null;
    if (getConfig().getSortBy() != null) {
      String[] sortParams = getConfig().getSortBy().split(":");
      Optional<Sort.Order> orderOptional =
          Try.of(() -> Sort.Order.valueOf(sortParams[1])).toJavaOptional();
      if (sortParams.length != 2 || !orderOptional.isPresent()) {
        throw new CsCliRunnerException(
            MessageFormat.format(
                getMessages().getString("list.failure.invalid-sort-format"),
                getConfig().getSortBy()));
      }
      sort = Sort.by(sortParams[0], orderOptional.get());
    }
    return sort;
  }
}
