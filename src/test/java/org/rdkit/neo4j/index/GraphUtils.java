package org.rdkit.neo4j.index;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Logging;

public class GraphUtils {
  public static Driver getDriver() {
    Config config = Config.builder()
        .withLogging( Logging.slf4j() )
        .build();
    String uri = "bolt://localhost:7687";
    return GraphDatabase.driver(uri, AuthTokens.basic("neo4j", "test"), config);
  }
}
