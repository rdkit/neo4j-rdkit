package org.rdkit.neo4j.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SSSQuery {
  private final String luceneQuery;
  private final int positiveBits;
}
