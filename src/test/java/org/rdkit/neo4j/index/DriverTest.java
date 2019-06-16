package org.rdkit.neo4j.index;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.summary.ResultSummary;

import org.rdkit.neo4j.index.utils.ChemicalStructureParser;
import org.rdkit.neo4j.index.utils.GraphUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverTest {
  private static final Logger logger = LoggerFactory.getLogger(DriverTest.class);

  @Disabled
  @Test
  public void testHello() {
    try (final Driver driver = GraphUtils.getDriver()) {
      String message = "Hi";

      try (Session session = driver.session()) {
        String greeting = session.writeTransaction(new TransactionWork<String>() {
          @Override
          public String execute(Transaction tx) {
            StatementResult result = tx.run(
                "CREATE (a:Greeting) " +
                    "SET a.message = $message " +
                    "RETURN a.message + ', from node ' + id(a)",
                parameters("message", message));
            return result.single().get(0).asString();
          }
        });
        System.out.println(greeting);
      }
    }
  }

//  https://github.com/neo4j/neo4j/issues/7228

  @Disabled
  @Test
  public void insertChemicalStructures() {
    List<String> rows = ChemicalStructureParser.readTestData();
    Map<String, Object> parameters = new HashMap<>();
    List<Map<String, Object>> structures = new ArrayList<>();

    for (final String row: rows) {
      structures.add(ChemicalStructureParser.mapChemicalRow(row));
    }

    try (final Driver driver = GraphUtils.getDriver()) {
      try (Session session = driver.session()) {
        parameters.put("structures", structures);

        insertChemicals(session, parameters);
        insertDocs(session, parameters);
        insertRelations(session, parameters);
      }
    }
  }

  private ResultSummary insertChemicals(Session session, Map<String, Object> parameters) {
    StatementResult resultNodes = session.run(
        "UNWIND {structures} as struct " +
            "CREATE (a:Chemical) SET a.mol_id = struct.mol_id, a.smiles = struct.smiles", parameters
    );
    val resNodes = resultNodes.consume();
    logger.info("{}", resNodes);
    return resNodes;
  }

  private ResultSummary insertDocs(Session session, Map<String, Object> parameters) {
    StatementResult resultDocs = session.run(
        "UNWIND {structures} as struct " +
            "CREATE (b:Doc) SET b.doc_id = struct.doc_id", parameters
    );
    val resDocs = resultDocs.consume();
    logger.info("{}", resDocs);
    return resDocs;
  }

  private ResultSummary insertRelations(Session session, Map<String, Object> parameters) {
    StatementResult resultRelations = session.run(
        "UNWIND {structures} as struct " +
            "MATCH (a:Chemical),(b:Doc) " +
            "WHERE a.mol_id = struct.mol_id AND b.doc_id = struct.doc_id " +
            "CREATE (a) -[:documented]-> (b)", parameters
    );
    val resRelation = resultRelations.consume();
    logger.info("{}", resRelation);
    return resRelation;
  }
}
