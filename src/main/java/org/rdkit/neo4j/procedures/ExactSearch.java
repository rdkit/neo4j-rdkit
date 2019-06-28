package org.rdkit.neo4j.procedures;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.logging.Log;

import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExactSearch {

  private static final Logger logger = LoggerFactory.getLogger(ExactSearch.class);

  @Context
  public GraphDatabaseService db;

  @Context
  public Log log;

  @Procedure(name = "org.rdkit.search.exact.smiles", mode = Mode.READ)
  public Stream<ExampleObject> exactSearchSmiles(@Name("label") String labelName,
      @Name("smiles") String smiles) {
    logger.info("label={}, smiles={}", labelName, smiles);

    // todo: validate smiles is correct (possible)

    // todo: is it even necessary to have index for it ?
    try {
      IndexDefinition index = db.schema().getIndexByName("rdkit");
      assert index.isNodeIndex();
      assert StreamSupport.stream(index.getLabels().spliterator(), false)
          .anyMatch(x -> x.equals(Label.label(labelName)));
    } catch (IllegalArgumentException e) {
      log.error("No `rdkit` node index found"); // todo: is it correct?
      return Stream.empty();
    }

    String query = String.format("MATCH (node:%s { smiles: '%s' }) RETURN node", labelName, smiles);
//    String query = "MATCH (node:$label { smiles: '$smiles' }) RETURN node";

    return db.execute(query)
        .stream()
        .map(ExampleObject::new);
//    return db.index()
//        .forNodes(label)
//        .query("MATCH (a:$label { smiles: $smiles } ) RETURN a", parameters)
//        .stream()
//        .map(ExampleObject::new);
  }

  private String indexName(String label) {
    return "rdkit";
//    return db.schema().getIndexes(Label.label(label)).iterator().next().getName();
  }

  public static class ExampleObject {

    public final long nodeId;
    public final String mol_id;
    public final String smiles;


    public ExampleObject(Node node) {
      this.nodeId = node.getId();
      this.mol_id = (String) node.getProperty("mol_id");
      this.smiles = (String) node.getProperty("smiles");
    }

    public ExampleObject(Map<String, Object> map) {
      this((Node) map.get("node"));
    }
  }
}
