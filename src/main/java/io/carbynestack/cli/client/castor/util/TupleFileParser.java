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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
      bytes = getTuplesWithoutHeader(fileInputStream);
    } catch (IOException e) {
      log.error(String.format("Cannot read file %s", tupleFile.getPath()));
      throw new RuntimeException();
    }

    return TupleChunk.of(tupleType.getTupleCls(), tupleType.getField(), chunkId, bytes);
  }

  /**
   * Removes the File Header that was introduced in MP-SPDZ v0.2.8
   *
   * @param tupleStreamWithHeader The file inputstream that includes the header
   * @return The file bytes shifted by the header
   */
  private static byte[] getTuplesWithoutHeader(FileInputStream tupleStreamWithHeader)
      throws IOException {
    long spdzHeaderLength = getSpdzHeaderLength(tupleStreamWithHeader);
    long skipedLength = tupleStreamWithHeader.skip(spdzHeaderLength);

    if (spdzHeaderLength != skipedLength) {
      throw new IOException("can not skip mp-spdz headers!");
    }
    return toByteArray(tupleStreamWithHeader);
  }

  private static long getSpdzHeaderLength(FileInputStream tupleStreamWithHeader)
      throws IOException {
    // See https://github.com/data61/MP-SPDZ/issues/418#issuecomment-975424591 for header
    // Starts with LongEndian 8 Byte Number depicting the header length, followed by [headerLength]
    // bytes
    // TODO: It would be possible to check the prime here, so that no incorrect values are uploaded,
    //  but Castor has no access to it.
    byte[] headerLengthBytes = toByteArray(tupleStreamWithHeader, 8);
    return ByteBuffer.wrap(headerLengthBytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
  }
}
