package org.rdkit.neo4j.models;

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

import java.util.Arrays;

/**
 * Enum with property names for the node object
 */
public enum NodeFields {

  MdlMol("mdlmol"),
  Smiles("smiles"),
  CanonicalSmiles("canonical_smiles"),
  InchiKey("inchi_key"),
  Formula("formula"),
  MolecularWeight("molecular_weight"),
  FingerprintEncoded("fp"),
  FingerprintOnes("fp_ones"); // name is used for compatability with `similarity` searches

  private final String value;

  NodeFields(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static NodeFields from(String val) {
    return Arrays.stream(NodeFields.values()).filter(nf -> nf.value.equals(val)).findFirst().orElseThrow(IllegalArgumentException::new);
  }

}
