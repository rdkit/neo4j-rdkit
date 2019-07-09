package org.rdkit.neo4j.index;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.val;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.rdkit.neo4j.bin.LibraryLoader;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.ChemicalStructureParser;
import org.rdkit.neo4j.index.utils.GraphUtils;


public class EmbeddedTest extends BaseTest {

  @BeforeClass
  public static void loadLibs() throws Throwable {
    LibraryLoader.loadLibraries();
  }

  @Test
  public void insertDataTest() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    List<Map<String, Object>> structures = ChemicalStructureParser.getChemicalRows();

    parameters.put("rows", structures);

    // Insert objects
    try (val tx = graphDb.beginTx()) {
      val r = graphDb.execute(
          "UNWIND {rows} as row "
              + "MERGE (from:Chemical{smiles: row.smiles, mol_id: row.mol_id}) "
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

  @Test
  public void loadIndexTest() {
    val result = graphDb.execute("CALL db.index.fulltext.listAvailableAnalyzers() "
        + "YIELD analyzer "
        + "WHERE analyzer = \"rdkit\" RETURN analyzer");

    Map<String, Object> analyzersList = GraphUtils.getFirstRow(result);
    assertEquals("rdkit", analyzersList.get("analyzer"));
    logger.info("Analyzer found");

    String chemical = "create (:Chemical {mol_id: \"CHEMBL77517\", smiles: \"NS(=O)(=O)c1ccc(S(=O)(=O)Nc2cccc3c(Cl)c[nH]c32)cc1\"})";
    graphDb.execute(chemical);

    String createIndex = "CALL db.index.fulltext.createNodeIndex(\"rdkit\", [\"Chemical\"], [\"smiles\"], {analyzer: \"rdkit\"})";
    graphDb.execute(createIndex);

    val indexExists = graphDb.execute("CALL db.indexes()");

    Map<String, Object> columns = GraphUtils.getFirstRow(indexExists);
    assertEquals("rdkit", columns.get("indexName"));
    assertEquals("node_fulltext", columns.get("type"));
    logger.info("Node Index created");
  }
}
