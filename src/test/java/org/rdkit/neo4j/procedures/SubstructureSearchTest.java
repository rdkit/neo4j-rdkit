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
      proceduresService.registerFunction(SubstructureSearch.class, true);
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

  @Test
  public void callIsSubstructureTest() throws Throwable {
    final String querySmiles = "c1ccccc1";
    final String[] candidateSmiles = new String[]{
        "OB(O)c1ccccc1",
        "C=C(C)CC1CC(O)[C@]2(C)OC3CC4OC5C[C@]6(C)O[C@]7(C)CCC8OC9C[C@]%10(C)OC%11C(C)=CC(=O)OC%11CC%10OC9C[C@H](C)C8OC7CC6O[C@]5(C)CC=CC4OC3CC2O1"
    };
    final boolean[] substructMatches = new boolean[]{true, false};

    try (val tx = graphDb.beginTx()) {
      for (String smiles: candidateSmiles)
        graphDb.execute("CREATE (n:Chemical:Structure {smiles:$smiles})", MapUtil.map("smiles", smiles));
      tx.success();
    }

    for (int i = 0; i < candidateSmiles.length; i++) {
      val result = graphDb.execute(
          "MATCH (n:Chemical:Structure) WHERE n.smiles = $candidate_smiles RETURN n.smiles as smiles, org.rdkit.search.substructure.is(n, $query) as bool",
          MapUtil.map("query", querySmiles, "candidate_smiles", candidateSmiles[i]));

      val map = result.next();
      Assert.assertEquals(substructMatches[i], map.get("bool"));
      Assert.assertEquals(candidateSmiles[i], map.get("smiles"));
    }

    graphDb.execute("CALL org.rdkit.search.dropIndex()"); // otherwise we get an exception on shutdown
  }
}
