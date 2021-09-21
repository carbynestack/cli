/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.castor.util;

import static org.apache.commons.io.IOUtils.toByteArray;

import io.carbynestack.castor.common.entities.TupleChunk;
import io.carbynestack.castor.common.entities.TupleType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TupleFileParser {

  /**
   * Reads and parses a SPDZ tuple file.
   *
   * @param tupleType Tupletype for the tuples to be uploaded
   * @param tupleFile Absolute path to the tuple file
   * @param chunkId assigned chunkId
   * @return The read tuples as TupleChunk
   */
  public static TupleChunk parse(TupleType tupleType, File tupleFile, UUID chunkId) {
    if (!tupleFile.isFile()) {
      log.error(String.format("%s is not a file.", tupleFile.getAbsolutePath()));
      throw new IllegalArgumentException();
    }

    byte[] bytes;
    try (FileInputStream fileInputStream = new FileInputStream(tupleFile)) {
      bytes = toByteArray(fileInputStream);
    } catch (IOException e) {
      log.error(String.format("Cannot read file %s", tupleFile.getPath()));
      throw new RuntimeException();
    }

    return TupleChunk.of(tupleType.getTupleCls(), tupleType.getField(), chunkId, bytes);
  }
}
