package org.rdkit.neo4j.procedures;

/*-
 * #%L
 * RDKit-Neo4j plugin
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
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.internal.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.GraphUtils;

public class UtilProceduresTest extends BaseTest {

  @Override
  public void prepareTestDatabase() {
    graphDb = GraphUtils.getTestDatabase();
    Procedures proceduresService = ((GraphDatabaseAPI) graphDb).getDependencyResolver().resolveDependency(Procedures.class, FIRST);

    try {
      proceduresService.registerProcedure(ExactSearch.class, true);
      proceduresService.registerFunction(UtilProcedures.class, true);
    } catch (KernelException e) {
      e.printStackTrace();
      logger.error("Not success :(");
    }
  }

  @Test
  public void functionCreateSvgTest() {
    final String smiles = "O=S(=O)(Cc1ccccc1)CS(=O)(=O)Cc1ccccc1";

    try (val tx = graphDb.beginTx()) {
      graphDb.execute("CREATE (n:Chemical {smiles: $smiles})", MapUtil.map("smiles", smiles));
      tx.success();
    }

    val result = graphDb.execute("MATCH (n:Chemical) return org.rdkit.utils.svg(n.smiles) as svg");
    Map<String, Object> map = result.next();
    final String svg = (String) map.get("svg");

    Assert.assertTrue(svg.contains("<svg"));
    Assert.assertTrue(svg.contains("</svg>"));
  }

  @Test
  public void searchCreateSvgTest() throws Throwable {
    insertChemblRows();
    final String smiles = "CCCC(C(=O)Nc1ccc(S(N)(=O)=O)cc1)C(C)(C)C";

    val result = graphDb.execute("CALL org.rdkit.search.exact.smiles($labels, $smiles) "
            + "YIELD canonical_smiles "
            + "RETURN org.rdkit.utils.svg(canonical_smiles) as svg",
        MapUtil.map("labels", defaultLabels, "smiles", smiles));
    Map<String, Object> map = result.next();
    final String svg = (String) map.get("svg");

    Assert.assertTrue(svg.contains("<svg"));
    Assert.assertTrue(svg.contains("</svg>"));
  }
}
