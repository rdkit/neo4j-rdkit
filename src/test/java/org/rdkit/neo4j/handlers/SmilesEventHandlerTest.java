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
    final String query = String.format("CREATE (c:Chemical:Structure {smiles: '%s'})", smiles);

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

    final String query = String.format("CREATE (c:Chemical:Structure {mdlmol: '%s'})", mol);
    final String canonicalSmiles = "COc1ccccc1";
    final String formula = "C7H8O";
    final String inchi = "RDOXTESZEPMUJZ-UHFFFAOYSA-N";
    final double molecularWeight = 108.057514876;

    try (val tx = graphDb.beginTx()) {
      graphDb.execute(query);
      tx.success();
    }

    try (val tx = graphDb.beginTx()) {
      Node node = graphDb.getNodeById(0);

      assertEquals(canonicalSmiles, node.getProperty("canonical_smiles"));
      assertEquals(formula, node.getProperty("formula"));
      assertEquals(inchi, node.getProperty("inchi"));
      assertEquals(molecularWeight, (Double) node.getProperty("molecular_weight"), 1e-3);
      tx.success();
    }
  }
}
