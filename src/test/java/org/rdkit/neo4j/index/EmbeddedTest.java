package org.rdkit.neo4j.index;

import apoc.refactor.GraphRefactoring;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.val;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.test.TestGraphDatabaseFactory;

import org.rdkit.neo4j.index.model.ChemblRow;
import org.rdkit.neo4j.index.utils.ChemicalStructureParser;
import org.rdkit.neo4j.index.utils.GraphUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedTest {
  private static final Logger logger = LoggerFactory.getLogger(EmbeddedTest.class);

  private GraphDatabaseService graphDb;

  @BeforeEach
  public void prepareTestDatabase() {
    graphDb = GraphUtils.getTestDatabase();
  }

  // todo: remove unnecessary tests later, they were used only to get familiar with neo4j

  @AfterEach
  public void destroyTestDatabase() {
    graphDb.shutdown();
  }

  @Test
  public void createTestDb() throws Exception {
    // Do not use neo4j-temp as it starts to load /plugins folder
    // todo: why plugin folder fails?
//   graphDb = GraphUtils.getEmbeddedDatabase(new File("neo4j-temp/data/test.db"));

    Node n, m;
    try (val tx = graphDb.beginTx()) {
      n = graphDb.createNode(Label.label("HellNode"));
      n.setProperty("name", "Lucene");
      n.setProperty("time", -100500L);

      m = graphDb.createNode();
      m.setProperty("name", "Engine");
      m.setProperty("time", 100500L);

      m.createRelationshipTo(n, RelationshipType.withName("connected"));
      tx.success();
    }

//    val result = graphDb.execute("START n=node(*) MATCH (n)-[r]->(m) RETURN n,r,m;");
//    result.stream().forEach(x -> {
//      logger.info("Entry: {}", x);
//      for (val t: x.entrySet()) {
//        logger.info("\tInner object: key={}, value={}", t.getKey(), t.getValue());
//        if (t.getValue() instanceof Node) {
//          Node node = (Node) t.getValue();
//          try {
//            for (val prop : node.getAllProperties().entrySet()) {
//              logger.info("\t\tNode property: key={}, value={}", prop.getKey(), prop.getValue());
//            }
//          } catch (Exception e) {
//            logger.error("\t\t Something went wrong: {}", e.getMessage());
//          }
//        }
//      }
//    });

    try (val tx = graphDb.beginTx()) {
      Node foundN = graphDb.getNodeById( n.getId() );
      Node foundM = graphDb.getNodeById( m.getId() );

      Assertions.assertEquals(0, foundN.getId());
      Assertions.assertEquals(1, foundM.getId());
      Assertions.assertEquals("Lucene", foundN.getProperty( "name" ));
      Assertions.assertEquals("Engine", foundM.getProperty( "name" ));
    }
  }

  @Test
  public void insertDataTest() {
    new TestGraphDatabaseFactory().newImpermanentDatabase();
//    final GraphDatabaseService graphDb = GraphUtils.getEmbeddedDatabase(new File("neo4j-temp/data/test.db"));
    final List<String> rows = ChemicalStructureParser.readTestData();

    // Insert objects
    try (val tx = graphDb.beginTx()) {
      for (final String row: rows) {
        ChemblRow chemblRow = ChemicalStructureParser.convertChemicalRow(row);
        Node chemical = graphDb.createNode(Label.label("Chemical"));
        Node doc = graphDb.createNode(Label.label("Doc"));

        doc.setProperty("doc_id", chemblRow.getDocId());
        chemical.setProperty("mol_id", chemblRow.getMolId());
        chemical.setProperty("smiles", chemblRow.getSmiles());

        chemical.createRelationshipTo(doc, RelationshipType.withName("occured"));
      }

      tx.success();
    }

    // Remove duplicates
    // https://github.com/neo4j-contrib/neo4j-apoc-procedures/blob/3.5/src/test/java/apoc/refactor/GraphRefactoringTest.java
//    MATCH (n:Tag)
//    WITH n.name AS name, COLLECT(n) AS nodelist, COUNT(*) AS count
//    WHERE count > 1
//    CALL apoc.refactor.mergeNodes(nodelist) YIELD node
//    RETURN node
    // todo: add apoc plugin or use library method
    String mergeChemical = "MATCH (o:Chemical) " +
        "WITH o.smiles AS smiles, COLLECT(o) AS nodelist, COUNT(*) AS count " +
        "WHERE count > 1 " +
        "CALL apoc.refactor.mergeNodes(nodelist) YIELD node " +
        "RETURN node";

    String mergeDoc = "MATCH (d:Doc) " +
        "WITH d.doc_id AS smiles, COLLECT(d) AS nodelist, COUNT(*) AS count " +
        "WHERE count > 1 " +
        "CALL apoc.refactor.mergeNodes(nodelist) YIELD node " +
        "RETURN node";

    val mc = graphDb.execute(mergeChemical);
    val md = graphDb.execute(mergeDoc);
    logger.info("MergeChemical: {}", mc);
    logger.info("MergeDoc: {}", md);

//    GraphRefactoring gr = new GraphRefactoring();
//    gr.mergeNodes()
  }
}
