package org.rdkit.neo4j.models;

/*-
 * #%L
 * RDKit-Neo4j
 * %%
 * Copyright (C) 2019 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

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
