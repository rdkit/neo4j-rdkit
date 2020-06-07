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

/**
 * Class stores built parameters from RDKit function call
 * Used as an intermediate storage of parameters, later those are saved in a node object as properties
 */

public class NodeParameters {
  private final String canonicalSmiles;
  private final String formula;
  private final double molecularWeight;
  private final String inchiKey;
  private final String fingerprintEncoded;
  private final long fingerpintOnes;
  private String molBlock;
  private String smiles;

  public NodeParameters(String canonicalSmiles, String formula, double molecularWeight, String inchiKey, String fingerprintEncoded, long fingerpintOnes) {
    this.canonicalSmiles = canonicalSmiles;
    this.formula = formula;
    this.molecularWeight = molecularWeight;
    this.inchiKey = inchiKey;
    this.fingerprintEncoded = fingerprintEncoded;
    this.fingerpintOnes = fingerpintOnes;
  }

  public String getCanonicalSmiles() {
    return canonicalSmiles;
  }

  public String getFormula() {
    return formula;
  }

  public double getMolecularWeight() {
    return molecularWeight;
  }

  public String getInchiKey() {
    return inchiKey;
  }

  public String getFingerprintEncoded() {
    return fingerprintEncoded;
  }

  public long getFingerpintOnes() {
    return fingerpintOnes;
  }

  public String getMolBlock() {
    return molBlock;
  }

  public String getSmiles() {
    return smiles;
  }

  public void setMolBlock(String molBlock) {
    this.molBlock = molBlock;
  }

  public void setSmiles(String smiles) {
    this.smiles = smiles;
  }
}
