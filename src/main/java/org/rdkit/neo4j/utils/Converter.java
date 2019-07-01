package org.rdkit.neo4j.utils;

import org.RDKit.RDKFuncs;
import org.RDKit.RWMol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Converter {
  private static final Logger logger = LoggerFactory.getLogger(Converter.class);

  // todo: create Closeable wrapper around RWMol (and other RDKit objects)

  public static String getRDKitSmiles(final String smiles) {
    // todo: And a `null` return from `MolFromSmiles()` means either an invalid SMILES or an invalid molecule
    // todo: always delete rdkit objects
    RWMol rwmol = RWMol.MolFromSmiles(smiles);
    if (rwmol == null) {
      logger.error("Unable to obtain RWMol from smiles={}", smiles);
      throw new IllegalArgumentException("Unable to obtain RWMol from smiles");
    }

    final String rdkitSmiles = RDKFuncs.MolToSmiles(rwmol);
    rwmol.delete();

    return rdkitSmiles;
  }
}
