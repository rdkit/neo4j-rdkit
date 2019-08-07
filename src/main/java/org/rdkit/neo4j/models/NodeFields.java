package org.rdkit.neo4j.models;

import java.util.Arrays;
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

  public static NodeFields from(String val) {
    return Arrays.stream(NodeFields.values()).filter(nf -> nf.value.equals(val)).findFirst().orElseThrow(IllegalArgumentException::new);
  }

  @Getter
  private final String value;
}
