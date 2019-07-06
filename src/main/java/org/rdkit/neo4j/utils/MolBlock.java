package org.rdkit.neo4j.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@ToString
@RequiredArgsConstructor
public class MolBlock {
  private final String canonicalSmiles;
  private final String formula;
  private final double molecularWeight;
  private final String inchi;
  private String molBlock;
  private String smiles;
}
