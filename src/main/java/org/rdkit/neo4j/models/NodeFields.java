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
