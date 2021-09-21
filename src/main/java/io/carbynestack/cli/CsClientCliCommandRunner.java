/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli;

import io.carbynestack.cli.config.CsClientCliCommandConfig;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import java.util.ResourceBundle;

public abstract class CsClientCliCommandRunner<T extends CsClientCliCommandConfig> {
  private T config;
  private ResourceBundle messages;

  public CsClientCliCommandRunner(T config) {
    // touch with care since this class is instantiated automatically
    // {@see CsClientCli#execute(CsClientCliCommand command)}

    this.config = config;
  }

  /**
   * Initializes the message bundle. Call {@link #getMessages()} to access the labels.
   *
   * @param messageBundle the base name of the resource bundle, a fully qualified class name
   */
  public void initializeMessageBundle(String messageBundle) {
    messages = ResourceBundle.getBundle(messageBundle);
  }

  public T getConfig() {
    return config;
  }

  /**
   * make sure to call {@link #initializeMessageBundle(String)} first.
   *
   * @return ResourceBundle containing the localized labels.
   */
  public ResourceBundle getMessages() {
    return messages;
  }

  /** This method is called to start the command execution. */
  public abstract void run() throws CsCliRunnerException;
}
