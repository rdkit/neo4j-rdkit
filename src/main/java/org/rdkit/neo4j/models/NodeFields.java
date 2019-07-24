package org.rdkit.neo4j.models;

public enum NodeFields {

  Chemical("Chemical"),
  Structure("Structure"),
  IndexName("rdkit"),

  MdlMol("mdlmol"),
  Smiles("smiles"),
  CanonicalSmiles("canonical_smiles"),
  Inchi("inchi"),
  Formula("formula"),
  MolecularWeight("molecular_weight"),
  FingerprintEncoded("fp"),
  FingerprintOnes("fp_ones");


  private final String value;

  NodeFields(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
