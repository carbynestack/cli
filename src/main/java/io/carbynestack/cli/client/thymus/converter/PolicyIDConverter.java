/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.thymus.converter;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import io.carbynestack.thymus.client.NamespacedName;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import static io.carbynestack.cli.client.thymus.ThymusClientCli.THYMUS_MESSAGE_BUNDLE;

public class PolicyIDConverter implements IStringConverter<NamespacedName> {

    private final ResourceBundle messages = ResourceBundle.getBundle(THYMUS_MESSAGE_BUNDLE);

    @Override
    public NamespacedName convert(String value) {
        try {
            return NamespacedName.fromString(value);
        } catch (Exception e) {
            throw new ParameterException(
                    MessageFormat.format(messages.getString("policyid.conversion.exception"), value));
        }
    }

}