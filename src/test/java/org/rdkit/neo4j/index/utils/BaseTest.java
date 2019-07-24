package org.rdkit.neo4j.index.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.val;
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

  protected void insertChemblRows() throws Exception {
    try (val tx = graphDb.beginTx()) {
      List<Map<String, Object>> structures = ChemicalStructureParser.getChemicalRows();
      Map<String, Object> parameters = new HashMap<>();

      parameters.put("rows", structures);

      graphDb.execute("UNWIND {rows} as row MERGE (from:Chemical:Structure {smiles: row.smiles, mol_id: row.mol_id})", parameters);

      tx.success();
    }
  }
}
