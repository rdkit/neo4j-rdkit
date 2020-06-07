package org.rdkit.neo4j.index.utils;

/*-
 * #%L
 * RDKit-Neo4j
 * %%
 * Copyright (C) 2019 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseTest {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected GraphDatabaseService graphDb;
  protected List<String> defaultLabels = Arrays.asList("Chemical", "Structure");

  @Before
  public void createTestDatabase() {
    GraphDatabaseBuilder builder = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder();
    prepareDatabase(builder);
    graphDb = builder.newGraphDatabase();
  }

  protected void prepareDatabase(GraphDatabaseBuilder builder) {
    // intentionally empty, to be overridden in derived classes
  }

  @After
  public void destroyTestDatabase() {
    graphDb.shutdown();
  }

  protected void insertChemblRows() throws Exception {
    try (Transaction tx = graphDb.beginTx()) {
      List<Map<String, Object>> structures = ChemicalStructureParser.getChemicalRows();
      Map<String, Object> parameters = new HashMap<>();

      parameters.put("rows", structures);

      graphDb.execute("UNWIND {rows} as row MERGE (from:Chemical:Structure {smiles: row.smiles, mol_id: row.mol_id})", parameters);

      tx.success();
    }
  }
}
