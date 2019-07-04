package org.rdkit.neo4j.index;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.val;

import org.junit.Test;

import org.neo4j.graphdb.Result;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.ChemicalStructureParser;
import org.rdkit.neo4j.index.utils.GraphUtils;


public class EmbeddedTest extends BaseTest {

  @Test
  public void insertDataTest() throws Exception {
    final List<String> rows = ChemicalStructureParser.readTestData();

    Map<String, Object> parameters = new HashMap<>();
    List<Map<String, Object>> structures = new ArrayList<>();

    for (final String row : rows) {
      structures.add(ChemicalStructureParser.mapChemicalRow(row));
    }
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

      result1.accept(rowVisited -> {
        Number docs = rowVisited.getNumber("docs");
        assertEquals(56L, docs);

        return false;
      });

      val result2 = graphDb.execute("MATCH (a:Chemical) RETURN count(*) as chemicals");

      result2.accept(rowVisited -> {
        Number chemicals = rowVisited.getNumber("chemicals");
        assertEquals(940L, chemicals);

        return false;
      });

      val result3 = graphDb
          .execute("MATCH (a:Chemical) -[b:PUBLISHED]-> (c:Doc) RETURN count(*) as relations");

      result3.accept(rowVisited -> {
        Number relationsCount = rowVisited.getNumber("relations");
        assertEquals(1111L, relationsCount);

        return false;
      });

      tx.success();
    }
  }

  @Test
  public void loadIndexTest() {
    graphDb = GraphUtils.getTestDatabase(new File("neo4j-temp/test"));

    val result = graphDb.execute("CALL db.index.fulltext.listAvailableAnalyzers() "
        + "YIELD analyzer "
        + "WHERE analyzer = \"rdkit\" RETURN analyzer");

    Map<String, Object> analyzersList = getFirstRow(result);
    assertEquals("rdkit", analyzersList.get("analyzer"));
    logger.info("Analyzer found");

    String chemical = "create (:Chemical {mol_id: \"CHEMBL77517\", smiles: \"NS(=O)(=O)c1ccc(S(=O)(=O)Nc2cccc3c(Cl)c[nH]c32)cc1\"})";
    graphDb.execute(chemical);

    String createIndex = "CALL db.index.fulltext.createNodeIndex(\"rdkit\", [\"Chemical\"], [\"smiles\"], {analyzer: \"rdkit\"})";
    graphDb.execute(createIndex);

    val indexExists = graphDb.execute("CALL db.indexes()");

    Map<String, Object> columns = getFirstRow(indexExists);
    assertEquals("rdkit", columns.get("indexName"));
    assertEquals("node_fulltext", columns.get("type"));
    logger.info("Node Index created");
  }

  private Map<String, Object> getFirstRow(Result result) {
    return result.next();
  }
}
