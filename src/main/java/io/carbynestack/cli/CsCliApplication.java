/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli;

import static io.carbynestack.cli.config.CsCliConfig.APPLICATION_TITLE;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import io.carbynestack.cli.client.amphora.AmphoraClientCli;
import io.carbynestack.cli.client.amphora.config.AmphoraClientCliConfig;
import io.carbynestack.cli.client.castor.CastorClientCli;
import io.carbynestack.cli.client.castor.config.CastorClientCliConfig;
import io.carbynestack.cli.client.ephemeral.EphemeralClientCli;
import io.carbynestack.cli.client.ephemeral.config.EphemeralClientCliConfig;
import io.carbynestack.cli.config.CsCliConfig;
import io.carbynestack.cli.configuration.Configuration;
import io.carbynestack.cli.configuration.ConfigurationCommand;
import io.carbynestack.cli.configuration.ConfigurationCommandFactory;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.exceptions.CsCliException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.carbynestack.cli.login.LoginCommand;
import io.carbynestack.cli.login.LoginCommandFactory;
import io.carbynestack.cli.login.VcpTokenStore;
import io.vavr.control.Option;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

@Slf4j
public class CsCliApplication {
  public static final String CS_CLI_MESSAGE_BUNDLE = "CsCliMessageBundle";
  private static final ResourceBundle messages = ResourceBundle.getBundle(CS_CLI_MESSAGE_BUNDLE);

  private final List<CsClient> clients = new ArrayList<>();

  private final CsCliConfig csCliConfig = new CsCliConfig();

  private final ConfigurationCommand configurationCommand;

  private final LoginCommand loginCommand;

  CsCliApplication() {
    this(Option.none(), Option.none());
  }

  CsCliApplication(
      Option<ConfigurationCommandFactory> configurationCommandFactory,
      Option<LoginCommandFactory> loginCommandFactory) {
    configurationCommand =
        configurationCommandFactory
            .map(ConfigurationCommandFactory::create)
            .getOrElse(new ConfigurationCommand());
    loginCommand =
        loginCommandFactory.map(LoginCommandFactory::create).getOrElse(LoginCommand::new);
  }

  public void run(String... args)
      throws CsCliRunnerException, CsCliException, CsCliConfigurationException,
          CsCliLoginException {
    JCommander jCommander = initializeJCommander(args);
    jCommander.setColumnSize(Integer.MAX_VALUE);
    String parsedCommand = jCommander.getParsedCommand();

    if (csCliConfig.isHelp()) {
      StringBuilder sb = new StringBuilder();
      jCommander.usage(sb);
      System.out.println(StringEscapeUtils.unescapeJava(sb.toString()));
      return;
    }

    if (csCliConfig.isDebug()) {
      Logger.getRootLogger().setLevel(Level.DEBUG);
    }

    if (parsedCommand == null) {
      ParameterException pe = new ParameterException(messages.getString("error.no-command"));
      pe.setJCommander(jCommander);
      throw pe;
    }
    try {
      Configuration.setConfigFilePath(csCliConfig.getConfigPath());
      Option.of(csCliConfig.getAccessTokenFilePath())
          .forEach(p -> VcpTokenStore.setDefaultLocation(Option.some(p)));
      switch (parsedCommand) {
        case ConfigurationCommand.COMMAND_NAME:
          configurationCommand.configure();
          break;
        case LoginCommand.COMMAND_NAME:
          loginCommand.login();
          break;
        default:
          for (CsClient client : clients) {
            if (parsedCommand.equals(client.getConfig().getClientName())) {
              client
                  .getClientCli()
                  .getConstructor(client.getConfig().getClass(), boolean.class)
                  .newInstance(client.getConfig(), csCliConfig.isDebug())
                  .parse();
            }
          }
      }
    } catch (IOException
        | InstantiationException
        | InvocationTargetException
        | NoSuchMethodException
        | IllegalAccessException e) {
      StringBuilder sb = new StringBuilder();
      jCommander.usage(sb);
      throw new CsCliException(
          MessageFormat.format(
              messages.getString("error.failed-execute-command"), APPLICATION_TITLE),
          e,
          StringEscapeUtils.unescapeJava(sb.toString()));
    }
  }

  private JCommander initializeJCommander(String... args) {
    JCommander.Builder jCommanderBuilder;
    jCommanderBuilder = JCommander.newBuilder().addObject(csCliConfig);
    jCommanderBuilder.addCommand(configurationCommand);
    jCommanderBuilder.addCommand(loginCommand);
    for (CsClient client : clients) {
      jCommanderBuilder.addCommand(client.getConfig().getClientName(), client.getConfig());
    }

    JCommander jCommander = jCommanderBuilder.build();
    jCommander.setProgramName(APPLICATION_TITLE);
    jCommander.parse(args);
    return jCommander;
  }

  void addClient(CsClient client) {
    this.clients.add(client);
  }

  public static void main(String... args) {
    CsCliApplication csCli = new CsCliApplication();
    csCli.addClient(new CsClient(new AmphoraClientCliConfig(), AmphoraClientCli.class));
    csCli.addClient(new CsClient(new CastorClientCliConfig(), CastorClientCli.class));
    csCli.addClient(new CsClient(new EphemeralClientCliConfig(), EphemeralClientCli.class));
    try {
      csCli.run(args);
    } catch (ParameterException pe) {
      log.error(String.format("%s%n%n", pe.getMessage()));
      pe.getJCommander().usage();
      System.exit(1);
    } catch (CsCliException sce) {
      log.error("Unexpected error occurred", sce);
      log.info(sce.getHelpMessage());
      System.exit(2);
    } catch (CsCliRunnerException scre) {
      log.error(
          MessageFormat.format(messages.getString("error.command-execution"), scre.getMessage()),
          scre);
      System.exit(3);
    } catch (CsCliConfigurationException scce) {
      log.error(scce.getMessage(), scce);
      System.exit(4);
    } catch (CsCliLoginException scle) {
      log.error(scle.getMessage(), scle);
      System.exit(5);
    }
    System.exit(0);
  }
}
