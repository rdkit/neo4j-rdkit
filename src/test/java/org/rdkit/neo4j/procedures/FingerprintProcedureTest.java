package org.rdkit.neo4j.procedures;

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

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.collection.MapUtil;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.TestUtils;
import org.rdkit.neo4j.models.NodeFields;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FingerprintProcedureTest extends BaseTest {

  @Before
  public void registerProcedures() {
    TestUtils.registerProcedures(graphDb, SubstructureSearch.class, FingerprintProcedures.class);
    graphDb.executeTransactionally("CALL org.rdkit.search.createIndex($labels)", MapUtil.map("labels", defaultLabels));
  }

  @Test
  public void createCustomFpTest() throws Exception {
    insertChemblRows();

    final String propertyName = "torsion_fp";
    final String fptype = FingerprintType.torsion.toString(); // morgan fails
    graphDb.executeTransactionally("CALL org.rdkit.fingerprint.create($labels, $fptype, $propertyName)", MapUtil.map(
       "labels", defaultLabels,
        "propertyName", propertyName,
        "fptype", fptype
    ));

    try (Transaction tx = graphDb.beginTx()) {
      final String positiveBitsAmount = propertyName + "_ones";
      final String fpTypeProperty = propertyName + "_type";
      tx.findNodes(Label.label(defaultLabels.get(0))).stream().allMatch(node -> {
        assertTrue(node.hasProperty(propertyName));
        assertTrue(node.hasProperty(positiveBitsAmount));
        assertEquals(node.getProperty(fpTypeProperty), fptype);
        return true;
      });

      tx.commit();
    }

    graphDb.executeTransactionally("CALL org.rdkit.search.dropIndex()");
    graphDb.executeTransactionally(String.format("DROP INDEX %s", propertyName + "_index"));
  }

  @Test(expected = IllegalStateException.class)
  // Test creation on reserved property name
  public void createReservedPropertyTest() throws Throwable {
    final String propertyName = NodeFields.MdlMol.getValue();
    try {
      graphDb.executeTransactionally("CALL org.rdkit.fingerprint.create($labels, $propertyName, $fptype)", MapUtil.map(
          "labels", defaultLabels,
          "propertyName", propertyName,
          "fptype", "morgan"
      ));
      fail();
    } catch (QueryExecutionException e) {
      // todo: looks terrible
      throw e.getCause().getCause().getCause().getCause(); // get Kernel exception, cypher execution exception, get procedure exception, get procedure invoked exception
    }
  }

  @Test(expected = IllegalStateException.class)
  // Test creation on reserved property name
  public void createInvalidFpTest() throws Throwable {
    final String propertyName = "morgan_fp";
    try {
      graphDb.executeTransactionally("CALL org.rdkit.fingerprint.create($labels, $fptype, $propertyName)", MapUtil.map(
          "labels", defaultLabels,
          "propertyName", propertyName,
          "fptype", "<invalid>"
      ));
      fail();
    } catch (QueryExecutionException e) {
      // todo: looks terrible
      throw e.getCause().getCause().getCause().getCause(); // get Kernel exception, cypher execution exception, get procedure exception, get procedure invoked exception
    }
  }

  @Test
  public void callSimilarityProcedureTest() throws Throwable {
    insertChemblRows();

    final String initialSmiles = "COc1ccc(C(=O)O)cc1";

    final String propertyName = "torsion_fp";
    final String fptype = FingerprintType.torsion.toString(); // morgan fails
    graphDb.executeTransactionally("CALL org.rdkit.fingerprint.create($labels, $fptype, $propertyName)", MapUtil.map(
        "labels", defaultLabels,
        "propertyName", propertyName,
        "fptype", fptype
    ));

    final int items = 2;
    final double[] similarities = new double[]{1.0d, 0.764d};

    graphDb.executeTransactionally("CALL org.rdkit.fingerprint.similarity.smiles($labels, $smiles, $fptype, $propertyName, $threshold)", MapUtil.map(
            "labels", defaultLabels,
            "smiles", initialSmiles,
            "fptype", fptype,
            "propertyName", propertyName,
            "threshold", 0.7d
    ), result -> {
      for (int i = 0; i < items; i++) {
        Map<String, Object> map = result.next();
        double similarity = (Double) map.get("similarity");
        assertEquals(similarities[i], similarity, 1e-2);
      }
      return null;
    });

    graphDb.executeTransactionally("CALL org.rdkit.search.dropIndex()");
    graphDb.executeTransactionally(String.format("DROP INDEX %s", propertyName + "_index"));
  }
}
