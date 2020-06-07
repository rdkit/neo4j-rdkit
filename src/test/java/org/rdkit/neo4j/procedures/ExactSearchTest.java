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


import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.helpers.collection.MapUtil;
import org.rdkit.neo4j.config.RDKitSettings;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.TestUtils;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExactSearchTest extends BaseTest {

  @Before
  public void registerProcedures() {
    TestUtils.registerProcedures(graphDb, ExactSearch.class);
  }

  @Override
  protected void prepareDatabase(GraphDatabaseBuilder builder) {
    builder.setConfig(RDKitSettings.indexSanitize, "false");
  }

  @Test
  public void callExactSmilesTest() throws Throwable {
    insertChemblRows();

    final String expectedSmiles = "COc1cc2c(cc1Br)C(C)CNCC2";
    try (val tx = graphDb.beginTx()) {
      val result = graphDb.execute("CALL org.rdkit.search.exact.smiles($labels, $smiles)",
          MapUtil.map("labels", defaultLabels, "smiles", expectedSmiles));

      final String[] chembls = new String[]{"CHEMBL180815", "CHEMBL182184", "CHEMBL180867"};

      for (int i = 0; i < chembls.length; i++) {
        Map<String, Object> map = result.next();
        String smiles = (String) map.get("canonical_smiles");
        assertEquals(expectedSmiles, smiles);
      }

      tx.success();
    }
  }

  @Test
  public void callExactMolTest() {
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

    graphDb.execute("CREATE (node:Chemical:Structure {mdlmol: $mol})", MapUtil.map("mol", mol));

    final String expectedSmiles = "COC1=CC=CC=C1";
    try (val tx = graphDb.beginTx()) {
      val result = graphDb.execute("CALL org.rdkit.search.exact.mol($labels, $mol, false)", MapUtil.map("labels", defaultLabels, "mol", mol));
      val item = result.next();
      String smiles = (String) item.get("canonical_smiles");
      assertEquals(expectedSmiles, smiles);
    }
  }
}
