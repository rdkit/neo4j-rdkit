package org.rdkit.neo4j.procedures;

import static org.neo4j.graphdb.DependencyResolver.SelectionStrategy.FIRST;

import java.util.Map;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;
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
  }

  @Test
  public void callSubstructureSearch() throws Exception {
    graphDb.execute("CALL org.rdkit.search.createIndex($labels)", MapUtil.map("labels", defaultLabels));

    insertChemblRows();

    final String sssSmiles = "c1ccccc1";
    final String query = String.format("CALL org.rdkit.search.substructure.smiles([\"Chemical\", \"Structure\"], \"%s\")", sssSmiles);
    try (val tx = graphDb.beginTx()) {
      val result = graphDb.execute(query);
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
      graphDb.execute(String.format("create (n:Structure:Chemical {smiles: '%s'}) return n", smiles));
      tx.success();
    }

    graphDb.execute("CALL org.rdkit.search.createIndex($labels)", MapUtil.map("labels", defaultLabels));
    try (val tx = graphDb.beginTx()) {
      val result = graphDb.execute(String.format("CALL org.rdkit.search.substructure.smiles([\"Chemical\", \"Structure\"], \"%s\")", smiles));
      Map<String, Object> columns = GraphUtils.getFirstRow(result);
      final String canonical = (String) columns.get("canonical_smiles");
      final long score = (Long) columns.get("score");

      Assert.assertEquals(0, score);
      Assert.assertEquals(canonical, smiles);

      tx.success();
    }

    graphDb.execute("CALL org.rdkit.search.dropIndex()"); // otherwise we get an exception on shutdown
  }
}
