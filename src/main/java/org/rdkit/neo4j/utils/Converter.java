package org.rdkit.neo4j.utils;

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

import java.util.BitSet;
import lombok.val;
import org.RDKit.MolDraw2DSVG;
import org.RDKit.MolSanitizeException;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.rdkit.fingerprint.DefaultFingerprintFactory;
import org.rdkit.fingerprint.DefaultFingerprintSettings;
import org.rdkit.fingerprint.FingerprintFactory;
import org.rdkit.fingerprint.FingerprintSettings;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.models.LuceneQuery;
import org.rdkit.neo4j.models.NodeParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter class
 * Provides functionality to create {@link org.rdkit.neo4j.models.NodeParameters} and {@link org.rdkit.neo4j.models.LuceneQuery} objects
 * The content of LuceneQuery varies depending on FingerprintType used, which is defined during constructor.
 * Default converter (with {@link org.rdkit.fingerprint.FingerprintType#pattern} is used for SSS
 */
public class Converter {

  /*  Static methods and fields  */
  public static Converter createDefault() {
    return createConverter(FingerprintType.pattern);
  }

  public static Converter createConverter(FingerprintType fpType) {

    FingerprintSettings settings = new DefaultFingerprintSettings(fpType).setNumBits(2048);
    switch (fpType) {
      case pattern:
        break;
      case morgan:
        settings = settings.setRadius(2);
        break;
      case torsion:
        settings = settings.setTorsionPathLength(4);
        break;
      default: break;
    }

    FingerprintFactory factory = new DefaultFingerprintFactory(settings);
    return new Converter(factory, fpType);
  }

  public static String molToSVG(final RWMolCloseable molOrigin) {
    /*val molCopy = RWMolCloseable.from(molOrigin);

    RWMol mol;

    try {
      RDKFuncs.prepareMolForDrawing(molOrigin);
      mol = molOrigin;
    } catch(final MolSanitizeException ex) {
      // skip kekulization. If this still fails we throw up our hands
      RDKFuncs.prepareMolForDrawing(molCopy,false);
      mol = molCopy;
    }*/

    molOrigin.updatePropertyCache(false);
    RDKFuncs.prepareMolForDrawing(molOrigin); //,false);

    final MolDraw2DSVG molDrawing = new MolDraw2DSVG(300, 300);
    molDrawing.drawMolecule(molOrigin);
    molDrawing.finishDrawing();

    // the svg namespace causes problems with the javascript table (github #29)
    final String svg = molDrawing.getDrawingText().replaceAll("svg:", "").replaceAll("xmlns:svg=", "xmlns=");
    logger.trace("Created svg={}", svg);
    return svg;
  }

  private static final Logger logger = LoggerFactory.getLogger(Converter.class);

  public static final String DELIMITER_WHITESPACE = " ";
  public static final String DELIMITER_AND = " AND ";
  public static final String DELIMITER_OR = " OR ";

  /*  Class fields  */

  private FingerprintFactory fingerprintFactory;
  private FingerprintType fingerprintType;

  private Converter(FingerprintFactory fingerprintFactory, FingerprintType type) {
    this.fingerprintFactory = fingerprintFactory;
    this.fingerprintType = type;
  }

  public FingerprintType getFingerprintType() {
    return fingerprintType;
  }

  /**
   * Create NodeParameters from SMILES
   *
   * @param smiles not canonicalized
   * @param sanitize
   * @return NodeParameters object
   */
  public NodeParameters convertSmiles(final String smiles, boolean sanitize) {
    try (RWMolCloseable rwmol = RWMolCloseable.from(RWMol.MolFromSmiles(smiles, 0, sanitize))) {
//    try (RWMolCloseable rwmol = RWMolCloseable.from(RWMol.MolFromSmiles(smiles))) {
      final NodeParameters block = createMolBlock(rwmol, sanitize);

      final String rdkitSmiles = block.getCanonicalSmiles();

      if (rdkitSmiles.isEmpty()) {
        logger.error("Empty canonical smiles obtained from smiles=`{}`", smiles);
        throw new IllegalArgumentException("Empty canonical smiles obtained");
      }

      block.setSmiles(smiles);
      block.setMolBlock(RDKFuncs.MolToMolBlock(rwmol));

      return block;
    }
  }

  /**
   * Create NodeParameters from string equivalent
   *
   * @param molBlock in string format
   * @param sanitize
   * @return NodeParameters object
   */
  public NodeParameters convertMolBlock(final String molBlock, boolean sanitize) {
    try (RWMolCloseable rwmol = RWMolCloseable.from(RWMol.MolFromMolBlock(molBlock, sanitize))) {
      NodeParameters block = createMolBlock(rwmol, sanitize);
      block.setMolBlock(molBlock);

      return block;
    }
  }

  /**
   * Method returns canonicalized SMILES
   *
   * @return canonicalized SMILES
   */
  public String getRDKitSmiles(String smiles) {
    try (RWMolCloseable rwmol = RWMolCloseable.from(RWMol.MolFromSmiles(smiles))) {
      return RDKFuncs.MolToSmiles(rwmol);
    }
  }

  public LuceneQuery getLuceneFingerprint(String smiles, boolean sanitize) {
    logger.debug("Get Lucene fingerprint from smiles={}", smiles);
    return getLuceneQuery(smiles, DELIMITER_WHITESPACE, sanitize);
  }

  public LuceneQuery getLuceneFingerprint(RWMol mol, boolean sanitize) {
    logger.debug("Get Lucene fingerprint from mol");
    return getLuceneQuery(mol, DELIMITER_WHITESPACE, sanitize);
  }

  public LuceneQuery getLuceneSimilarityQuery(String smiles, boolean sanitize) {
    logger.debug("Get Lucene similairy query for smiles={}", smiles);
    return getLuceneQuery(smiles, DELIMITER_OR, sanitize);
  }

  public LuceneQuery getLuceneSimilarityQuery(RWMol mol, boolean sanitize) {
    logger.debug("Get Lucene similairy query for mol");
    return getLuceneQuery(mol, DELIMITER_OR, sanitize);
  }

  /**
   * Return encoded query object with string for lucene fulltext query and count of set bits
   *
   * @param smiles to convert for further LuceneQuery
   * @param sanitize
   * @return ex.: { str="3 AND 5 AND 14 AND 256 AND 258", int=5 }
   */
  public LuceneQuery getLuceneSSSQuery(String smiles, boolean sanitize) {
    logger.debug("Get Lucene fp query for smiles={}", smiles);
    return getLuceneQuery(smiles, DELIMITER_AND, sanitize);
  }

  /**
   * Return encoded query object with string for lucene fulltext query and count of set bits
   *
   * @param mol to user for further construction LuceneQuery
   * @param sanitize
   * @return ex.: { str="3 AND 5 AND 14 AND 256 AND 258", int=5 }
   */
  public LuceneQuery getLuceneSSSQuery(ROMol mol, boolean sanitize) {
    logger.debug("Get Lucene fp query for mol");
    return getLuceneQuery(mol, DELIMITER_AND, sanitize);
  }

  private LuceneQuery getLuceneQuery(ROMol mol, final String delimiter, boolean sanitize) {
    final BitSet fp = fingerprintFactory.createStructureFingerprint(mol, sanitize);
    return getLuceneQuery(fp, delimiter);
  }

  private LuceneQuery getLuceneQuery(String smiles, final String delimiter, boolean sanitize) {
    final BitSet fp = fingerprintFactory.createStructureFingerprint(smiles, sanitize);
    return getLuceneQuery(fp, delimiter);
  }

  private LuceneQuery getLuceneQuery(final BitSet fp, final String delimiter) {
    LuceneQuery luceneQuery = new LuceneQuery(fp, delimiter);
    logger.debug("Lucene fp luceneQuery={}", luceneQuery);
    return luceneQuery;
  }

  /**
   * Method fulfills the NodeParameters with parameters from rwmol object Used to extend properties of the node
   *
   * @param rwmol object
   * @param sanitize
   * @return NodeParameters with fields
   */
  private NodeParameters createMolBlock(final RWMol rwmol, boolean sanitize) {
    logger.debug("Construct default molBlock fields");
    final String rdkitSmiles = RDKFuncs.MolToSmiles(rwmol);
    rwmol.updatePropertyCache(sanitize);
    final String formula = RDKFuncs.calcMolFormula(rwmol);
    final double molecularWeight = RDKFuncs.calcExactMW(rwmol);
    final String inchi = RDKFuncs.MolToInchiKey(rwmol);

    logger.debug("Construct structure fingerprint for lucene");
    LuceneQuery luceneQuery = getLuceneQuery(rwmol, DELIMITER_WHITESPACE, sanitize);

    final long fingerprintOnes = luceneQuery.getPositiveBits();
    final String fingerprintEncoded = luceneQuery.getLuceneQuery();

    logger.debug("Constructed fp encoded={}", fingerprintEncoded);
    return new NodeParameters(rdkitSmiles, formula, molecularWeight, inchi, fingerprintEncoded, fingerprintOnes);
  }
}
