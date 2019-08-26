package org.rdkit.neo4j.index;

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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.val;

import org.junit.BeforeClass;
import org.junit.Test;

import org.rdkit.neo4j.bin.LibraryLoader;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.ChemicalStructureParser;
import org.rdkit.neo4j.index.utils.GraphUtils;


public class EmbeddedTest extends BaseTest {

  @Test
  public void insertDataTest() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    List<Map<String, Object>> structures = ChemicalStructureParser.getChemicalRows();

    parameters.put("rows", structures);

    // Insert objects
    try (val tx = graphDb.beginTx()) {
      val r = graphDb.execute(
          "UNWIND {rows} as row "
              + "MERGE (from:Chemical:Structure {smiles: row.smiles, mol_id: row.mol_id}) "
              + "MERGE (to:Doc{doc_id: row.doc_id}) "
              + "MERGE (from) -[:PUBLISHED]-> (to)", parameters);

      logger.info("{}", r.resultAsString());
      tx.success();
    }

    try (val tx = graphDb.beginTx()) {
      val result1 = graphDb.execute("MATCH (c:Doc) RETURN count(*) as docs");
      long docsAmount = (Long) GraphUtils.getFirstRow(result1).get("docs");
      assertEquals(56L, docsAmount);

      val result2 = graphDb.execute("MATCH (a:Chemical) RETURN count(*) as chemicals");

      long chemicalsAmount = (Long) GraphUtils.getFirstRow(result2).get("chemicals");
      assertEquals(940L, chemicalsAmount);

      val result3 = graphDb.execute("MATCH (a:Chemical) -[b:PUBLISHED]-> (c:Doc) RETURN count(*) as relations");

      long relationsCount = (Long) GraphUtils.getFirstRow(result3).get("relations");
      assertEquals(1111L, relationsCount);

      tx.success();
    }
  }
}
