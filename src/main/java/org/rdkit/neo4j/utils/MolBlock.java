package org.rdkit.neo4j.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MolBlock {
  private final String molBlock;
  private final String rdkitSmiles;
  private final String formula;
  private final double molecularWeight;
  private final String inchi;
}
