package org.rdkit.neo4j.index.model;

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
