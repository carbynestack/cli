/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.castor;

import static io.carbynestack.cli.CsCliApplication.CS_CLI_MESSAGE_BUNDLE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.beust.jcommander.JCommander;
import io.carbynestack.castor.client.download.CastorIntraVcpClient;
import io.carbynestack.castor.client.upload.CastorUploadClient;
import io.carbynestack.castor.common.entities.TelemetryData;
import io.carbynestack.castor.common.entities.TupleChunk;
import io.carbynestack.castor.common.entities.TupleType;
import io.carbynestack.castor.common.exceptions.CastorClientException;
import io.carbynestack.cli.CsClientCli;
import io.carbynestack.cli.TemporaryConfiguration;
import io.carbynestack.cli.client.castor.command.config.ActivateChunkCastorClientCliCommandConfig;
import io.carbynestack.cli.client.castor.command.config.GetCastorTelemetryCliCommandConfig;
import io.carbynestack.cli.client.castor.command.config.UploadTupleCastorClientCliCommandConfig;
import io.carbynestack.cli.client.castor.config.CastorClientCliConfig;
import io.carbynestack.cli.client.castor.util.TelemetryPrinter;
import io.carbynestack.cli.exceptions.CsCliException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.vavr.control.Option;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.UUID;
import lombok.SneakyThrows;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CastorClientCliTest {

  private static final String CASTOR_URL = "ws://localhost:8080/ws";
  private static final String TEST_TRIPLE_FILE_NAME = "Triples-p-P0-10000";
  protected static final String TUPLE_FILE_PATH =
      CastorClientCliTest.class.getClassLoader().getResource(TEST_TRIPLE_FILE_NAME).getPath();
  private final ResourceBundle csCliMessages = ResourceBundle.getBundle(CS_CLI_MESSAGE_BUNDLE);

  @Mock private CastorUploadClient castorUploadClientMock;
  @Mock private CastorIntraVcpClient castorInterVcpClientMock;

  @Rule public TemporaryConfiguration temporaryConfiguration = new TemporaryConfiguration();
  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  private CastorClientCli getCliWithArgs(String... args) {
    CastorClientCliConfig config = new CastorClientCliConfig();
    JCommander.newBuilder().addObject(config).build().parse(args);
    return new CastorClientCli(
        config,
        false,
        Option.some(() -> castorUploadClientMock),
        Option.some(() -> castorInterVcpClientMock));
  }

  private CastorClientCli getCliWithFactoriesAndArgs(
      CastorUploadClientFactory uploadClientFactory,
      CastorIntraVcpClientFactory intraVcpClientFactory,
      String... args) {
    CastorClientCliConfig config = new CastorClientCliConfig();
    JCommander.newBuilder().addObject(config).build().parse(args);
    return new CastorClientCli(
        config, false, Option.of(uploadClientFactory), Option.of(intraVcpClientFactory));
  }

  @Test
  public void showHelpTest()
      throws CsCliException, NoSuchMethodException, InvocationTargetException,
          IllegalAccessException, CsCliRunnerException, CsCliLoginException {
    String helpOption = "--help";
    CastorClientCli castorClientCli = getCliWithArgs(helpOption);
    castorClientCli.parse();
    Method getProgramName = CsClientCli.class.getDeclaredMethod("getProgramName");
    getProgramName.setAccessible(true);
    assertThat(
        systemOutRule.getLog(),
        containsString(String.format("Usage: %s", getProgramName.invoke(castorClientCli))));
  }

  @Test
  public void noCommandTest() throws CsCliRunnerException, CsCliLoginException {
    try {
      String optionOnly = "--debug";
      getCliWithArgs(optionOnly).parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      assertThat(
          e.getMessage(), CoreMatchers.startsWith(csCliMessages.getString("error.no-command")));
    }
  }

  @Test
  public void uploadTupleMissingTripleFileOptionTest()
      throws CsCliRunnerException, CsCliLoginException {
    try {
      getCliWithArgs(
              UploadTupleCastorClientCliCommandConfig.COMMAND_NAME,
              "-t",
              TupleType.MULTIPLICATION_TRIPLE_GFP.name(),
              "1")
          .parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      assertThat(e.getMessage(), CoreMatchers.startsWith("The following option is required:"));
      assertThat(e.getMessage(), containsString("--tuple-file"));
    }
  }

  @Test
  public void uploadTupleMissingCastorServiceIdTest()
      throws CsCliRunnerException, CsCliLoginException {
    try {
      String tripleFile =
          this.getClass().getClassLoader().getResource(TEST_TRIPLE_FILE_NAME).getPath();
      getCliWithFactoriesAndArgs(
              null,
              null,
              UploadTupleCastorClientCliCommandConfig.COMMAND_NAME,
              "-t",
              TupleType.MULTIPLICATION_TRIPLE_GFP.name(),
              "-f",
              tripleFile)
          .parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      assertThat(e.getMessage(), CoreMatchers.startsWith("Main parameters are required"));
    }
  }

  @Test
  public void uploadTupleMissingTupleTypeTest() throws CsCliRunnerException, CsCliLoginException {
    try {
      String tupleFile =
          this.getClass().getClassLoader().getResource(TEST_TRIPLE_FILE_NAME).getPath();
      getCliWithArgs(UploadTupleCastorClientCliCommandConfig.COMMAND_NAME, "-f", tupleFile).parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      assertThat(e.getMessage(), CoreMatchers.startsWith("The following option is required:"));
      assertThat(e.getMessage(), containsString("--tuple-type"));
    }
  }

  @Test
  public void uploadTuplesSuccessTest()
      throws CsCliRunnerException, CsCliException, CsCliLoginException {
    UUID chunkId = UUID.randomUUID();
    when(castorUploadClientMock.uploadTupleChunk(any())).thenReturn(true);
    getCliWithArgs(
            UploadTupleCastorClientCliCommandConfig.COMMAND_NAME,
            "-t",
            TupleType.MULTIPLICATION_TRIPLE_GFP.name(),
            "-f",
            TUPLE_FILE_PATH,
            "-i",
            chunkId.toString(),
            "1")
        .parse();
    ArgumentCaptor<TupleChunk> tupleChunkArgumentCaptor = ArgumentCaptor.forClass(TupleChunk.class);
    verify(castorUploadClientMock, times(1)).uploadTupleChunk(tupleChunkArgumentCaptor.capture());
    TupleChunk capturedChunk = tupleChunkArgumentCaptor.getValue();
    Assert.assertNotNull(capturedChunk);
    Assert.assertEquals(chunkId, capturedChunk.getChunkId());
  }

  @SneakyThrows
  @Test
  public void getTelemetryConnectionRefusedTest() {
    CastorClientException expectedCause = new CastorClientException("Connection refused");
    when(castorInterVcpClientMock.getTelemetryData()).thenThrow(expectedCause);
    CsCliRunnerException actualException =
        assertThrows(
            CsCliRunnerException.class,
            () -> getCliWithArgs(GetCastorTelemetryCliCommandConfig.COMMAND_NAME, "1").parse());

    assertEquals("Failed fetching telemetry data", actualException.getMessage());
    assertEquals(expectedCause, actualException.getCause());
  }

  @SneakyThrows
  @Test
  public void getTelemetryWithIntervalSuccessTest() {
    Duration interval = Duration.ofSeconds(42);
    TelemetryData telemetryData = new TelemetryData(Collections.EMPTY_LIST, interval.toMillis());
    when(castorInterVcpClientMock.getTelemetryData(interval.getSeconds()))
        .thenReturn(telemetryData);
    getCliWithArgs(
            GetCastorTelemetryCliCommandConfig.COMMAND_NAME,
            "-i",
            Long.toString(interval.getSeconds()),
            "1")
        .parse();
    ArgumentCaptor<Long> longArgumentCaptor = ArgumentCaptor.forClass(Long.class);
    verify(castorInterVcpClientMock, times(1)).getTelemetryData(longArgumentCaptor.capture());
    Assert.assertEquals(interval.getSeconds(), longArgumentCaptor.getValue().longValue());
    Assert.assertThat(
        systemOutRule.getLog(),
        containsString(TelemetryPrinter.telemetryDataToString(telemetryData)));
  }

  @Test
  public void activateChunkMissingIdTest() throws CsCliRunnerException, CsCliLoginException {
    try {
      getCliWithArgs(ActivateChunkCastorClientCliCommandConfig.COMMAND_NAME, "1").parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      assertThat(e.getMessage(), CoreMatchers.startsWith("The following option is required:"));
      assertThat(e.getMessage(), containsString("--chunk-id"));
    }
  }

  @Test
  public void activateChunkInvalidIdTest() throws CsCliRunnerException, CsCliLoginException {
    String invalidUUID = "invalidUUID";
    try {
      getCliWithArgs(
              ActivateChunkCastorClientCliCommandConfig.COMMAND_NAME, CASTOR_URL, "-i", invalidUUID)
          .parse();
      Assert.fail("Expected Exception");
    } catch (CsCliException e) {
      assertThat(e.getMessage(), CoreMatchers.startsWith("Invalid UUID"));
      assertThat(e.getMessage(), containsString(invalidUUID));
    }
  }

  @SneakyThrows
  @Test
  public void activateChunkSuccessTest() {
    UUID chunkId = UUID.randomUUID();
    getCliWithArgs(
            ActivateChunkCastorClientCliCommandConfig.COMMAND_NAME, "-i", chunkId.toString(), "1")
        .parse();
    ArgumentCaptor<UUID> uuidArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(castorUploadClientMock, times(1)).activateTupleChunk(uuidArgumentCaptor.capture());
    Assert.assertEquals(chunkId, uuidArgumentCaptor.getValue());
  }
}
