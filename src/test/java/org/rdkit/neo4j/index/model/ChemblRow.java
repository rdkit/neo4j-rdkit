package org.rdkit.neo4j.index.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChemblRow {
  private String molId;
  private String docId;
  private String smiles;

  public ChemblRow(String ... elements) {
    assert elements.length == 3;

    docId = elements[0];
    molId = elements[1];
    smiles = elements[2];
  }
}
