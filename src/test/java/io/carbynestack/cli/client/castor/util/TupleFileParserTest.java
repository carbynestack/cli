package io.carbynestack.cli.client.castor.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import io.carbynestack.castor.common.entities.TupleChunk;
import io.carbynestack.castor.common.entities.TupleType;
import io.carbynestack.cli.client.castor.CastorClientCliTest;
import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.mockito.Mockito;

public class TupleFileParserTest {

  private static final String TEST_TRIPLE_FILE_NAME = "Triples-p-P0-10000";
  private static final String TUPLE_FILE_PATH =
      CastorClientCliTest.class.getClassLoader().getResource(TEST_TRIPLE_FILE_NAME).getPath();
  private static final byte[] KNOWN_HEADER =
      new byte[] {
        29, 0, 0, 0, 0, 0, 0, 0, 83, 80, 68, 90, 32, 103, 102, 112, 0, 16, 0, 0, 0, -128, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 27, -128, 1
      };

  private static ArrayStartMatcher startsWith(byte[] start) {
    return new ArrayStartMatcher(start);
  }

  @Test
  public void parsesInputsToTupleChunk() {

    final TupleType tupleType = TupleType.valueOf("MULTIPLICATION_TRIPLE_GFP");
    final UUID chunkId = UUID.randomUUID();

    final TupleChunk tupleChunk =
        TupleFileParser.parse(tupleType, new File(TUPLE_FILE_PATH), chunkId);

    assertThat(tupleChunk, is(notNullValue()));
    assertThat(tupleChunk.getTupleType(), is(equalTo(tupleType)));
    assertThat(tupleChunk.getChunkId(), is(equalTo(chunkId)));

    // make sure tuples don't include header part which is provided via tuples file
    assertThat(tupleChunk.getTuples(), not(startsWith(KNOWN_HEADER)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void handlesNonFileAsFileInput() {

    File tupleFile = Mockito.mock(File.class);
    when(tupleFile.isFile()).thenReturn(false);

    TupleFileParser.parse(
        TupleType.valueOf("MULTIPLICATION_TRIPLE_GFP"), tupleFile, UUID.randomUUID());
  }

  private static final class ArrayStartMatcher extends TypeSafeMatcher<byte[]> {

    private final byte[] start;

    private ArrayStartMatcher(byte[] start) {
      this.start = start;
    }

    @Override
    protected boolean matchesSafely(byte[] item) {
      if (item.length < start.length) {
        return false;
      }

      for (int i = 0; i < start.length; i++) {
        if (item[i] != start[i]) {
          return false;
        }
      }
      return true;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("start with Array " + Arrays.toString(start));
    }
  }
}
