package org.rdkit.neo4j.utils;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rdkit.neo4j.bin.LibraryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConverterTest {
  private static final Logger logger = LoggerFactory.getLogger(ConverterTest.class);

  @BeforeClass
  public static void initializeLibraries() throws Exception {
    LibraryLoader.loadLibraries();
  }

  @Test
  public void smilesTest() {
    final String smiles1 = "O=S(=O)(Cc1ccccc1)CS(=O)(=O)Cc1ccccc1";
    final String smiles2 = "O=S(=O)(CC1=CC=CC=C1)CS(=O)(=O)CC1=CC=CC=C1";

    String rdkitSmiles1 = Converter.getRDKitSmiles(smiles1);
    String rdkitSmiles2 = Converter.getRDKitSmiles(smiles2);

    logger.info("{} -> {}", smiles1, rdkitSmiles1);
    logger.info("{} -> {}", smiles2, rdkitSmiles2);

    assertEquals(rdkitSmiles1, rdkitSmiles2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void smilesFailureTest() {
    final String smiles = "nonvalid";
    String rdkitSmiles = Converter.getRDKitSmiles(smiles);

    logger.error(rdkitSmiles);
  }

  @Test
  public void molBlockTest() {
//    final String molBlock = "\n"
//            + "Absolutely any text\n"
//            + "  8  8  0  0  0  0            999 V2000\n"
//            + "   -4.4436   -2.5359    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
//            + "   -5.1581   -2.9484    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
//            + "   -5.1581   -3.7734    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
//            + "   -4.4436   -4.1859    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
//            + "   -3.7291   -3.7734    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
//            + "   -3.7291   -2.9484    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
//            + "   -3.0147   -2.5359    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
//            + "   -3.0147   -1.7109    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
//            + "  1  2  1  0  0  0  0\n"
//            + "  2  3  2  0  0  0  0\n"
//            + "  3  4  1  0  0  0  0\n"
//            + "  4  5  2  0  0  0  0\n"
//            + "  5  6  1  0  0  0  0\n"
//            + "  1  6  2  0  0  0  0\n"
//            + "  6  7  1  0  0  0  0\n"
//            + "  7  8  1  0  0  0  0\n"
//            + "M  END";

    final String molBlock = "\n"
            + "Actelion Java MolfileCreator 1.0\n"
            + "\n"
            + " 21 22  0  0  0  0  0  0  0  0999 V2000\n"
            + "    4.4462   -2.0490   -0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    5.1962   -0.7500   -0.0000 S   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    5.9462   -2.0490   -0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    6.4952   -0.0000   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    7.7943   -0.7500   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    7.7943   -2.2500   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    9.0933   -3.0000   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "   10.3923   -2.2500   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "   10.3923   -0.7500   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    9.0933   -0.0000   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    3.8971   -0.0000   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    2.5981   -0.7500   -0.0000 S   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    1.0981   -0.7500   -0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    1.8481    0.5490   -0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    2.5981   -2.2500   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    1.2990   -3.0000   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    1.2990   -4.5000   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    0.0000   -5.2500   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "   -1.2990   -4.5000   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "   -1.2990   -3.0000   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    0.0000   -2.2500   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "  2  1  2  0  0  0  0\n"
            + "  3  2  2  0  0  0  0\n"
            + "  4  2  1  0  0  0  0\n"
            + "  5  4  1  0  0  0  0\n"
            + "  6  5  2  0  0  0  0\n"
            + "  7  6  1  0  0  0  0\n"
            + "  8  7  2  0  0  0  0\n"
            + "  9  8  1  0  0  0  0\n"
            + " 10  9  2  0  0  0  0\n"
            + " 10  5  1  0  0  0  0\n"
            + " 11  2  1  0  0  0  0\n"
            + " 12 11  1  0  0  0  0\n"
            + " 13 12  2  0  0  0  0\n"
            + " 14 12  2  0  0  0  0\n"
            + " 15 12  1  0  0  0  0\n"
            + " 16 15  1  0  0  0  0\n"
            + " 17 16  2  0  0  0  0\n"
            + " 18 17  1  0  0  0  0\n"
            + " 19 18  2  0  0  0  0\n"
            + " 20 19  1  0  0  0  0\n"
            + " 21 20  2  0  0  0  0\n"
            + " 21 16  1  0  0  0  0\n"
            + "M  END\n";

    final String smiles = "O=S(=O)(Cc1ccccc1)CS(=O)(=O)Cc1ccccc1";
    final String formula = "C15H16O4S2";

    MolBlock block = Converter.getRDKitMolBlock(molBlock);
    assertEquals(smiles, block.getRdkitSmiles());
    assertEquals(formula, block.getFormula());
  }
}