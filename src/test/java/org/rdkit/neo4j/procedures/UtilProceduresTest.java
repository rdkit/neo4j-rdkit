package org.rdkit.neo4j.procedures;

/*-
 * #%L
 * RDKit-Neo4j plugin
 * %%
 * Copyright (C) 2019 - 2020 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.internal.helpers.collection.Iterators;
import org.neo4j.internal.helpers.collection.MapUtil;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.TestUtils;

import java.util.Collections;
import java.util.Map;

public class UtilProceduresTest extends BaseTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void registerProcedures() {
    TestUtils.registerProcedures(graphDb, ExactSearch.class, UtilProcedures.class);
  }

  @Test
  public void functionCreateSvgTest() {
    final String smiles = "O=S(=O)(Cc1ccccc1)CS(=O)(=O)Cc1ccccc1";

    Map<String, Object> result = graphDb.executeTransactionally("RETURN org.rdkit.utils.svg($smiles) AS svg", Collections.singletonMap("smiles", smiles), Iterators::single);
    final String svg = (String) result.get("svg");

    Assert.assertTrue(svg.contains("<svg"));
    Assert.assertTrue(svg.contains("</svg>"));
  }

  @Test
  public void invalidSmilesSvgTest() {
    thrown.expect(QueryExecutionException.class);
    thrown.expectMessage("MolSanitizeException");
    final String smiles = "Cl[C](C)(C)(C)Br";

    graphDb.executeTransactionally("return org.rdkit.utils.svg($smiles) as svg", Collections.singletonMap("smiles", smiles), Iterators::single);
  }

  @Test
  public void invalidSmilesWithFlagSvgTest() {
    final String smiles = "Cl[C](C)(C)(C)Br";

    Map<String, Object> result = graphDb.executeTransactionally("RETURN org.rdkit.utils.svg($smiles, false) AS svg", Collections.singletonMap("smiles", smiles), Iterators::single);
    final String svg = (String) result.get("svg");

    Assert.assertTrue(svg.contains("<svg"));
    Assert.assertTrue(svg.contains("</svg>"));
  }

  @Test
  public void searchCreateSvgTest() throws Throwable {
    insertChemblRows();
    final String smiles = "CCCC(C(=O)Nc1ccc(S(N)(=O)=O)cc1)C(C)(C)C";

    Map<String, Object> map = graphDb.executeTransactionally("CALL org.rdkit.search.exact.smiles($labels, $smiles) "
                    + "YIELD canonical_smiles "
                    + "RETURN org.rdkit.utils.svg(canonical_smiles) AS svg",
            MapUtil.map("labels", defaultLabels, "smiles", smiles), Iterators::single);
    final String svg = (String) map.get("svg");

    Assert.assertTrue(svg.contains("<svg"));
    Assert.assertTrue(svg.contains("</svg>"));
  }
}
