package org.rdkit.neo4j.index;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectTest {
  private static final Logger logger = LoggerFactory.getLogger(ConnectTest.class);

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

  @Test
  public void insertBatchesTest() {
    Map<String, Object> parameters = new HashMap<>();
    List<Map<String, Object>> batches = new ArrayList<>();
    for (int i = 1; i < 10; ++i) {
      batches.add(createBatch(i));
    }

    try (final Driver driver = GraphUtils.getDriver()) {
      try (Session session = driver.session()) {
        parameters.put("batches", batches);
        StatementResult result = session.run(
            "UNWIND {batches} as batch " +
                "CREATE (a:Node) SET a.id = batch.id, a.login = batch.login", parameters
        );
        val t = result.consume();
        logger.info("{}", t);
      }
    }
  }

  private Map<String, Object> createBatch(int i) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", i);
    map.put("login", "login" + i);

    return map;
  }
}
