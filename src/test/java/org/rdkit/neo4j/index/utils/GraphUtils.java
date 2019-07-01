package org.rdkit.neo4j.index.utils;

import java.io.File;
import java.io.IOException;
import lombok.val;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Logging;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.test.TestGraphDatabaseFactory;

public class GraphUtils {
  public static Driver getDriver() {
    Config config = Config.builder()
        .withLogging( Logging.slf4j() )
        .build();
    String uri = "bolt://localhost:7687";
    return GraphDatabase.driver(uri, AuthTokens.basic("neo4j", "test"), config);
  }

  // todo: it is possible to use folder
  // https://neo4j.com/docs/java-reference/current/tutorials-java-embedded/unit-testing/
  public static GraphDatabaseService getTestDatabase() {
    return new TestGraphDatabaseFactory().newImpermanentDatabase();
  }

  public static GraphDatabaseService getTestDatabase(File storeDir) {
    return new TestGraphDatabaseFactory().newImpermanentDatabase(storeDir);
  }

  public static GraphDatabaseService getEmbeddedDatabase(File db) {
    GraphDatabaseFactory graphDbFactory = new GraphDatabaseFactory();
    return graphDbFactory.newEmbeddedDatabase(db);
  }
}
