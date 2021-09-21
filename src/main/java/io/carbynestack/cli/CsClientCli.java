/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli;

import static io.carbynestack.cli.CsCliApplication.*;
import static io.carbynestack.cli.config.CsCliConfig.*;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import io.carbynestack.cli.config.CsCliConfig;
import io.carbynestack.cli.config.CsClientCliConfig;
import io.carbynestack.cli.exceptions.CsCliException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import org.apache.commons.lang3.StringEscapeUtils;

public class CsClientCli {
  private final CsCliConfig csCliConfig = new CsCliConfig();

  final boolean isDebug;
  private final List<CsClientCliCommand> commands = new ArrayList<>();
  private final CsClientCliConfig config;

  private JCommander jCommander = null;

  private ResourceBundle messages;

  /**
   * Add new commands to the client {@link #addCommand(CsClientCliCommand)} and call {@link
   * #parse()} afterwards to start parsing the command line arguments for the defined commands.
   *
   * @param config Carbyne Stack Service client configuration secret containing command args and
   *     client name.
   * @param debug Toggles Debug log messages.
   */
  public CsClientCli(CsClientCliConfig config, boolean debug) {
    // touch with care since this class is instantiated automatically
    // {@see CsCliApplication#run()}

    this.config = config;
    this.isDebug = debug;

    messages = ResourceBundle.getBundle(CS_CLI_MESSAGE_BUNDLE);
  }

  /**
   * Adds the passed command to the client.
   *
   * @param command
   * @return
   */
  public CsClientCli addCommand(CsClientCliCommand command) {
    commands.add(command);
    return this;
  }

  /**
   * Parses the programs arguments and calls {@link #execute(CsClientCliCommand)} if a defined
   * command is recognized
   */
  public void parse() throws CsCliException, CsCliRunnerException, CsCliLoginException {
    initializeJCommander();
    parseConfig();
  }

  /** Initialize jCommander with basic configuration and all commands. */
  private void initializeJCommander() {
    JCommander.Builder jcBuilder =
        JCommander.newBuilder().addObject(csCliConfig).columnSize(Integer.MAX_VALUE);
    for (CsClientCliCommand command : commands) {
      jcBuilder.addCommand(command.getConfig().getCommandName(), command.getConfig());
    }
    jCommander = jcBuilder.build();
    jCommander.setProgramName(this.getProgramName());
  }

  private void parseConfig() throws CsCliException, CsCliRunnerException, CsCliLoginException {
    try {
      jCommander.parse(config.getArgs());
      if (csCliConfig.isHelp()) {
        StringBuilder sb = new StringBuilder();
        jCommander.usage(sb);
        System.out.println(StringEscapeUtils.unescapeJava(sb.toString()));
        return;
      }

      String parsedCommand = jCommander.getParsedCommand();
      if (parsedCommand == null) {
        StringBuilder sb = new StringBuilder();
        jCommander.usage(sb);
        throw new CsCliException(
            messages.getString("error.no-command"),
            new Throwable().fillInStackTrace(),
            sb.toString());
      }
      for (CsClientCliCommand command : commands) {
        if (parsedCommand.equals(command.getConfig().getCommandName())) {
          execute(command);
        }
      }
    } catch (ParameterException pe) {
      StringBuilder sb = new StringBuilder();
      pe.getJCommander().usage(sb);
      throw new CsCliException(String.format("%s%n%n", pe.getMessage()), pe, sb.toString());
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof CsCliLoginException) {
        // The reflective execute call might wrap internal exceptions in InvocationTargetExceptions
        // and that's not intended
        throw (CsCliLoginException) ite.getTargetException();
      }
      StringBuilder sb = new StringBuilder();
      jCommander.usage(sb);
      throw new CsCliException(
          MessageFormat.format(
              messages.getString("error.failed-execute-command"), this.getProgramName()),
          ite.getTargetException(),
          sb.toString());
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
      StringBuilder sb = new StringBuilder();
      jCommander.usage(sb);
      throw new CsCliException(
          MessageFormat.format(
              messages.getString("error.failed-execute-command"), this.getProgramName()),
          e,
          sb.toString());
    }
  }

  /**
   * This method will execute the passed command and is automatically called after parsing the given
   * cli arguments.
   *
   * @param command
   */
  private void execute(CsClientCliCommand command)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
          InstantiationException, CsCliRunnerException {
    command
        .getRunner()
        .getConstructor(command.getConfig().getClass())
        .newInstance(command.getConfig())
        .run();
  }

  /**
   * Returns the SubCommand CLI Program name composed from Carbyne Stack application title and
   * service (subCommand) name.
   */
  private String getProgramName() {
    return String.format("%s %s", APPLICATION_TITLE, config.getClientName());
  }
}
