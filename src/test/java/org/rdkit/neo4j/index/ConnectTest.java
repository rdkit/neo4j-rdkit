package org.rdkit.neo4j.index;

import static org.neo4j.driver.v1.Values.parameters;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

public class ConnectTest {

  public Driver getDriver() {
    String uri = "bolt://localhost:7687";
    return GraphDatabase.driver(uri, AuthTokens.basic("neo4j", "test"));
  }

  @Test
  public void testHello() {
    try (final Driver driver = getDriver()) {
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
}
