package org.rdkit.neo4j.utils;

import java.util.BitSet;
import java.util.StringJoiner;
import org.RDKit.RDKFuncs;
import org.RDKit.RWMol;
import org.rdkit.fingerprint.FingerprintFactory;
import org.rdkit.neo4j.models.MolBlock;
import org.rdkit.neo4j.models.SSSQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Converter {

  private static final Logger logger = LoggerFactory.getLogger(Converter.class);

  private final String DELIMITER_WHITESPACE = " ";
  private final String DELIMITER_AND = " AND ";

  private FingerprintFactory fingerprintFactory;

  public Converter(FingerprintFactory fingerprintFactory) {
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
    StringJoiner joiner = bitsetToJoiner(fp, DELIMITER_AND);
    final int positiveBits = joiner.length();
    final String query = joiner.toString();

    logger.debug("Lucene fp positiveBits={}, query={}", query, positiveBits);
    return new SSSQuery(query, positiveBits);
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
    StringJoiner joiner = bitsetToJoiner(fp, DELIMITER_WHITESPACE);

    final int fingerprintOnes = joiner.length(); // todo: does it work?
    final String fingerprintEncoded = joiner.toString();

    logger.debug("Constructed fp encoded={}", fingerprintEncoded);
    return new MolBlock(rdkitSmiles, formula, molecularWeight, inchi, fingerprintEncoded, fingerprintOnes);
  }

  /**
   * Convert a bitset into a string with specified `delimiter`
   *
   * @param fingerprint to convert
   * @param delimiter to join
   * @return encoded string joiner
   */
  private StringJoiner bitsetToJoiner(final BitSet fingerprint, final String delimiter) {
    StringJoiner joiner = new StringJoiner(delimiter);

    // if i == -1, the FP has ended
    for (int i = fingerprint.nextSetBit(0); i >= 0; i = fingerprint.nextSetBit(i + 1)) {
      joiner.add(Integer.toString(i));
    }

    return joiner;
  }
}
