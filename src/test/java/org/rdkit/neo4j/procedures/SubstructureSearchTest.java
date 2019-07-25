package org.rdkit.neo4j.procedures;

import static org.neo4j.graphdb.DependencyResolver.SelectionStrategy.FIRST;

import java.util.Map;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;
import org.neo4j.internal.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.GraphUtils;
import org.rdkit.neo4j.models.Constants;

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
    graphDb.execute("CALL org.rdkit.search.substructure.createIndex([\"Chemical\", \"Structure\"])");

    insertChemblRows();

    final String sssSmiles = "c1ccccc1";
    final String query = String.format("CALL org.rdkit.search.substructure.smiles([\"Chemical\", \"Structure\"], \"%s\")", sssSmiles);
    try (val tx = graphDb.beginTx()) {
      val result = graphDb.execute(query);
      logger.info(result.resultAsString());
      tx.success();
    }

    graphDb.execute(String.format("CALL db.index.fulltext.drop('%s')", Constants.IndexName.getValue())); // otherwise we get an exception on shutdown
  }

  @Test
  public void callSubstructureOnEqual() {
    final String smiles = "c1ccccc1";

    try (val tx = graphDb.beginTx()) {
      graphDb.execute(String.format("create (n:Structure:Chemical {smiles: '%s'}) return n", smiles));
      tx.success();
    }

    graphDb.execute("CALL org.rdkit.search.substructure.createIndex([\"Chemical\", \"Structure\"])");
    try (val tx = graphDb.beginTx()) {
      val result = graphDb.execute(String.format("CALL org.rdkit.search.substructure.smiles([\"Chemical\", \"Structure\"], \"%s\")", smiles));
      Map<String, Object> columns = GraphUtils.getFirstRow(result);
      final String canonical = (String) columns.get("canonical_smiles");
      final long score = (Long) columns.get("score");

      Assert.assertEquals(0, score);
      Assert.assertEquals(canonical, smiles);

      tx.success();
    }
    
    graphDb.execute(String.format("CALL db.index.fulltext.drop('%s')", Constants.IndexName.getValue())); // otherwise we get an exception on shutdown
  }
}
