package org.rdkit.neo4j.procedures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.neo4j.graphdb.DependencyResolver.SelectionStrategy.FIRST;

import lombok.val;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.internal.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.GraphUtils;

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
  public void createCustomFp() throws Exception {
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
}
