package org.rdkit.neo4j.models;

public enum NodeFields {

  Chemical("Chemical"),
  Structure("Structure"),
  MdlMol("mdlmol"),
  Smiles("smiles"),
  CanonicalSmiles("canonical_smiles"),
  Inchi("inchi"),
  Formula("formula"),
  MolecularWeight("molecular_weight");


  private final String value;

  NodeFields(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
