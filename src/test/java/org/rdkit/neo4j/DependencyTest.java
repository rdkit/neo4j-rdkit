package org.rdkit.neo4j;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyTest {
  private static final Logger logger = LoggerFactory.getLogger(DependencyTest.class);

  @Test
  public void loggerTest() {
    logger.error("Error check");
  }


}
