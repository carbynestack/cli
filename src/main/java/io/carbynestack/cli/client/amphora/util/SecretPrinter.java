/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.cli.client.amphora.util;

import io.carbynestack.amphora.client.Secret;
import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.Tag;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class SecretPrinter {

  public static String metadataListToString(List<Metadata> metadataList, boolean idsOnly) {
    String output = "";
    for (int i = 0; i < metadataList.size(); i++) {
      output =
          String.format(
              "%s%s%s",
              output, i == 0 ? "" : System.lineSeparator(), metadataList.get(i).getSecretId());
      if (!idsOnly) {
        output = String.format("%s%n%s", output, tagsToString(metadataList.get(i).getTags()));
      }
    }
    return output;
  }

  public static String secretToString(Secret secret) {
    String output = Arrays.toString(secret.getData());
    output = String.format("%s%n%s", output, tagsToString(secret.getTags()));
    return output;
  }

  public static String tagsToString(List<Tag> tags) {
    if (tags == null) {
      return "";
    }
    String output = "";
    for (Tag tag : tags) {
      output = String.format("%s%s\n", output, tagToString(tag));
    }
    output = StringUtils.chomp(output);
    return output;
  }

  public static String tagToString(Tag tag) {
    if (tag == null) {
      return "";
    }
    return String.format("\t%s -> %s", tag.getKey(), tag.getValue());
  }
}
