/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora;

import static io.carbynestack.cli.CsCliApplication.CS_CLI_MESSAGE_BUNDLE;
import static io.carbynestack.cli.client.amphora.AmphoraClientCli.AMPHORA_MESSAGE_BUNDLE;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.beust.jcommander.JCommander;
import io.carbynestack.amphora.client.AmphoraClient;
import io.carbynestack.amphora.client.Secret;
import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.TagFilter;
import io.carbynestack.amphora.common.TagFilterOperator;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.amphora.common.exceptions.SecretVerificationException;
import io.carbynestack.amphora.common.paging.Sort;
import io.carbynestack.cli.CsClientCli;
import io.carbynestack.cli.TemporaryConfiguration;
import io.carbynestack.cli.client.amphora.command.config.*;
import io.carbynestack.cli.client.amphora.config.AmphoraClientCliConfig;
import io.carbynestack.cli.client.amphora.util.SecretPrinter;
import io.carbynestack.cli.exceptions.CsCliException;
import io.carbynestack.cli.exceptions.CsCliRunnerException;
import io.carbynestack.cli.login.CsCliLoginException;
import io.vavr.control.Option;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class AmphoraClientCliTest {

  private final ResourceBundle amphoraMessages = ResourceBundle.getBundle(AMPHORA_MESSAGE_BUNDLE);
  private final ResourceBundle csCliMessages = ResourceBundle.getBundle(CS_CLI_MESSAGE_BUNDLE);

  @Mock private AmphoraClient amphoraClientMock = mock(AmphoraClient.class);

  private AmphoraClientCli getCliWithArgs(String... args) {
    AmphoraClientCliConfig config = new AmphoraClientCliConfig();
    JCommander.newBuilder().addObject(config).build().parse(args);
    return new AmphoraClientCli(config, false, Option.some(() -> amphoraClientMock));
  }

  @Rule public TemporaryConfiguration temporaryConfiguration = new TemporaryConfiguration();

  @Rule public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  @Test
  public void showHelpTest()
      throws CsCliException, NoSuchMethodException, InvocationTargetException,
          IllegalAccessException, CsCliRunnerException, CsCliLoginException {
    String helpOption = "--help";
    AmphoraClientCli amphoraClientCli = getCliWithArgs(helpOption);
    amphoraClientCli.parse();
    Method getProgramName = CsClientCli.class.getDeclaredMethod("getProgramName");
    getProgramName.setAccessible(true);
    Assert.assertThat(
        systemOutRule.getLog(),
        containsString(String.format("Usage: %s", getProgramName.invoke(amphoraClientCli))));
  }

  @Test
  public void noCommandTest() throws CsCliRunnerException, CsCliLoginException {
    try {
      String optionOnly = "--debug";
      getCliWithArgs(optionOnly).parse();
      fail("Expected Exception");
    } catch (CsCliException e) {
      Assert.assertThat(
          e.getMessage(), CoreMatchers.startsWith(csCliMessages.getString("error.no-command")));
    }
  }

  @Test
  public void uploadMissingSecretsTest()
      throws CsCliRunnerException, CsCliException, CsCliLoginException {
    try {
      getCliWithArgs(CreateSecretAmphoraClientCliCommandConfig.COMMAND_NAME).parse();
      fail("Expected Exception");
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), amphoraMessages.getString("create.failure.read-secrets"));
    }
  }

  @Test
  public void uploadInvalidTagFormatTest() throws CsCliRunnerException, CsCliLoginException {
    try {
      getCliWithArgs(
              CreateSecretAmphoraClientCliCommandConfig.COMMAND_NAME,
              "-s",
              "42",
              "-t",
              "key1:value1")
          .parse();
      fail("Expected Exception");
    } catch (CsCliException e) {
      Assert.assertThat(
          e.getMessage(),
          containsString(amphoraMessages.getString("tag.conversion.exception.format")));
    }
  }

  @Test(expected = CsCliRunnerException.class)
  public void uploadFailedTest()
      throws AmphoraClientException, CsCliRunnerException, CsCliException, CsCliLoginException {
    when(amphoraClientMock.createSecret(any(Secret.class)))
        .thenThrow(new AmphoraClientException(""));
    getCliWithArgs(CreateSecretAmphoraClientCliCommandConfig.COMMAND_NAME, "42").parse();
  }

  @Test
  public void downloadMissingIdOptionTest() throws CsCliRunnerException, CsCliLoginException {
    try {
      getCliWithArgs(GetSecretAmphoraClientCliCommandConfig.COMMAND_NAME).parse();
      fail("Expected Exception");
    } catch (CsCliException e) {
      Assert.assertThat(
          e.getMessage(), CoreMatchers.startsWith("Main parameters are required (\"SECRET_ID\")"));
    }
  }

  @Test(expected = CsCliRunnerException.class)
  public void downloadFailedTest()
      throws CsCliException, CsCliRunnerException, AmphoraClientException,
          SecretVerificationException, CsCliLoginException {
    when(amphoraClientMock.getSecret(any(UUID.class))).thenThrow(new AmphoraClientException(""));
    getCliWithArgs(
            GetSecretAmphoraClientCliCommandConfig.COMMAND_NAME, UUID.randomUUID().toString())
        .parse();
  }

  @Test
  public void uploadSuccessRandomIdTest()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    long secret = 42;
    ArgumentCaptor<Secret> secretCaptor = ArgumentCaptor.forClass(Secret.class);
    getCliWithArgs(CreateSecretAmphoraClientCliCommandConfig.COMMAND_NAME, Long.toString(secret))
        .parse();
    verify(amphoraClientMock, times(1)).createSecret(secretCaptor.capture());
    Secret capturedSecret = secretCaptor.getValue();
    Assert.assertNotNull(capturedSecret.getSecretId());
    Assert.assertArrayEquals(
        new BigInteger[] {BigInteger.valueOf(secret)}, capturedSecret.getData());
  }

  @Test
  public void uploadSuccessMultipleSecrets()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    long secret1 = 42;
    long secret2 = 24;
    ArgumentCaptor<Secret> secretCaptor = ArgumentCaptor.forClass(Secret.class);
    getCliWithArgs(
            CreateSecretAmphoraClientCliCommandConfig.COMMAND_NAME,
            Long.toString(secret1),
            Long.toString(secret2))
        .parse();
    verify(amphoraClientMock, times(1)).createSecret(secretCaptor.capture());
    Secret capturedSecret = secretCaptor.getValue();
    Assert.assertNotNull(capturedSecret.getSecretId());
    Assert.assertArrayEquals(
        new BigInteger[] {BigInteger.valueOf(secret1), BigInteger.valueOf(secret2)},
        capturedSecret.getData());
  }

  @Test
  public void uploadSuccessMultipleSecretsFromStdin()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    String[] fourSecretsFromStdIn = new String[] {"123", "456", "789", "951"};
    systemInMock.provideLines(fourSecretsFromStdIn);
    ArgumentCaptor<Secret> secretCaptor = ArgumentCaptor.forClass(Secret.class);
    getCliWithArgs(CreateSecretAmphoraClientCliCommandConfig.COMMAND_NAME).parse();
    verify(amphoraClientMock, times(1)).createSecret(secretCaptor.capture());
    Secret capturedSecret = secretCaptor.getValue();
    Assert.assertNotNull(capturedSecret.getSecretId());
    Assert.assertArrayEquals(
        Arrays.stream(fourSecretsFromStdIn).map(BigInteger::new).toArray(),
        capturedSecret.getData());
  }

  @Test
  public void uploadSuccessMultipleSecretsFromStdinSkipsEmptyLines()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    String[] secretsFromStdIn = new String[] {"123", "", "456", "", "789", "951"};
    systemInMock.provideLines(secretsFromStdIn);
    ArgumentCaptor<Secret> secretCaptor = ArgumentCaptor.forClass(Secret.class);
    getCliWithArgs(CreateSecretAmphoraClientCliCommandConfig.COMMAND_NAME).parse();
    verify(amphoraClientMock, times(1)).createSecret(secretCaptor.capture());
    Secret capturedSecret = secretCaptor.getValue();
    Assert.assertNotNull(capturedSecret.getSecretId());
    Assert.assertArrayEquals(
        Arrays.stream(secretsFromStdIn).filter(s -> !s.isEmpty()).map(BigInteger::new).toArray(),
        capturedSecret.getData());
  }

  @Test
  public void uploadSuccessWithIdTest()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    UUID secretId = UUID.randomUUID();
    long secret = 42;
    ArgumentCaptor<Secret> secretCaptor = ArgumentCaptor.forClass(Secret.class);
    when(amphoraClientMock.createSecret(any())).thenReturn(secretId);
    getCliWithArgs(
            CreateSecretAmphoraClientCliCommandConfig.COMMAND_NAME,
            Long.toString(secret),
            "-i",
            secretId.toString())
        .parse();
    verify(amphoraClientMock, times(1)).createSecret(secretCaptor.capture());
    Secret capturedSecret = secretCaptor.getValue();
    Assert.assertEquals(secretId, capturedSecret.getSecretId());
    Assert.assertArrayEquals(
        new BigInteger[] {BigInteger.valueOf(secret)}, capturedSecret.getData());
  }

  @Test
  public void uploadSuccessWithOneTagTest()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    long secret = 42;
    Tag tag = Tag.builder().key("key_1").value("value_1").build();
    ArgumentCaptor<Secret> secretCaptor = ArgumentCaptor.forClass(Secret.class);
    when(amphoraClientMock.createSecret(any())).thenReturn(UUID.randomUUID());
    getCliWithArgs(
            CreateSecretAmphoraClientCliCommandConfig.COMMAND_NAME,
            Long.toString(secret),
            "-t",
            String.format("%s=%s", tag.getKey(), tag.getValue()))
        .parse();
    verify(amphoraClientMock, times(1)).createSecret(secretCaptor.capture());
    Secret capturedSecret = secretCaptor.getValue();
    Assert.assertNotNull(capturedSecret.getSecretId());
    Assert.assertArrayEquals(
        new BigInteger[] {BigInteger.valueOf(secret)}, capturedSecret.getData());
    Assert.assertThat(capturedSecret.getTags(), Matchers.containsInAnyOrder(tag));
  }

  @Test
  public void uploadSuccessWithMultipleTagsTest()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    long secret = 42;
    Tag tag1 = Tag.builder().key("key_1").value("value_1").build();
    Tag tag2 = Tag.builder().key("2-key").value("2-value").build();
    Tag tag3 =
        Tag.builder()
            .key("key.3")
            .value(
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789äöüÄÖÜ-_.:,;#'*+~!\"§$%&/()=?\\ß`'^°@€<>|")
            .build();
    ArgumentCaptor<Secret> secretCaptor = ArgumentCaptor.forClass(Secret.class);
    when(amphoraClientMock.createSecret(any())).thenReturn(UUID.randomUUID());
    getCliWithArgs(
            CreateSecretAmphoraClientCliCommandConfig.COMMAND_NAME,
            Long.toString(secret),
            "-t",
            String.format("%s=%s", tag1.getKey(), tag1.getValue()),
            "-t",
            String.format("%s=%s", tag2.getKey(), tag2.getValue()),
            "-t",
            String.format("%s=%s", tag3.getKey(), tag3.getValue()))
        .parse();
    verify(amphoraClientMock, times(1)).createSecret(secretCaptor.capture());
    Secret capturedSecret = secretCaptor.getValue();
    Assert.assertNotNull(capturedSecret.getSecretId());
    Assert.assertArrayEquals(
        new BigInteger[] {BigInteger.valueOf(secret)}, capturedSecret.getData());
    Assert.assertThat(capturedSecret.getTags(), Matchers.containsInAnyOrder(tag1, tag2, tag3));
  }

  @SneakyThrows
  @Test
  public void downloadSuccessTest() {
    Secret expectedSecret =
        Secret.of(UUID.randomUUID(), emptyList(), new BigInteger[] {BigInteger.TEN});
    when(amphoraClientMock.getSecret(expectedSecret.getSecretId())).thenReturn(expectedSecret);
    getCliWithArgs(
            GetSecretAmphoraClientCliCommandConfig.COMMAND_NAME,
            expectedSecret.getSecretId().toString())
        .parse();
    Assert.assertEquals(Arrays.toString(expectedSecret.getData()), systemOutRule.getLog().trim());
  }

  @Test
  public void downloadFailInvalidNumberOfMainParameters()
      throws CsCliRunnerException, CsCliLoginException {
    UUID secretId = UUID.randomUUID();
    try {
      getCliWithArgs(
              GetSecretAmphoraClientCliCommandConfig.COMMAND_NAME,
              secretId.toString(),
              secretId.toString())
          .parse();
      fail("Exception expected");
    } catch (CsCliException sce) {
      Assert.assertThat(
          sce.getMessage(),
          containsString("There should be exactly 1 main parameters but 2 were found"));
    }
  }

  @SneakyThrows
  @Test
  public void downloadSecretVerificationFails() {
    SecretVerificationException expectedCause =
        new SecretVerificationException("Verification failed.");
    Secret expectedSecret =
        Secret.of(
            UUID.randomUUID(),
            emptyList(),
            new BigInteger[] {BigInteger.TEN, BigInteger.ONE, BigInteger.ZERO});
    when(amphoraClientMock.getSecret(expectedSecret.getSecretId())).thenThrow(expectedCause);
    CsCliRunnerException actualException =
        assertThrows(
            CsCliRunnerException.class,
            () ->
                getCliWithArgs(
                        GetSecretAmphoraClientCliCommandConfig.COMMAND_NAME,
                        expectedSecret.getSecretId().toString())
                    .parse());
    assertEquals("Failed fetching secret.", actualException.getMessage());
    assertEquals(expectedCause, actualException.getCause());
  }

  @Test
  public void downloadLargeSecretSuccessTest()
      throws CsCliException, CsCliRunnerException, AmphoraClientException,
          SecretVerificationException, CsCliLoginException {
    Secret expectedSecret =
        Secret.of(
            UUID.randomUUID(),
            emptyList(),
            new BigInteger[] {BigInteger.TEN, BigInteger.ONE, BigInteger.ZERO});
    when(amphoraClientMock.getSecret(expectedSecret.getSecretId())).thenReturn(expectedSecret);
    getCliWithArgs(
            GetSecretAmphoraClientCliCommandConfig.COMMAND_NAME,
            expectedSecret.getSecretId().toString())
        .parse();
    Assert.assertEquals(Arrays.toString(expectedSecret.getData()), systemOutRule.getLog().trim());
  }

  @Test
  public void downloadSuccessWithTagsTest()
      throws CsCliException, CsCliRunnerException, AmphoraClientException,
          SecretVerificationException, CsCliLoginException {
    Secret expectedSecret =
        Secret.of(
            UUID.randomUUID(),
            Collections.singletonList(Tag.builder().key("key_1").value("value_1").build()),
            new BigInteger[] {BigInteger.TEN});
    when(amphoraClientMock.getSecret(expectedSecret.getSecretId())).thenReturn(expectedSecret);
    getCliWithArgs(
            GetSecretAmphoraClientCliCommandConfig.COMMAND_NAME,
            expectedSecret.getSecretId().toString())
        .parse();
    Assert.assertEquals(
        SecretPrinter.secretToString(expectedSecret), systemOutRule.getLog().trim());
  }

  @Test
  public void listSuccessTest()
      throws CsCliException, CsCliRunnerException, AmphoraClientException, CsCliLoginException {
    List<Metadata> secretMetadataList = AmphoraTestData.getMetadataList();
    when(amphoraClientMock.getSecrets(emptyList(), (Sort) null)).thenReturn(secretMetadataList);
    getCliWithArgs(ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME).parse();
    Assert.assertThat(
        systemOutRule.getLog(),
        containsString(SecretPrinter.metadataListToString(secretMetadataList, false)));
  }

  @Test
  public void testListSecretsFailed()
      throws CsCliException, AmphoraClientException, CsCliLoginException {
    String errorMsg = "Sth went wrong";
    when(amphoraClientMock.getSecrets(emptyList(), (Sort) null))
        .thenThrow(new AmphoraClientException(errorMsg));
    try {
      getCliWithArgs(ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME).parse();
      fail("Exception expected");
    } catch (CsCliRunnerException scre) {
      Assert.assertThat(scre.getMessage(), containsString(errorMsg));
    }
  }

  @Test
  public void listWithIdsOnlySuccessTest()
      throws CsCliException, CsCliRunnerException, AmphoraClientException, CsCliLoginException {
    List<Metadata> secretMetadataList = AmphoraTestData.getMetadataList();
    when(amphoraClientMock.getSecrets(emptyList(), (Sort) null)).thenReturn(secretMetadataList);
    getCliWithArgs(ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME, "-l").parse();
    Assert.assertThat(
        systemOutRule.getLog(),
        containsString(SecretPrinter.metadataListToString(secretMetadataList, true)));
  }

  @Test
  public void givenInvalidTagFilterString_whenListSecrets_thenExceptionIsThrown() {
    String invalidTagFilterString = "key=";

    CsCliRunnerException actualCCRE =
        assertThrows(
            CsCliRunnerException.class,
            () ->
                getCliWithArgs(
                        ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME,
                        "-l",
                        "-f",
                        invalidTagFilterString)
                    .parse());
    assertEquals(
        MessageFormat.format(
            amphoraMessages.getString("list.failure.invalid-tag-filter"), invalidTagFilterString),
        actualCCRE.getMessage());
  }

  @Test
  public void givenEmptyTagFilterString_whenListSecrets_thenExceptionIsThrown() {
    String emptyTagFilterString = "";

    CsCliRunnerException actualCCRE =
        assertThrows(
            CsCliRunnerException.class,
            () ->
                getCliWithArgs(
                        ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME,
                        "-l",
                        "-f",
                        emptyTagFilterString)
                    .parse());
    assertEquals(
        MessageFormat.format(
            amphoraMessages.getString("list.failure.invalid-tag-filter"), emptyTagFilterString),
        actualCCRE.getMessage());
  }

  @Test
  public void givenValidTagFilters_whenListSecrets_thenCallClientWithExpectedArguments()
      throws AmphoraClientException, CsCliRunnerException, CsCliLoginException, CsCliException {
    TagFilter expectedTagFilter1 = TagFilter.with("time", "42", TagFilterOperator.LESS_THAN);
    TagFilter expectedTagFilter2 = TagFilter.with("type", "temperature", TagFilterOperator.EQUALS);

    getCliWithArgs(
            ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME,
            "-l",
            "-f",
            tagFilterToString(expectedTagFilter1),
            "-f",
            tagFilterToString(expectedTagFilter2))
        .parse();

    ArgumentCaptor<ArrayList> listArgumentCaptor = ArgumentCaptor.forClass(ArrayList.class);
    verify(amphoraClientMock, times(1)).getSecrets(listArgumentCaptor.capture(), eq((Sort) null));
    assertEquals(
        Arrays.asList(expectedTagFilter1, expectedTagFilter2), listArgumentCaptor.getValue());
  }

  @Test
  public void givenInvalidSortConfig_whenListSecrets_thenExceptionIsThrown() {
    String invalidSortString = "key:DOWN";

    CsCliRunnerException actualCCRE =
        assertThrows(
            CsCliRunnerException.class,
            () ->
                getCliWithArgs(
                        ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME,
                        "-l",
                        "-s",
                        invalidSortString)
                    .parse());
    assertEquals(
        MessageFormat.format(
            amphoraMessages.getString("list.failure.invalid-sort-format"), invalidSortString),
        actualCCRE.getMessage());
  }

  @Test
  public void givenEmptySortConfig_whenListSecrets_thenExceptionIsThrown() {
    String emptySortString = "";

    CsCliRunnerException actualCCRE =
        assertThrows(
            CsCliRunnerException.class,
            () ->
                getCliWithArgs(
                        ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME,
                        "-l",
                        "-s",
                        emptySortString)
                    .parse());
    assertEquals(
        MessageFormat.format(
            amphoraMessages.getString("list.failure.invalid-sort-format"), emptySortString),
        actualCCRE.getMessage());
  }

  @Test
  public void givenValidSortConfig_whenListSecrets_thenCallClientWithExpectedArguments()
      throws AmphoraClientException, CsCliRunnerException, CsCliLoginException, CsCliException {
    Sort expectedSortConfig = Sort.by("time", Sort.Order.ASC);

    getCliWithArgs(
            ListSecretsAmphoraClientCliCommandConfig.COMMAND_NAME,
            "-l",
            "-s",
            String.format("%s:%s", expectedSortConfig.getProperty(), expectedSortConfig.getOrder()))
        .parse();

    ArgumentCaptor<Sort> sortArgumentCaptor = ArgumentCaptor.forClass(Sort.class);
    verify(amphoraClientMock, times(1)).getSecrets(anyList(), sortArgumentCaptor.capture());
    assertEquals(expectedSortConfig, sortArgumentCaptor.getValue());
  }

  @Test
  public void deleteSecretsSuccess()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    UUID delID1 = UUID.randomUUID();
    UUID delID2 = UUID.randomUUID();

    ArgumentCaptor<UUID> amphoraIDCaptor = ArgumentCaptor.forClass(UUID.class);
    getCliWithArgs(
            DeleteSecretsAmphoraClientCliCommandConfig.COMMAND_NAME,
            delID1.toString(),
            delID2.toString())
        .parse();
    verify(amphoraClientMock, times(2)).deleteSecret(amphoraIDCaptor.capture());
    List<UUID> capturedIDs = amphoraIDCaptor.getAllValues();
    Assert.assertThat(capturedIDs, hasItem(delID1));
    Assert.assertThat(capturedIDs, hasItem(delID2));
    Assert.assertThat(systemOutRule.getLog(), containsString("Secrets deleted"));
  }

  @Test
  public void deleteSecretsSuccessCli()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    UUID delID1 = UUID.randomUUID();
    UUID delID2 = UUID.randomUUID();
    systemInMock.provideLines(delID1.toString(), delID2.toString());

    ArgumentCaptor<UUID> amphoraIDCaptor = ArgumentCaptor.forClass(UUID.class);
    getCliWithArgs(DeleteSecretsAmphoraClientCliCommandConfig.COMMAND_NAME).parse();

    verify(amphoraClientMock, times(2)).deleteSecret(amphoraIDCaptor.capture());
    List<UUID> capturedIDs = amphoraIDCaptor.getAllValues();
    Assert.assertThat(capturedIDs, hasItem(delID1));
    Assert.assertThat(capturedIDs, hasItem(delID2));
    Assert.assertThat(systemOutRule.getLog(), containsString("Secrets deleted"));
  }

  @Test(expected = CsCliRunnerException.class)
  public void deleteSecretsFailSuccessMix()
      throws CsCliException, CsCliRunnerException, IOException, CsCliLoginException {
    HashMap<UUID, Boolean> delIds = new HashMap<>();
    final int NUM_VARS = 100;
    for (int i = 0; i < NUM_VARS; i++) {
      UUID uuid = UUID.randomUUID();
      delIds.put(uuid, (i % 3 == 0) ? Boolean.FALSE : Boolean.TRUE);
      if (i % 3 == 0) {
        doThrow(new AmphoraClientException("Unable to delete secret"))
            .when(amphoraClientMock)
            .deleteSecret(uuid);
      }
    }
    // Pattern to check for UUIDs
    final Pattern uuidPattern =
        Pattern.compile(
            "^([^\\s]+).*([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}).*$");
    ArgumentCaptor<UUID> amphoraIDCaptor = ArgumentCaptor.forClass(UUID.class);
    try {
      getCliWithArgs(
              ArrayUtils.addAll(
                  new String[] {DeleteSecretsAmphoraClientCliCommandConfig.COMMAND_NAME},
                  delIds.keySet().stream().map(UUID::toString).toArray(String[]::new)))
          .parse();
    } catch (Exception e) {
      Map<String, Long> numResponseIds =
          Arrays.stream(systemOutRule.getLog().split("\\r?\\n"))
              .map(x -> uuidPattern.matcher(x))
              .filter(x -> x.matches())
              .filter(x -> x.groupCount() == 2)
              .peek(
                  x -> {
                    // If the line had 2 matches, it should either say success or fail
                    if (delIds.get(UUID.fromString(x.group(2)))) {
                      Assert.assertThat(x.group(1), containsString("Success"));
                    } else {
                      Assert.assertThat(x.group(1), containsString("Fail"));
                    }
                  })
              .map(x -> x.group(2))
              .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
      // Check the number of returned values - to validate that the exception throwing did not
      // prematurely
      // terminate some instances.
      Assert.assertEquals(
          "Number of received IDs not equal",
          delIds.keySet().size(),
          numResponseIds.keySet().size());
      throw e;
    }
  }

  @Test
  public void deleteSecretsFailesUnexpected()
      throws CsCliException, IOException, CsCliLoginException {
    String errorMsg = "Sth went wrong";
    UUID secretId = UUID.randomUUID();
    doThrow(new RuntimeException(errorMsg)).when(amphoraClientMock).deleteSecret(secretId);
    try {
      getCliWithArgs(DeleteSecretsAmphoraClientCliCommandConfig.COMMAND_NAME, secretId.toString())
          .parse();
      fail("Exception expected");
    } catch (CsCliRunnerException scre) {
      Assert.assertThat(scre.getMessage(), containsString("Failed to delete secrets"));
    }
  }

  @Test
  public void testCreateTag()
      throws CsCliException, CsCliRunnerException, AmphoraClientException, CsCliLoginException {
    UUID secretId = UUID.randomUUID();
    Tag tag = Tag.builder().key("key").value("value").build();
    getCliWithArgs(
            CreateTagAmphoraClientCliCommandConfig.COMMAND_NAME,
            "-i",
            secretId.toString(),
            String.format("%s=%s", tag.getKey(), tag.getValue()))
        .parse();
    verify(amphoraClientMock, times(1)).createTag(secretId, tag);
  }

  @Test
  public void testCreateTagFailed()
      throws CsCliException, AmphoraClientException, CsCliLoginException {
    String errorMsg = "Sth went wrong";
    UUID secretId = UUID.randomUUID();
    Tag tag = Tag.builder().key("key").value("value").build();
    doThrow(new AmphoraClientException(errorMsg)).when(amphoraClientMock).createTag(secretId, tag);
    try {
      getCliWithArgs(
              CreateTagAmphoraClientCliCommandConfig.COMMAND_NAME,
              "-i",
              secretId.toString(),
              String.format("%s=%s", tag.getKey(), tag.getValue()))
          .parse();
      fail("Exception expected");
    } catch (CsCliRunnerException scre) {
      Assert.assertThat(scre.getMessage(), containsString(errorMsg));
    }
  }

  @Test
  public void testGetTag()
      throws CsCliException, CsCliRunnerException, AmphoraClientException, CsCliLoginException {
    UUID secretId = UUID.randomUUID();
    Tag tag1 = Tag.builder().key("key_1").value("value_1").build();
    when(amphoraClientMock.getTag(secretId, tag1.getKey())).thenReturn(tag1);
    getCliWithArgs(
            GetTagAmphoraClientCliCommandConfig.COMMAND_NAME,
            "-i",
            secretId.toString(),
            tag1.getKey())
        .parse();
    Assert.assertThat(
        systemOutRule.getLog(), containsString(SecretPrinter.tagToString(tag1).trim()));
  }

  @Test
  public void testGetTagFailed()
      throws CsCliException, AmphoraClientException, CsCliLoginException {
    String errorMsg = "Sth went wrong";
    UUID secretId = UUID.randomUUID();
    Tag tag1 = Tag.builder().key("key_1").value("value_1").build();
    when(amphoraClientMock.getTag(secretId, tag1.getKey()))
        .thenThrow(new AmphoraClientException(errorMsg));
    try {
      getCliWithArgs(
              GetTagAmphoraClientCliCommandConfig.COMMAND_NAME,
              "-i",
              secretId.toString(),
              tag1.getKey())
          .parse();
      fail("Exception expected");
    } catch (CsCliRunnerException scre) {
      Assert.assertThat(scre.getMessage(), containsString(errorMsg));
    }
  }

  @Test
  public void testGetTags()
      throws CsCliException, CsCliRunnerException, AmphoraClientException, CsCliLoginException {
    UUID secretId = UUID.randomUUID();
    Tag tag1 = Tag.builder().key("key_1").value("value_1").build();
    Tag tag2 = Tag.builder().key("2-key").value("2-value").build();
    when(amphoraClientMock.getTags(secretId)).thenReturn(Arrays.asList(tag1, tag2));
    getCliWithArgs(GetTagsAmphoraClientCliCommandConfig.COMMAND_NAME, secretId.toString()).parse();
    Assert.assertThat(
        systemOutRule.getLog(), containsString(SecretPrinter.tagToString(tag1).trim()));
    Assert.assertThat(
        systemOutRule.getLog(), containsString(SecretPrinter.tagToString(tag2).trim()));
  }

  @Test
  public void testGetTagsFailed()
      throws CsCliException, AmphoraClientException, CsCliLoginException {
    String errorMsg = "Sth went wrong";
    UUID secretId = UUID.randomUUID();
    when(amphoraClientMock.getTags(secretId)).thenThrow(new AmphoraClientException(errorMsg));
    try {
      getCliWithArgs(GetTagsAmphoraClientCliCommandConfig.COMMAND_NAME, secretId.toString())
          .parse();
      fail("Exception expected");
    } catch (CsCliRunnerException scre) {
      Assert.assertThat(scre.getMessage(), containsString(errorMsg));
    }
  }

  @Test
  public void testOverwriteTags()
      throws CsCliException, CsCliRunnerException, AmphoraClientException, CsCliLoginException {
    UUID secretId = UUID.randomUUID();
    Tag tag = Tag.builder().key("key").value("value").build();
    getCliWithArgs(
            OverwriteTagsAmphoraClientCliCommandConfig.COMMAND_NAME,
            "-i",
            secretId.toString(),
            String.format("%s=%s", tag.getKey(), tag.getValue()))
        .parse();
    verify(amphoraClientMock, times(1)).overwriteTags(secretId, Collections.singletonList(tag));
  }

  @Test
  public void testOverwriteTagsFailed()
      throws CsCliException, CsCliRunnerException, AmphoraClientException, CsCliLoginException {
    String errorMsg = "Sth went wrong";
    UUID secretId = UUID.randomUUID();
    Tag tag = Tag.builder().key("key").value("value").build();
    doThrow(new AmphoraClientException(errorMsg))
        .when(amphoraClientMock)
        .overwriteTags(eq(secretId), any());
    try {
      getCliWithArgs(
              OverwriteTagsAmphoraClientCliCommandConfig.COMMAND_NAME,
              "-i",
              secretId.toString(),
              String.format("%s=%s", tag.getKey(), tag.getValue()))
          .parse();
      fail("Exception expected");
    } catch (CsCliRunnerException scre) {
      Assert.assertThat(scre.getMessage(), containsString(errorMsg));
    }
  }

  @Test
  public void testUpdateTag()
      throws CsCliException, CsCliRunnerException, AmphoraClientException, CsCliLoginException {
    UUID secretId = UUID.randomUUID();
    Tag tag = Tag.builder().key("key").value("value").build();
    getCliWithArgs(
            UpdateTagAmphoraClientCliCommandConfig.COMMAND_NAME,
            "-i",
            secretId.toString(),
            String.format("%s=%s", tag.getKey(), tag.getValue()))
        .parse();
    verify(amphoraClientMock, times(1)).updateTag(secretId, tag);
  }

  @Test
  public void testUpdateTagFailed()
      throws CsCliException, AmphoraClientException, CsCliLoginException {
    String errorMsg = "Sth went wrong";
    UUID secretId = UUID.randomUUID();
    Tag tag = Tag.builder().key("key").value("value").build();
    doThrow(new AmphoraClientException(errorMsg)).when(amphoraClientMock).updateTag(secretId, tag);
    try {
      getCliWithArgs(
              UpdateTagAmphoraClientCliCommandConfig.COMMAND_NAME,
              "-i",
              secretId.toString(),
              String.format("%s=%s", tag.getKey(), tag.getValue()))
          .parse();
      fail("Exception expected");
    } catch (CsCliRunnerException scre) {
      Assert.assertThat(scre.getMessage(), containsString(errorMsg));
    }
  }

  @Test
  public void testDeleteTag()
      throws CsCliException, CsCliRunnerException, AmphoraClientException, CsCliLoginException {
    UUID secretId = UUID.randomUUID();
    Tag tag = Tag.builder().key("key").value("value").build();
    getCliWithArgs(
            DeleteTagAmphoraClientCliCommandConfig.COMMAND_NAME,
            "-i",
            secretId.toString(),
            tag.getKey())
        .parse();
    verify(amphoraClientMock, times(1)).deleteTag(secretId, tag.getKey());
  }

  @Test
  public void testDeleteTagFailed()
      throws CsCliException, AmphoraClientException, CsCliLoginException {
    String errorMsg = "Sth went wrong";
    UUID secretId = UUID.randomUUID();
    Tag tag = Tag.builder().key("key").value("value").build();
    doThrow(new AmphoraClientException(errorMsg))
        .when(amphoraClientMock)
        .deleteTag(secretId, tag.getKey());
    try {
      getCliWithArgs(
              DeleteTagAmphoraClientCliCommandConfig.COMMAND_NAME,
              "-i",
              secretId.toString(),
              tag.getKey())
          .parse();
      fail("Exception expected");
    } catch (CsCliRunnerException scre) {
      Assert.assertThat(scre.getMessage(), containsString(errorMsg));
    }
  }

  private String tagFilterToString(TagFilter tagFilter) {
    return String.format(
        "%s%s%s", tagFilter.getKey(), tagFilter.getOperator(), tagFilter.getValue());
  }
}
