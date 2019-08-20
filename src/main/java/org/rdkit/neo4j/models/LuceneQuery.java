package org.rdkit.neo4j.models;

import java.util.BitSet;
import java.util.StringJoiner;
import lombok.Getter;
import lombok.ToString;

@ToString
public class LuceneQuery {
  @Getter
  private final String luceneQuery;
  @Getter
  private final long positiveBits;
  @Getter
  private final String delimiter;

  /**
   * Convert a bitset into a string with specified `delimiter`
   *
   * @param fingerprint to convert into lucene string
   * @param delimiter to join
   */
  public LuceneQuery(final BitSet fingerprint, final String delimiter) {
    int counter = 0;
    StringJoiner joiner = new StringJoiner(delimiter);

    // if i == -1, the FP has ended
    for (int i = fingerprint.nextSetBit(0); i >= 0; i = fingerprint.nextSetBit(i + 1)) {
      joiner.add(Integer.toString(i));
      counter++;
    }

    this.luceneQuery = joiner.toString();
    this.positiveBits = counter;
    this.delimiter = delimiter;
  }
}
