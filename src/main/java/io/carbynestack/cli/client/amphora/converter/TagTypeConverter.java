/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora.converter;

import static io.carbynestack.cli.client.amphora.AmphoraClientCli.AMPHORA_MESSAGE_BUNDLE;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.TagValueType;
import io.vavr.control.Try;
import java.util.ResourceBundle;

public class TagTypeConverter implements IStringConverter<Tag> {

  private ResourceBundle messages = ResourceBundle.getBundle(AMPHORA_MESSAGE_BUNDLE);

  @Override
  public Tag convert(String value) {
    // only split at the first "=" since tags can contain any character. However, spaces are not
    // allowed.
    String[] data = value.split("=", 2);

    if (data.length != 2) {
      throw new ParameterException(messages.getString("tag.conversion.exception.format"));
    }
    boolean isLong = Try.of(() -> Long.parseLong(data[1])).isSuccess();
    return Tag.builder()
        .key(data[0])
        .value(data[1])
        .valueType(isLong ? TagValueType.LONG : TagValueType.STRING)
        .build();
  }
}
