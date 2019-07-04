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

}