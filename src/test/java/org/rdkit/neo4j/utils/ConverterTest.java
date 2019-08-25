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

import static org.junit.Assert.*;
import static org.rdkit.neo4j.utils.Converter.DELIMITER_AND;
import static org.rdkit.neo4j.utils.Converter.DELIMITER_OR;
import static org.rdkit.neo4j.utils.Converter.DELIMITER_WHITESPACE;

import org.RDKit.RWMol;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.bin.LibraryLoader;
import org.rdkit.neo4j.models.LuceneQuery;
import org.rdkit.neo4j.models.NodeParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConverterTest {
  private static final Logger logger = LoggerFactory.getLogger(ConverterTest.class);
  private Converter converter = Converter.createDefault();


  @BeforeClass
  public static void initializeLibraries() throws Exception {
    LibraryLoader.loadLibraries();
  }

  @Test
  public void smilesTest() {
    final String smiles1 = "O=S(=O)(Cc1ccccc1)CS(=O)(=O)Cc1ccccc1";
    final String smiles2 = "O=S(=O)(CC1=CC=CC=C1)CS(=O)(=O)CC1=CC=CC=C1";

    NodeParameters block1 = converter.convertSmiles(smiles1);
    NodeParameters block2 = converter.convertSmiles(smiles2);

    String rdkitSmiles1 = block1.getCanonicalSmiles();
    String rdkitSmiles2 = block2.getCanonicalSmiles();

    logger.info("{} -> {}", smiles1, rdkitSmiles1);
    logger.info("{} -> {}", smiles2, rdkitSmiles2);

    assertEquals(rdkitSmiles1, rdkitSmiles2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void smilesFailureTest() {
    final String smiles = "nonvalid";
    NodeParameters block = converter.convertSmiles(smiles);
    String rdkitSmiles = block.getCanonicalSmiles();

    logger.error(rdkitSmiles);
  }

  @Test
  public void molBlockTest() {
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
    final String inchi = "DKXNMYFLQWZCGD-UHFFFAOYSA-N";
    final double molecularWeight = 324.049000992;

    NodeParameters block = converter.convertMolBlock(molBlock);
    logger.info("{}", block);

    assertEquals(smiles, block.getCanonicalSmiles());
    assertEquals(formula, block.getFormula());
    assertEquals(inchi, block.getInchi());
    assertEquals(molecularWeight, block.getMolecularWeight(), 1e-4);
  }

  @Test
  public void luceneSSSQueryTest() {
    final String expectedLuceneQuery = "1 AND 3 AND 11 AND 32 AND 39 AND 46 AND 54 AND 57 AND 64 AND 84 AND 87 AND 103 AND 106 AND 108 AND 114 AND 149 AND 173 AND 175 AND 178 AND 194 AND 202 AND 203 AND 207 AND 217 AND 224 AND 230 AND 234 AND 249 AND 252 AND 253 AND 257 AND 261 AND 270 AND 283 AND 295 AND 296 AND 337 AND 343 AND 348 AND 360 AND 389 AND 394 AND 410 AND 413 AND 417 AND 424 AND 428 AND 429 AND 434 AND 435 AND 445 AND 447 AND 458 AND 465 AND 466 AND 469 AND 474 AND 475 AND 479 AND 488 AND 490 AND 497 AND 512 AND 513 AND 517 AND 518 AND 523 AND 527 AND 532 AND 533 AND 550 AND 552 AND 565 AND 575 AND 584 AND 587 AND 595 AND 601 AND 616 AND 617 AND 618 AND 622 AND 630 AND 653 AND 661 AND 663 AND 664 AND 673 AND 675 AND 682 AND 683 AND 687 AND 691 AND 692 AND 695 AND 698 AND 699 AND 702 AND 705 AND 725 AND 730 AND 734 AND 737 AND 753 AND 772 AND 773 AND 778 AND 779 AND 789 AND 797 AND 798 AND 809 AND 812 AND 822 AND 833 AND 853 AND 864 AND 865 AND 869 AND 872 AND 875 AND 877 AND 898 AND 904 AND 912 AND 921 AND 923 AND 935 AND 943 AND 944 AND 945 AND 957 AND 963 AND 967 AND 972 AND 998 AND 1001 AND 1003 AND 1007 AND 1008 AND 1022 AND 1033 AND 1035 AND 1051 AND 1052 AND 1060 AND 1061 AND 1064 AND 1065 AND 1066 AND 1069 AND 1072 AND 1084 AND 1092 AND 1093 AND 1102 AND 1106 AND 1110 AND 1113 AND 1124 AND 1130 AND 1132 AND 1133 AND 1148 AND 1155 AND 1159 AND 1163 AND 1164 AND 1165 AND 1172 AND 1179 AND 1182 AND 1185 AND 1189 AND 1203 AND 1205 AND 1208 AND 1214 AND 1222 AND 1236 AND 1257 AND 1265 AND 1272 AND 1281 AND 1289 AND 1295 AND 1299 AND 1323 AND 1328 AND 1329 AND 1364 AND 1369 AND 1383 AND 1385 AND 1386 AND 1387 AND 1388 AND 1389 AND 1394 AND 1397 AND 1399 AND 1403 AND 1408 AND 1412 AND 1414 AND 1416 AND 1417 AND 1440 AND 1444 AND 1447 AND 1449 AND 1455 AND 1460 AND 1465 AND 1473 AND 1475 AND 1494 AND 1499 AND 1508 AND 1512 AND 1513 AND 1524 AND 1526 AND 1531 AND 1534 AND 1536 AND 1538 AND 1560 AND 1562 AND 1565 AND 1570 AND 1573 AND 1576 AND 1598 AND 1607 AND 1608 AND 1637 AND 1647 AND 1654 AND 1656 AND 1702 AND 1713 AND 1715 AND 1732 AND 1733 AND 1735 AND 1764 AND 1782 AND 1792 AND 1819 AND 1839 AND 1887 AND 1889 AND 1899 AND 1902 AND 1906 AND 1912 AND 1931 AND 1946 AND 1947 AND 1952 AND 1961 AND 1966 AND 1980 AND 1981 AND 1982 AND 1985 AND 2017 AND 2019";
    final int expectedPositive = 269;

    Converter converter = Converter.createConverter(FingerprintType.pattern);
    LuceneQuery luceneQuery = converter.getLuceneSSSQuery("O=S(=O)(Cc1ccccc1)CS(=O)(=O)Cc1ccccc1");

    assertEquals(expectedPositive, luceneQuery.getPositiveBits());
    assertEquals(expectedLuceneQuery, luceneQuery.getLuceneQuery());
    assertEquals(DELIMITER_AND, luceneQuery.getDelimiter());
  }

  @Test
  @Ignore
  public void luceneSimilarityQueryTest() {
    final String expectedLuceneQuery = "???";
    final int expectedPositive = -1;

    Converter converter = Converter.createConverter(FingerprintType.morgan);
    LuceneQuery luceneQuery = converter.getLuceneSSSQuery("COc1cc2c(cc1Br)C(C)CNCC2"); // todo: why fails?

    assertEquals(expectedPositive, luceneQuery.getPositiveBits());
    assertEquals(expectedLuceneQuery, luceneQuery.getLuceneQuery());
    assertEquals(DELIMITER_OR, luceneQuery.getDelimiter());
  }

  @Test
  // Proves different settings generate different items
  public void differentLuceneFPsTest() {
    final String smiles = "O=S(=O)(Cc1ccccc1)CS(=O)(=O)Cc1ccccc1";
    Converter converterTorsion = Converter.createConverter(FingerprintType.torsion);
    Converter converterPattern = Converter.createConverter(FingerprintType.pattern);
    LuceneQuery patternQuery = converterPattern.getLuceneFingerprint(smiles);
    LuceneQuery torsionQuery = converterTorsion.getLuceneFingerprint(smiles);

    assertEquals(DELIMITER_WHITESPACE, patternQuery.getDelimiter());
    assertEquals(DELIMITER_WHITESPACE, torsionQuery.getDelimiter());
    assertNotEquals(patternQuery.getLuceneQuery(), torsionQuery.getLuceneQuery());
    assertNotEquals(patternQuery.getPositiveBits(), torsionQuery.getPositiveBits());
  }

  @Test
  // Proves rwmol can be created from mdlmol, but failures to be created from its canonical_smiles
  public void failureSmilesTest() {
    final String mdlmol = "\n"
        + "  ACD/Labs05011920082D\n"
        + "\n"
        + " 70 82  0  0  0  0  0  0  0  0  2 V2000\n"
        + "   11.5937   -9.5440    0.0000 Cl  0  5  0  0  0  0  0  0  0  0  0  0\n"
        + "   10.6329  -10.5048    0.0000 Al  0  3  0  0  0  0  0  0  0  0  0  0\n"
        + "   12.0848   -3.5229    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.3305  -17.6147    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    3.4589   -9.1810    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   17.8282  -11.9567    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   10.6329   -8.9675    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   10.6329  -12.0207    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    8.3270  -12.6399    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   12.8748   -8.1562    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    8.4337   -8.1775    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   12.8748  -12.7040    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.1170  -10.5048    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   12.1702  -10.5048    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.5653  -13.1096    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   11.7218   -7.8786    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   11.7218  -13.1096    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.5653   -7.8786    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   11.3161   -6.9605    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.9924  -14.0277    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.9924   -6.9178    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   11.3802  -14.0277    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   13.2591  -11.5723    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    8.0280   -9.4159    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    8.0280  -11.5723    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   13.2591   -9.4159    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    6.8537   -9.8429    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   14.2839  -11.2094    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    6.8537  -11.1453    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   14.3053   -9.8215    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.2878  -15.2447    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   12.0207   -5.8289    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.3518   -5.7648    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   12.0848  -15.2447    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    5.7435   -9.2024    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   15.4796  -11.9353    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    5.7435  -11.7859    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   15.5437   -9.1383    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   11.3802   -4.6546    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.9924  -16.4404    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   11.3802  -16.4404    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   10.0564   -4.6332    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    4.6332   -9.8429    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   16.6966  -11.2734    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    4.6332  -11.1453    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   16.7393   -9.8642    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   11.4442   -2.3486    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.9924  -18.7677    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   19.0026  -11.3161    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    2.3059   -9.8429    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.3305  -19.9207    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   12.1275   -1.2170    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   11.3375  -18.7677    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   10.0991   -2.3059    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    2.3059  -11.1880    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   19.0453   -9.9924    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    1.1530   -9.1810    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   20.1555  -12.0207    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   11.5083   -0.0427    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.9924  -21.0736    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    9.4586   -1.1316    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   11.9994  -19.9207    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   10.1632    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   11.3375  -21.0736    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    1.1530  -11.8499    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   20.2196   -9.3518    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    0.0000   -9.8429    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   21.3085  -11.3802    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "    0.0000  -11.1880    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   21.3512  -10.0564    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "  2  7  1  0  0  0  0\n"
        + "  2  8  1  0  0  0  0\n"
        + "  3 39  1  0  0  0  0\n"
        + "  3 47  1  0  0  0  0\n"
        + "  4 40  1  0  0  0  0\n"
        + "  4 48  1  0  0  0  0\n"
        + "  5 43  1  0  0  0  0\n"
        + "  5 50  1  0  0  0  0\n"
        + "  6 44  1  0  0  0  0\n"
        + "  6 49  1  0  0  0  0\n"
        + "  7 16  1  0  0  0  0\n"
        + "  7 18  1  0  0  0  0\n"
        + "  8 15  1  0  0  0  0\n"
        + "  8 17  1  0  0  0  0\n"
        + "  9 15  1  0  0  0  0\n"
        + "  9 25  1  0  0  0  0\n"
        + " 10 16  1  0  0  0  0\n"
        + " 10 26  1  0  0  0  0\n"
        + " 11 18  1  0  0  0  0\n"
        + " 11 24  1  0  0  0  0\n"
        + " 12 17  1  0  0  0  0\n"
        + " 12 23  1  0  0  0  0\n"
        + " 13 24  1  0  0  0  0\n"
        + " 13 25  1  0  0  0  0\n"
        + " 14 23  1  0  0  0  0\n"
        + " 14 26  1  0  0  0  0\n"
        + " 15 20  2  0  0  0  0\n"
        + " 16 19  2  0  0  0  0\n"
        + " 17 22  2  0  0  0  0\n"
        + " 18 21  2  0  0  0  0\n"
        + " 19 21  1  0  0  0  0\n"
        + " 19 32  1  0  0  0  0\n"
        + " 20 22  1  0  0  0  0\n"
        + " 20 31  1  0  0  0  0\n"
        + " 21 33  1  0  0  0  0\n"
        + " 22 34  1  0  0  0  0\n"
        + " 23 28  2  0  0  0  0\n"
        + " 24 27  2  0  0  0  0\n"
        + " 25 29  2  0  0  0  0\n"
        + " 26 30  2  0  0  0  0\n"
        + " 27 29  1  0  0  0  0\n"
        + " 27 35  1  0  0  0  0\n"
        + " 28 30  1  0  0  0  0\n"
        + " 28 36  1  0  0  0  0\n"
        + " 29 37  1  0  0  0  0\n"
        + " 30 38  1  0  0  0  0\n"
        + " 31 40  2  0  0  0  0\n"
        + " 32 39  2  0  0  0  0\n"
        + " 33 42  2  0  0  0  0\n"
        + " 34 41  2  0  0  0  0\n"
        + " 35 43  2  0  0  0  0\n"
        + " 36 44  2  0  0  0  0\n"
        + " 37 45  2  0  0  0  0\n"
        + " 38 46  2  0  0  0  0\n"
        + " 39 42  1  0  0  0  0\n"
        + " 40 41  1  0  0  0  0\n"
        + " 43 45  1  0  0  0  0\n"
        + " 44 46  1  0  0  0  0\n"
        + " 47 52  2  0  0  0  0\n"
        + " 47 54  1  0  0  0  0\n"
        + " 48 51  2  0  0  0  0\n"
        + " 48 53  1  0  0  0  0\n"
        + " 49 56  2  0  0  0  0\n"
        + " 49 58  1  0  0  0  0\n"
        + " 50 55  2  0  0  0  0\n"
        + " 50 57  1  0  0  0  0\n"
        + " 51 60  1  0  0  0  0\n"
        + " 52 59  1  0  0  0  0\n"
        + " 53 62  2  0  0  0  0\n"
        + " 54 61  2  0  0  0  0\n"
        + " 55 65  1  0  0  0  0\n"
        + " 56 66  1  0  0  0  0\n"
        + " 57 67  2  0  0  0  0\n"
        + " 58 68  2  0  0  0  0\n"
        + " 59 63  2  0  0  0  0\n"
        + " 60 64  2  0  0  0  0\n"
        + " 61 63  1  0  0  0  0\n"
        + " 62 64  1  0  0  0  0\n"
        + " 65 69  2  0  0  0  0\n"
        + " 66 70  2  0  0  0  0\n"
        + " 67 69  1  0  0  0  0\n"
        + " 68 70  1  0  0  0  0\n"
        + "M  CHG  2   1  -1   2   1\n"
        + "M  END\n";

    NodeParameters block = converter.convertMolBlock(mdlmol);
    final String expectedSmiles = "[Cl-].c1ccc(Oc2ccc3c4[nH]c([nH]c5c6ccc(Oc7ccccc7)cc6c6[nH]c7[nH]c([nH]c8c9ccc(Oc%10ccccc%10)cc9c([nH]4)n8[al+]n56)c4cc(Oc5ccccc5)ccc74)c3c2)cc1";
    assertEquals("RWMol from mdlmol block creates canonical smiles", expectedSmiles, block.getCanonicalSmiles());

    RWMol mol = RWMol.MolFromSmarts(expectedSmiles, 0, false);
    assertNull("RWMol from smiles = null", mol);
  }
}
