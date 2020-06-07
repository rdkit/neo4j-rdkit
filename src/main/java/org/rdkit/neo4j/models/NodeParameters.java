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

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Class stores built parameters from RDKit function call
 * Used as an intermediate storage of parameters, later those are saved in a node object as properties
 */

@Data
@ToString
@RequiredArgsConstructor
public class NodeParameters {
  private final String canonicalSmiles;
  private final String formula;
  private final double molecularWeight;
  private final String inchiKey;
  private final String fingerprintEncoded;
  private final long fingerpintOnes;
  private String molBlock;
  private String smiles;
}
