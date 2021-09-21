/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.client.amphora;

import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.castor.common.entities.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;

public class AmphoraTestData {

  static final InputMask<Field.Gfp>[] inputMasks =
      new InputMask[] {
        new InputMask(
            Field.GFP,
            new Share(
                RandomUtils.nextBytes(Field.GFP.getElementSize()),
                RandomUtils.nextBytes(Field.GFP.getElementSize()))),
        new InputMask(
            Field.GFP,
            new Share(
                RandomUtils.nextBytes(Field.GFP.getElementSize()),
                RandomUtils.nextBytes(Field.GFP.getElementSize())))
      };

  public static TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMasksToList(
      InputMask<Field.Gfp>... inputMasks) {
    TupleList tupleList = new TupleList<>(TupleType.INPUT_MASK_GFP.getTupleCls(), Field.GFP);
    Collections.addAll(tupleList, inputMasks);
    return tupleList;
  }

  static List<Metadata> getMetadataList() {
    List<Tag> emptyTags = new ArrayList<>();
    emptyTags.add(null);
    List<Metadata> metadataList = new ArrayList<>();
    metadataList.add(Metadata.builder().secretId(UUID.randomUUID()).tags(getTags()).build());
    metadataList.add(Metadata.builder().secretId(UUID.randomUUID()).tags(getTags()).build());
    metadataList.add(Metadata.builder().secretId(UUID.randomUUID()).tags(emptyTags).build());
    return metadataList;
  }

  static List<Tag> getTags() {
    Tag tag1 = Tag.builder().key("key1").value("tag1").build();
    Tag tag2 = Tag.builder().key("key2").value("tag2").build();
    List<Tag> tags = new ArrayList<>();
    tags.add(tag1);
    tags.add(tag2);
    return tags;
  }
}
