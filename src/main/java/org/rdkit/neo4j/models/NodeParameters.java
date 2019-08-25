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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@ToString
@RequiredArgsConstructor
public class NodeParameters {
  private final String canonicalSmiles;
  private final String formula;
  private final double molecularWeight;
  private final String inchi;
  private final String fingerprintEncoded;
  private final long fingerpintOnes;
  private String molBlock;
  private String smiles;
}
