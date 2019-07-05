package org.rdkit.neo4j.utils;

import org.RDKit.RDKFuncs;
import org.RDKit.RWMol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Converter {
  private static final Logger logger = LoggerFactory.getLogger(Converter.class);

  public static String getRDKitSmiles(final String smiles) {
    try (RWMolCloseable rwmol = RWMolCloseable.from(RWMol.MolFromSmiles(smiles))) {
      final String rdkitSmiles = RDKFuncs.MolToSmiles(rwmol);

      if (rdkitSmiles.isEmpty()) {
        logger.error("Empty canonical smiles obtained from smiles=`{}`", smiles);
        throw new IllegalArgumentException("Empty canonical smiles obtained");
      }

      return rdkitSmiles;
    }
  }

  public static MolBlock getRDKitMolBlock(final String molBlock) {
    // todo: may be add more constructors and differentiate log errors?
    try (RWMolCloseable rwmol = RWMolCloseable.from(RWMol.MolFromMolBlock(molBlock))) {
      final String rdkitSmiles = RDKFuncs.MolToSmiles(rwmol);
      final String formula = RDKFuncs.calcMolFormula(rwmol);
      final double molecularWeight =  RDKFuncs.calcExactMW(rwmol);
      final String inchi = RDKFuncs.MolToInchiKey(rwmol);

      return new MolBlock(molBlock, rdkitSmiles, formula, molecularWeight, inchi);
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
}
