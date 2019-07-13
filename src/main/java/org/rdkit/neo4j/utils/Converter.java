package org.rdkit.neo4j.utils;

import org.RDKit.RDKFuncs;
import org.RDKit.RWMol;
import org.rdkit.neo4j.models.MolBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Converter {
  private static final Logger logger = LoggerFactory.getLogger(Converter.class);

  public static MolBlock convertSmiles(final String smiles) {
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

  public static MolBlock convertMolBlock(final String molBlock) {
    try (RWMolCloseable rwmol = RWMolCloseable.from(RWMol.MolFromMolBlock(molBlock))) {
      MolBlock block = createMolBlock(rwmol);
      block.setMolBlock(molBlock);

      return block;
//      Attributes:
  //      luri - unique uuid
  //      tag - for display only
  //      preferred_name - molecule name as in original toxcast database
  //      molecular_formula
  //      mdlmol - MOL file as in toxcast
  //      smiles - canonical smiles (produced in Knime with RDKit)
  //      molecular_weight_value
    }
  }

  public static String getRDKitSmiles(String smiles) {
    try (RWMolCloseable rwmol = RWMolCloseable.from(RWMol.MolFromSmiles(smiles))) {
      return RDKFuncs.MolToSmiles(rwmol);
    }
  }

  private static MolBlock createMolBlock(final RWMol rwmol) {
    final String rdkitSmiles = RDKFuncs.MolToSmiles(rwmol);
    final String formula = RDKFuncs.calcMolFormula(rwmol);
    final double molecularWeight =  RDKFuncs.calcExactMW(rwmol);
    final String inchi = RDKFuncs.MolToInchiKey(rwmol);

    return new MolBlock(rdkitSmiles, formula, molecularWeight, inchi);
  }
}
