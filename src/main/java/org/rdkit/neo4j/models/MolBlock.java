package org.rdkit.neo4j.models;

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
  private final String fingerprintEncoded;
  private final int fingerpintOnes;
  private String molBlock;
  private String smiles;
}
