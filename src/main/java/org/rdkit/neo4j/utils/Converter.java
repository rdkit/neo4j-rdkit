package org.rdkit.neo4j.utils;

import java.util.BitSet;
import org.RDKit.RDKFuncs;
import org.RDKit.RWMol;
import org.rdkit.fingerprint.DefaultFingerprintFactory;
import org.rdkit.fingerprint.DefaultFingerprintSettings;
import org.rdkit.fingerprint.FingerprintFactory;
import org.rdkit.fingerprint.FingerprintSettings;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.models.MolBlock;
import org.rdkit.neo4j.models.SSSQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Converter {

  public static Converter createDefault() {
    FingerprintType type = FingerprintType.pattern;
    FingerprintSettings settings = new DefaultFingerprintSettings(type)
        .setNumBits(2048);
    FingerprintFactory factory = new DefaultFingerprintFactory(settings);
    return new Converter(factory);
  }

  private static final Logger logger = LoggerFactory.getLogger(Converter.class);

  private final String DELIMITER_WHITESPACE = " ";
  private final String DELIMITER_AND = " AND ";

  private FingerprintFactory fingerprintFactory;

  private Converter(FingerprintFactory fingerprintFactory) {
    this.fingerprintFactory = fingerprintFactory;
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

  /**
   * Return encoded query object with string for lucene fulltext query and count of set bits
   *
   * @param smiles to convert for further SSSQuery
   * @return ex.: { str="3 AND 5 AND 14 AND 256 AND 258", int=5 }
   */
  public SSSQuery getLuceneFPQuery(String smiles) {
    logger.info("Get Lucene fp query for smiles={}", smiles);

    final BitSet fp = fingerprintFactory.createStructureFingerprint(smiles);
    SSSQuery sssQuery = new SSSQuery(fp, DELIMITER_AND);

    logger.debug("Lucene fp sssQuery={}", sssQuery);
    return sssQuery;
  }

  /**
   * Return encoded query object with string for lucene fulltext query and count of set bits
   *
   * @param mol to user for further construction SSSQuery
   * @return ex.: { str="3 AND 5 AND 14 AND 256 AND 258", int=5 }
   */
  public SSSQuery getLuceneFPQuery(RWMol mol) {
    logger.info("Get Lucene fp query for mol");

    final BitSet fp = fingerprintFactory.createStructureFingerprint(mol);
    SSSQuery sssQuery = new SSSQuery(fp, DELIMITER_AND);

    logger.debug("Lucene fp sssQuery={}", sssQuery);
    return sssQuery;
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
    BitSet fp = fingerprintFactory.createStructureFingerprint(rwmol);
    SSSQuery sssQuery = new SSSQuery(fp, DELIMITER_WHITESPACE);

    final long fingerprintOnes = sssQuery.getPositiveBits();
    final String fingerprintEncoded = sssQuery.getLuceneQuery();

    logger.debug("Constructed fp encoded={}", fingerprintEncoded);
    return new MolBlock(rdkitSmiles, formula, molecularWeight, inchi, fingerprintEncoded, fingerprintOnes);
  }
}
