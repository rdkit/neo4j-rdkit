package org.rdkit.neo4j.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum NodeFields {

  MdlMol("mdlmol"),
  Smiles("smiles"),
  CanonicalSmiles("canonical_smiles"),
  Inchi("inchi"),
  Formula("formula"),
  MolecularWeight("molecular_weight"),
  FingerprintEncoded("fp"),
  FingerprintOnes("fp_ones");


  @Getter
  private final String value;
}
