package org.rdkit.neo4j.procedures;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;

import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

public class ExactSearch {

  @Context
  public GraphDatabaseService db;

  @Context
  public Log log;

  @Procedure(name="org.rdkit.search.exact.smiles", mode = Mode.READ)
  public Stream<ExampleObject> exactSearchSmiles(@Name("label") String label, @Name("smiles") String smiles) {
    String index = indexName(label);

    // todo: validate smiles is correct (possible)
    // todo: validate existence of label
    log.info("Before exists");
    if (!db.index().existsForNodes(label)) {
      log.debug("Skipping index query since index does not exist: `%s`", index);
      return Stream.empty();
    }
    log.info("After exists");

//    db.index().forNodes(index).query(query).stream().map()
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("label", label);
    parameters.put("smiles", smiles);

    log.info("Create stream");
    return db.index()
        .forNodes(label)
        .query("MATCH (a:$label { smiles: $smiles } ) RETURN a", parameters)
        .stream()
        .map(ExampleObject::new);
  }

  private String indexName(String label) {
    return "rdkit";
//    return db.schema().getIndexes(Label.label(label)).iterator().next().getName();
  }

  public static class ExampleObject {
    public long nodeId;

    public ExampleObject(Node node) {
      this.nodeId = node.getId();
    }
  }

}
