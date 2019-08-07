package org.rdkit.neo4j.procedures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.neo4j.graphdb.DependencyResolver.SelectionStrategy.FIRST;

import lombok.val;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.internal.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.GraphUtils;
import org.rdkit.neo4j.models.NodeFields;

public class FingerprintProcedureTest extends BaseTest {

  @Override
  public void prepareTestDatabase() {
    graphDb = GraphUtils.getTestDatabase();
    Procedures proceduresService = ((GraphDatabaseAPI) graphDb).getDependencyResolver().resolveDependency(Procedures.class, FIRST);
    try {
      proceduresService.registerProcedure(SubstructureSearch.class, true);
      proceduresService.registerProcedure(FingerprintProcedures.class, true);
    } catch (KernelException e) {
      e.printStackTrace();
      logger.error("Not success :(");
    }
    graphDb.execute("CALL org.rdkit.search.createIndex($labels)", MapUtil.map("labels", defaultLabels));
  }

  @Test
  public void createCustomFpTest() throws Exception {
    insertChemblRows();

    final String propertyName = "torsion_fp";
    final String fptype = FingerprintType.torsion.toString(); // morgan fails
    graphDb.execute("CALL org.rdkit.fingerprint.create($labels, $propertyName, $fptype)", MapUtil.map(
       "labels", defaultLabels,
        "propertyName", propertyName,
        "fptype", fptype
    ));

    try (val tx = graphDb.beginTx()) {
      final String positiveBitsAmount = propertyName + "_ones";
      final String fpTypeProperty = propertyName + "_type";
      graphDb.findNodes(Label.label(defaultLabels.get(0))).stream().allMatch(node -> {
        assertTrue(node.hasProperty(propertyName));
        assertTrue(node.hasProperty(positiveBitsAmount));
        assertEquals(node.getProperty(fpTypeProperty), fptype);
        return true;
      });

      tx.success();
    }

    graphDb.execute("CALL org.rdkit.search.dropIndex()");
    graphDb.execute("CALL db.index.fulltext.drop($indexName)", MapUtil.map("indexName", propertyName));
  }

  @Test(expected = IllegalStateException.class)
  // Test creation on reserved property name
  public void createReservedPropertyTest() throws Throwable {
    final String propertyName = NodeFields.MdlMol.getValue();
    try {
      graphDb.execute("CALL org.rdkit.fingerprint.create($labels, $propertyName, $fptype)", MapUtil.map(
          "labels", defaultLabels,
          "propertyName", propertyName,
          "fptype", "morgan"
      ));
    } catch (QueryExecutionException e) {
      // todo: looks terrible
      throw e.getCause().getCause().getCause().getCause(); // get Kernel exception, cypher execution exception, get procedure exception, get procedure invoked exceptionведь
    }
  }

  @Test(expected = IllegalStateException.class)
  // Test creation on reserved property name
  public void createInvalidFpTest() throws Throwable {
    final String propertyName = "morgan_fp";
    try {
      graphDb.execute("CALL org.rdkit.fingerprint.create($labels, $propertyName, $fptype)", MapUtil.map(
          "labels", defaultLabels,
          "propertyName", propertyName,
          "fptype", "<invalid>"
      ));
    } catch (QueryExecutionException e) {
      // todo: looks terrible
      throw e.getCause().getCause().getCause().getCause(); // get Kernel exception, cypher execution exception, get procedure exception, get procedure invoked exceptionведь
    }
  }
}
