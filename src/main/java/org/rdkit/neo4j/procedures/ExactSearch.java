package org.rdkit.neo4j.procedures;

import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.utils.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExactSearch {

  private static final Logger logger = LoggerFactory.getLogger(ExactSearch.class);

  private static final String query = "MATCH (node:%s { %s: '%s' }) RETURN node";

  @Context
  public GraphDatabaseService db;

  @Context
  public Log log;

  @Procedure(name = "org.rdkit.search.exact.smiles", mode = Mode.READ)
  @Description("RDKit exact search on `smiles` property")
  public Stream<NodeWrapper> exactSearchSmiles(@Name("label") String labelName, @Name("smiles") String smiles) {
    logger.info("Exact search smiles :: label={}, smiles={}", labelName, smiles);

    // todo: validate smiles is correct (possible)

    final String rdkitSmiles = Converter.getRDKitSmiles(smiles);
    ResourceIterator<Node> nodes = db.findNodes(Label.label(labelName), NodeFields.Smiles.getValue(), rdkitSmiles);
    return nodes.stream().map(NodeWrapper::new);
  }

  @Procedure(name="org.rdkit.search.exact.mol", mode=Mode.READ)
  @Description("RDKit exact search on `mdlmol` property")
  public Stream<NodeWrapper> exactSearchMol(@Name("label") String labelName, @Name("mol") String molBlock) {
    logger.info("Exact search mol :: label={}, molBlock={}", labelName, molBlock);

    ResourceIterator<Node> nodes = db.findNodes(Label.label(labelName), NodeFields.MdlMol.getValue(), molBlock);
    return nodes.stream().map(NodeWrapper::new);
  }

  private String indexName(String label) {
    return "rdkit";
  }

  public static class NodeWrapper {
    public final Node node;

    public NodeWrapper(Node node) {
      this.node = node;
    }
  }

  private void checkIndexExistence(String labelName) {
    try {
      IndexDefinition index = db.schema().getIndexByName("rdkit");
      assert index.isNodeIndex();
      assert StreamSupport.stream(index.getLabels().spliterator(), false)
          .anyMatch(x -> x.equals(Label.label(labelName)));
    } catch (IllegalArgumentException e) {
      log.error("No `rdkit` node index found"); // todo: is it correct?
      Stream.empty();
    }
  }
}
