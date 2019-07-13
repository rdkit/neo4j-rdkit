package org.rdkit.neo4j.index.utils;

import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseTest {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected GraphDatabaseService graphDb;

  @Before
  public void prepareTestDatabase() {
    graphDb = GraphUtils.getTestDatabase();
  }

  @After
  public void destroyTestDatabase() {
    graphDb.shutdown();
  }
}
