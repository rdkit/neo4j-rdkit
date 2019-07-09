package org.rdkit.neo4j.procedures;


import static org.junit.Assert.assertEquals;
import static org.neo4j.graphdb.DependencyResolver.SelectionStrategy.FIRST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.val;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.graphdb.Node;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.internal.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.rdkit.neo4j.index.utils.BaseTest;
import org.rdkit.neo4j.index.utils.ChemicalStructureParser;
import org.rdkit.neo4j.index.utils.GraphUtils;


public class ExactSearchTest extends BaseTest {

  public Neo4jRule neo4j = new Neo4jRule()
      .withProcedure(ExactSearch.class);

  @Override
  public void prepareTestDatabase() {
    graphDb = GraphUtils.getTestDatabase();
    Procedures proceduresService = ((GraphDatabaseAPI) graphDb).getDependencyResolver().resolveDependency(Procedures.class, FIRST);
    try {
      proceduresService.registerProcedure(ExactSearch.class, true);
    } catch (KernelException e) {
      e.printStackTrace();
      logger.error("Not success :(");
    }
  }

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
    try (val tx = graphDb.beginTx()) {
      List<Map<String, Object>> structures = ChemicalStructureParser.getChemicalRows();
      Map<String, Object> parameters = new HashMap<>();

      parameters.put("rows", structures);

      graphDb.execute("UNWIND {rows} as row MERGE (from:Chemical{smiles: row.smiles, mol_id: row.mol_id})", parameters);

//      String createIndex = "CALL db.index.fulltext.createNodeIndex(\"rdkit\", [\"Chemical\"], [\"smiles\"], {analyzer: \"rdkit\"})";
//      graphDb.execute(createIndex);
      tx.success();
    }



    final String expectedSmiles = "COc1cc2c(cc1Br)C(C)CNCC2";
    final String label = "Chemical";
    final String query = String.format("CALL org.rdkit.search.exact.smiles(\"%s\", \"%s\")", label, expectedSmiles);
    try (val tx = graphDb.beginTx()) {
      val result = graphDb.execute(query);

      final String[] chembls = new String[]{"CHEMBL180815", "CHEMBL182184", "CHEMBL180867"};

      for (int i = 0; i < chembls.length; i++) {
        Node node = (Node) result.next().get("node");
        String chembl = (String) node.getProperty("mol_id");
        String smiles = (String) node.getProperty("smiles");
        assertEquals(chembls[i], chembl);
        assertEquals(expectedSmiles, smiles);
      }

      tx.success();
    }
  }

  @Test
  @Ignore
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

    try (org.neo4j.graphdb.Transaction tx = graphDb.beginTx()) {
      graphDb.execute(String.format("CREATE (node:Chemical {mdlmol: %s})", mol));
      tx.success();
    }

//    String createIndex = "CALL db.index.fulltext.createNodeIndex(\"rdkit\", [\"Chemical\"], [\"smiles\"], {analyzer: \"rdkit\"})";
//    graphDb.execute(createIndex);

    final String label = "Chemical";
    final String query = String.format("CALL org.rdkit.search.exact.mol(\"%s\", \"%s\")", label, mol);

    try (val tx = graphDb.beginTx()) {
      val result = graphDb.execute(query);
      val item = result.next();

      Node node = (Node) item.get("node");
      String obtainedMol = (String) node.getProperty("mdlmol");
      assertEquals(obtainedMol, mol);
    }
  }
}
