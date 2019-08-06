package org.rdkit.neo4j.procedures;

import static org.neo4j.graphdb.DependencyResolver.SelectionStrategy.FIRST;

import java.util.Map;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.internal.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.GraphUtils;

public class SubstructureSearchTest extends BaseTest {

  @Override
  public void prepareTestDatabase() {
    graphDb = GraphUtils.getTestDatabase();
    Procedures proceduresService = ((GraphDatabaseAPI) graphDb).getDependencyResolver().resolveDependency(Procedures.class, FIRST);
    try {
      proceduresService.registerProcedure(SubstructureSearch.class, true);
    } catch (KernelException e) {
      e.printStackTrace();
      logger.error("Not success :(");
    }
    graphDb.execute("CALL org.rdkit.search.createIndex($labels)", MapUtil.map("labels", defaultLabels));
  }

  @Test
  public void callSubstructureSearch() throws Exception {
    insertChemblRows();

    final String sssSmiles = "c1ccccc1";

    try (val tx = graphDb.beginTx()) {
      val result = graphDb.execute("CALL org.rdkit.search.substructure.smiles($labels, $smiles)", MapUtil.map(
          "labels", defaultLabels,
          "smiles", sssSmiles
      ));
      Map<String, Object> row = GraphUtils.getFirstRow(result);

      String smiles = (String) row.get("canonical_smiles");
      long score = (Long) row.get("score");
      Assert.assertEquals(63L, score);
      Assert.assertEquals("OB(O)c1ccccc1", smiles);

      tx.success();
    }

    graphDb.execute("CALL org.rdkit.search.dropIndex()");
  }

  @Test
  public void fingerprintMatchEqualTest() {
    final String smiles = "c1ccccc1";

    try (val tx = graphDb.beginTx()) {
      graphDb.execute("create (n:Structure:Chemical {smiles: $smiles}) return n", MapUtil.map("smiles", smiles));
      tx.success();
    }

    try (val tx = graphDb.beginTx()) {
      val result = graphDb.execute("CALL org.rdkit.search.substructure.smiles($labels, $smiles)", MapUtil.map(
          "labels", defaultLabels,
          "smiles", smiles
      ));
      Map<String, Object> columns = GraphUtils.getFirstRow(result);
      final String canonical = (String) columns.get("canonical_smiles");
      final long score = (Long) columns.get("score");

      Assert.assertEquals(0, score);
      Assert.assertEquals(canonical, smiles);

      tx.success();
    }

    graphDb.execute("CALL org.rdkit.search.dropIndex()"); // otherwise we get an exception on shutdown
  }

  @Test
  public void substructureSearchMolTest() {
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

    final String smiles = "COc1ccccc1";

    try (val tx = graphDb.beginTx()) {
      graphDb.execute("create (n:Structure:Chemical {smiles: $smiles}) return n", MapUtil.map("smiles", smiles));
      tx.success();
    }

    try (val tx = graphDb.beginTx()) {
      val result = graphDb.execute("CALL org.rdkit.search.substructure.mol($labels, $mol)", MapUtil.map(
          "labels", defaultLabels,
          "mol", mol
      ));
      Map<String, Object> columns = GraphUtils.getFirstRow(result);
      final String canonical = (String) columns.get("canonical_smiles");
      final long score = (Long) columns.get("score");

      Assert.assertEquals(0, score);
      Assert.assertEquals(canonical, smiles);

      tx.success();
    }
    graphDb.execute("CALL org.rdkit.search.dropIndex()"); // otherwise we get an exception on shutdown
  }

  @Test(expected = IllegalArgumentException.class)
  public void smilesNullRWMolTest() throws Throwable {
    final String smiles = "[H]C1O[C@@H](C(=O)[O-])[C@H](O)[C@@H](O)[C@H]10";

    try (val tx = graphDb.beginTx()) {
      graphDb.execute("CALL org.rdkit.search.substructure.smiles($labels, $smiles)", MapUtil.map(
          "labels", defaultLabels,
          "smiles", smiles
      ));
    } catch (QueryExecutionException e) {
      // todo: looks terrible
      throw e.getCause().getCause().getCause().getCause(); // get Kernel exception, cypher execution exception, get procedure exception, get procedure invoked exceptionведь
    }
  }
}
