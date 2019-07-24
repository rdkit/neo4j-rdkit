package org.rdkit.neo4j.procedures;

import static org.neo4j.graphdb.DependencyResolver.SelectionStrategy.FIRST;

import lombok.val;
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
//
//      final String[] chembls = new String[]{"CHEMBL180815", "CHEMBL182184", "CHEMBL180867"};
//
//      for (int i = 0; i < chembls.length; i++) {
//        Node node = (Node) result.next().get("node");
//        String chembl = (String) node.getProperty("mol_id");
//        String smiles = (String) node.getProperty("smiles");
//        assertEquals(chembls[i], chembl);
//        assertEquals(expectedSmiles, smiles);
//      }

      tx.success();
    }

    graphDb.execute(String.format("CALL db.index.fulltext.drop('%s')", Constants.IndexName.getValue())); // otherwise we get an exception on shutdown
  }
}
