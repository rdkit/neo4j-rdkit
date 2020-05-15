package org.rdkit.neo4j.handlers;

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

import static org.junit.Assert.assertEquals;

import lombok.val;
import org.junit.BeforeClass;
import org.junit.Test;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.helpers.collection.Iterators;
import org.rdkit.neo4j.bin.LibraryLoader;
import org.rdkit.neo4j.index.utils.BaseTest;

import java.util.Collections;

public class SmilesEventHandlerTest extends BaseTest {

  @BeforeClass
  public static void loadLibraries() throws Exception {
    LibraryLoader.loadLibraries();
  }

  @Test
  public void emptySmilesTest() {
    final String query = "CREATE (c:Chemical:Structure {smiles: ''})";
    logger.info("{}", query);
    try (val tx = graphDb.beginTx()) {
      graphDb.execute(query);
      tx.success();
    } catch (TransactionFailureException e) {
      assertEquals(e.getCause().getCause().getClass(), IllegalArgumentException.class);
      logger.info("Successfully detected IllegalArgumentException.class");
    }
  }

  @Test
  public void invalidNodeTest() {
    final String query = "CREATE (c:Chemical:Structure {mol_id: '1244_65'})";
    logger.info("{}", query);

    try (val tx = graphDb.beginTx()) {
      graphDb.execute(query);
      tx.success();
    } catch (TransactionFailureException e) {
      assertEquals(e.getCause().getCause().getClass(), NotFoundException.class);
      logger.info("Successfully detected NotFoundException.class");
    }
  }

  @Test
  public void insertCanonicalSmilesTest() {
    final String smiles = "C(F)(F)F";
    final String canonicalSmiles = "FC(F)F";
    final String query = "CREATE (c:Chemical:Structure {smiles: $smiles})";

    logger.info("{}, expected canonical={}", query, canonicalSmiles);

    graphDb.execute(query, Collections.singletonMap("smiles", smiles));

    try (val tx = graphDb.beginTx()) {
      Node node = graphDb.getNodeById(0);
      assertEquals(node.getProperty("smiles"), smiles);
      assertEquals(node.getProperty("canonical_smiles"), canonicalSmiles);
      tx.success();
    }
  }

  @Test
  public void insertMolBlockTest() {
    final String mol = "\n"
        + "  Mrv1810 07051914202D          \n"
        + "\n"
        + "  8  8  0  0  0  0            999 V2000\n"
        + "   -4.4436   -2.5359    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   -5.1581   -2.9484    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   -5.1581   -3.7734    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   -4.4436   -4.1859    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   -3.7291   -3.7734    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   -3.7291   -2.9484    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   -3.0147   -2.5359    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "   -3.0147   -1.7109    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
        + "  1  2  1  0  0  0  0\n"
        + "  2  3  2  0  0  0  0\n"
        + "  3  4  1  0  0  0  0\n"
        + "  4  5  2  0  0  0  0\n"
        + "  5  6  1  0  0  0  0\n"
        + "  1  6  2  0  0  0  0\n"
        + "  6  7  1  0  0  0  0\n"
        + "  7  8  1  0  0  0  0\n"
        + "M  END\n";

    final String query = String.format("CREATE (c:Chemical:Structure {mdlmol: '%s'}) RETURN id(c) as id", mol);
//    final String canonicalSmiles = "COc1ccccc1";
    final String canonicalSmiles = "COC1=CC=CC=C1";
    final String formula = "C7H8O";
    final String inchi = "RDOXTESZEPMUJZ-UHFFFAOYSA-N";
    final double molecularWeight = 108.057514876;

    long id = (long) Iterators.single(graphDb.execute(query)).get("id");

    try (val tx = graphDb.beginTx()) {
      Node node = graphDb.getNodeById(id);

      assertEquals(canonicalSmiles, node.getProperty("canonical_smiles"));
      assertEquals(formula, node.getProperty("formula"));
      assertEquals(inchi, node.getProperty("inchi"));
      assertEquals(molecularWeight, (Double) node.getProperty("molecular_weight"), 1e-3);
      tx.success();
    }
  }

  @Test
  public void testInvalidSmiles() {
    graphDb.execute("CREATE (n:Entity:Chemical:Compound:Structure { luri: 'test3', tag:'<test3>', preferred_name: 'aabbcc3', smiles: 'Cl[C](C)(C)(C)Br'})");
  }

  @Test
  public void testInvalidMdMol() {
    String invalidMolBlock = "\n" +
            "     RDKit          2D\n\n" +
            "  6  5  0  0  0  0  0  0  0  0999 V2000\n" +
            "    2.5981    1.5000    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    1.2990    0.7500    0.0000 C   0  0  0  0  0  5  0  0  0  0  0  0\n" +
            "    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    2.5981   -0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    1.2990    2.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    0.0000    1.5000    0.0000 Br  0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "  1  2  1  0\n" +
            "  2  3  1  0\n" +
            "  2  4  1  0\n" +
            "  2  5  1  0\n" +
            "  2  6  1  0\n" +
            "M  END\n";

    graphDb.execute(
            " CREATE (n:Entity:Chemical:Compound:Structure { luri: 'test5', tag:'<test5>', preferred_name: 'aabbcc5', mdlmol: $molBlock})",
            Collections.singletonMap("molBlock", invalidMolBlock));
  }
}
