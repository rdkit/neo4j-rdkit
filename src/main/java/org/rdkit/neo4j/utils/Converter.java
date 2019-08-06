package org.rdkit.neo4j.utils;

import java.util.BitSet;
import org.RDKit.RDKFuncs;
import org.RDKit.RWMol;
import org.rdkit.fingerprint.DefaultFingerprintFactory;
import org.rdkit.fingerprint.DefaultFingerprintSettings;
import org.rdkit.fingerprint.FingerprintFactory;
import org.rdkit.fingerprint.FingerprintSettings;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.models.LuceneQuery;
import org.rdkit.neo4j.models.MolBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Converter {

  public static Converter createDefault() {
    return createConverter(FingerprintType.pattern);
  }

  public static Converter createConverter(FingerprintType fpType) {

    FingerprintSettings settings = new DefaultFingerprintSettings(fpType).setNumBits(2048);
    switch (fpType) {
      case pattern:
        break;
      case morgan:
        settings = settings
            .setRadius(2);
        break;
      case torsion:
        settings = settings
            .setTorsionPathLength(4);
        break;
      default: break;
    }

    FingerprintFactory factory = new DefaultFingerprintFactory(settings);
    return new Converter(factory, fpType);
  }

  private static final Logger logger = LoggerFactory.getLogger(Converter.class);

  static final String DELIMITER_WHITESPACE = " ";
  static final String DELIMITER_AND = " AND ";
  static final String DELIMITER_OR = " OR ";

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
   * Create MolBlock from SMILES
   *
   * @param smiles not canonicalized
   * @return MolBlock object
   */
  public MolBlock convertSmiles(final String smiles) {
    try (RWMolCloseable rwmol = RWMolCloseable.from(RWMol.MolFromSmiles(smiles))) {
      final MolBlock block = createMolBlock(rwmol);

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
   * Create MolBlock from string equivalent
   *
   * @param molBlock in string format
   * @return MolBlock object
   */
  public MolBlock convertMolBlock(final String molBlock) {
    try (RWMolCloseable rwmol = RWMolCloseable.from(RWMol.MolFromMolBlock(molBlock))) {
      MolBlock block = createMolBlock(rwmol);
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

  public LuceneQuery getLuceneFingerprint(String smiles) {
    logger.debug("Get Lucene fingerprint from smiles={}", smiles);
    return getLuceneQuery(smiles, DELIMITER_WHITESPACE);
  }

  public LuceneQuery getLuceneFingerprint(RWMol mol) {
    logger.debug("Get Lucene fingerprint from mol");
    return getLuceneQuery(mol, DELIMITER_WHITESPACE);
  }

  public LuceneQuery getLuceneSimilarityQuery(String smiles) {
    logger.debug("Get Lucene similairy query for smiles={}", smiles);
    return getLuceneQuery(smiles, DELIMITER_OR);
  }

  public LuceneQuery getLuceneSimilarityQuery(RWMol mol) {
    logger.debug("Get Lucene similairy query for mol");
    return getLuceneQuery(mol, DELIMITER_OR);
  }

  /**
   * Return encoded query object with string for lucene fulltext query and count of set bits
   *
   * @param smiles to convert for further LuceneQuery
   * @return ex.: { str="3 AND 5 AND 14 AND 256 AND 258", int=5 }
   */
  public LuceneQuery getLuceneSSSQuery(String smiles) {
    logger.debug("Get Lucene fp query for smiles={}", smiles);
    return getLuceneQuery(smiles, DELIMITER_AND);
  }

  /**
   * Return encoded query object with string for lucene fulltext query and count of set bits
   *
   * @param mol to user for further construction LuceneQuery
   * @return ex.: { str="3 AND 5 AND 14 AND 256 AND 258", int=5 }
   */
  public LuceneQuery getLuceneSSSQuery(RWMol mol) {
    logger.debug("Get Lucene fp query for mol");
    return getLuceneQuery(mol, DELIMITER_AND);
  }

  private LuceneQuery getLuceneQuery(RWMol mol, final String delimiter) {
    final BitSet fp = fingerprintFactory.createStructureFingerprint(mol);
    return getLuceneQuery(fp, delimiter);
  }

  private LuceneQuery getLuceneQuery(String smiles, final String delimiter) {
    final BitSet fp = fingerprintFactory.createStructureFingerprint(smiles);
    return getLuceneQuery(fp, delimiter);
  }

  private LuceneQuery getLuceneQuery(final BitSet fp, final String delimiter) {
    LuceneQuery luceneQuery = new LuceneQuery(fp, delimiter);
    logger.debug("Lucene fp luceneQuery={}", luceneQuery);
    return luceneQuery;
  }

  /**
   * Method fulfills the MolBlock with parameters from rwmol object Used to extend properties of the node
   *
   * @param rwmol object
   * @return MolBlock with fields
   */
  private MolBlock createMolBlock(final RWMol rwmol) {
    logger.debug("Construct default molBlock fields");
    final String rdkitSmiles = RDKFuncs.MolToSmiles(rwmol);
    final String formula = RDKFuncs.calcMolFormula(rwmol);
    final double molecularWeight = RDKFuncs.calcExactMW(rwmol);
    final String inchi = RDKFuncs.MolToInchiKey(rwmol);

    logger.debug("Construct structure fingerprint for lucene");
    LuceneQuery luceneQuery = getLuceneQuery(rwmol, DELIMITER_WHITESPACE);

    final long fingerprintOnes = luceneQuery.getPositiveBits();
    final String fingerprintEncoded = luceneQuery.getLuceneQuery();

    logger.debug("Constructed fp encoded={}", fingerprintEncoded);
    return new MolBlock(rdkitSmiles, formula, molecularWeight, inchi, fingerprintEncoded, fingerprintOnes);
  }
}
