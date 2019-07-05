package org.rdkit.neo4j.procedures;


import static org.junit.Assert.assertEquals;
import static org.neo4j.graphdb.DependencyResolver.SelectionStrategy.FIRST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.val;
import org.junit.Ignore;
import org.junit.Test;

import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.ChemicalStructureParser;


public class ExactSearchTest extends BaseTest {

  public Neo4jRule neo4j = new Neo4jRule()
      .withProcedure(ExactSearch.class);

  @Test
  @Ignore
  // todo: java.lang.IllegalStateException: Cannot access instance URI before or after the test runs
  public void searchTest() throws Exception {
    try (Driver driver = GraphDatabase
        .driver(neo4j.boltURI(), Config.build().withoutEncryption().toConfig())) {

      // Given I've started Neo4j with the FullTextIndex procedure class
      //       which my 'neo4j' rule above does.
      Session session = driver.session();

      try (Transaction tx = driver.session().beginTransaction()) {

        final List<String> rows = ChemicalStructureParser.readTestData();

        Map<String, Object> parameters = new HashMap<>();
        List<Map<String, Object>> structures = new ArrayList<>();

        for (final String row : rows) {
          structures.add(ChemicalStructureParser.mapChemicalRow(row));
        }

        parameters.put("rows", structures);

        // Insert objects
        val r = session.run(
            "UNWIND {rows} as row "
                + "MERGE (from:Chemical{smiles: row.smiles, mol_id: row.mol_id}) "
                + "MERGE (to:Doc{doc_id: row.doc_id}) "
                + "MERGE (from) -[:PUBLISHED]-> (to)", parameters);

//        logger.info("{}", r.summary().toString());
        tx.success();
        String createIndex = "CALL db.index.fulltext.createNodeIndex(\"rdkit\", [\"Chemical\"], [\"mol_id\"], {analyzer: \"rdkit\"})";
        session.run(createIndex);
      }
      session.close();

      session = driver.session();

      val res = session.run("CALL org.rdkit.search.exact.smiles('Chemical', 'COc1cc2c(cc1Br)C(C)CNCC2')");
      logger.info("{}", res.next());
    }
  }

  @Test
  public void callExactSmilesTest() throws Throwable {
    try (org.neo4j.graphdb.Transaction tx = graphDb.beginTx()) {
      final List<String> rows = ChemicalStructureParser.readTestData();
      Map<String, Object> parameters = new HashMap<>();
      List<Map<String, Object>> structures = new ArrayList<>();

      for (final String row : rows) {
        structures.add(ChemicalStructureParser.mapChemicalRow(row));
      }
      parameters.put("rows", structures);

      val r = graphDb.execute(
          "UNWIND {rows} as row "
              + "MERGE (from:Chemical{smiles: row.smiles, mol_id: row.mol_id})", parameters);

      tx.success();
    }

    String createIndex = "CALL db.index.fulltext.createNodeIndex(\"rdkit\", [\"Chemical\"], [\"smiles\"], {analyzer: \"rdkit\"})";
    graphDb.execute(createIndex);

    Procedures proceduresService = ((GraphDatabaseAPI) graphDb).getDependencyResolver().resolveDependency(Procedures.class, FIRST);
    proceduresService.registerProcedure(ExactSearch.class, true);

    final String expectedSmiles = "COc1cc2c(cc1Br)C(C)CNCC2";
    final String label = "Chemical";
    final String query = String.format("CALL org.rdkit.search.exact.smiles(\"%s\", \"%s\")", label, expectedSmiles);
    val result = graphDb.execute(query);

    final String[] chembls = new String[]{"CHEMBL180815", "CHEMBL182184", "CHEMBL180867"};

    for (int i = 0; i < chembls.length; i++) {
      val item = result.next();
      String chembl = (String) item.get("mol_id");
      String smiles = (String) item.get("smiles");
      assertEquals(chembls[i], chembl);
      assertEquals(expectedSmiles, smiles);
    }
  }
}
