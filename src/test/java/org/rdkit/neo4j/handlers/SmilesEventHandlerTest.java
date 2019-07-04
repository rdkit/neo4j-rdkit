package org.rdkit.neo4j.handlers;

import static org.junit.Assert.assertEquals;

import lombok.val;
import org.junit.BeforeClass;
import org.junit.Test;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.TransactionFailureException;
import org.rdkit.neo4j.bin.LibraryLoader;
import org.rdkit.neo4j.index.utils.BaseTest;

public class SmilesEventHandlerTest extends BaseTest {

  @BeforeClass
  public static void loadLibraries() throws Exception {
    LibraryLoader.loadLibraries();
  }

  @Test
  public void emptySmilesTest() {
    // todo: I must load jni libs somewhere
    // todo: should it be placed in event handler?
    final String query = "CREATE (c:Chemical {smiles: ''})";
    logger.info("{}", query);
    try (val tx = graphDb.beginTx()) {
      graphDb.execute(query);
      tx.success();
    } catch (TransactionFailureException e) {
      assertEquals(e.getCause().getCause().getClass(), IllegalArgumentException.class);
      logger.info("Successfully detected IllegalArgumentException.class");
    }

    logger.error("After exception, should not be visible");
  }

  @Test
  public void invalidNodeTest() {
    final String query = "CREATE (c:Chemical {mol_id: '1244_65'})";
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
    final String query = String.format("CREATE (c:Chemical {smiles: '%s'})", smiles);

    logger.info("{}, expected canonical={}", query, canonicalSmiles);

    try (val tx = graphDb.beginTx()) {
      graphDb.execute(query);
      tx.success();
    }

    try (val tx = graphDb.beginTx()) {
      Node node = graphDb.getNodeById(0);
      assertEquals(node.getProperty("smiles"), smiles);
      assertEquals(node.getProperty("canonical_smiles"), canonicalSmiles);
      tx.success();
    }
  }
}
