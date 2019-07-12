package org.rdkit.neo4j.procedures;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.val;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.collection.PagingIterator;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import org.rdkit.neo4j.handlers.RDKitEventHandler;
import org.rdkit.neo4j.models.MolBlock;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.utils.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDKitProcedures {

  private static final Logger logger = LoggerFactory.getLogger(RDKitProcedures.class);

  private static final String query = "MATCH (node:%s { %s: '%s' }) RETURN node";

  @Context
  public GraphDatabaseService db;

  @Context
  public Log log;

  @Procedure(name = "org.rdkit.search.exact.smiles", mode = Mode.READ)
  @Description("RDKit exact search on `smiles` property")
  public Stream<NodeWrapper> exactSearchSmiles(@Name("label") List<String> labelNames, @Name("smiles") String smiles) {
    logger.info("Exact search smiles :: label={}, smiles={}", labelNames, smiles);

    // todo: validate smiles is correct (possible)

    final String rdkitSmiles = Converter.getRDKitSmiles(smiles);
    final String labels = String.join(":", labelNames);

    Result result = db.execute(String.format(query, labels, NodeFields.CanonicalSmiles.getValue(), rdkitSmiles));
//    ResourceIterator<Node> nodes = db.findNodes(Label.label(labelName), NodeFields.CanonicalSmiles.getValue(), rdkitSmiles);
    return result.stream().map(NodeWrapper::new);
  }

  @Procedure(name = "org.rdkit.search.exact.mol", mode = Mode.READ)
  @Description("RDKit exact search on `mdlmol` property")
  public Stream<NodeWrapper> exactSearchMol(@Name("labels") List<String> labelNames, @Name("mol") String molBlock) {
    logger.info("Exact search mol :: label={}, molBlock={}", labelNames, molBlock);

    final String rdkitSmiles = Converter.convertMolBlock(molBlock).getCanonicalSmiles();
    final String labels = String.join(":", labelNames);

    Result result = db.execute(String.format(query, labels, NodeFields.CanonicalSmiles.getValue(), rdkitSmiles));
    return result.stream().map(NodeWrapper::new);
  }

  @Procedure(name = "org.rdkit.update", mode = Mode.WRITE)
  @Description("RDKit update procedure, allows to construct ['formula', 'molecular_weight', 'canonical_smiles'] values from 'mdlmol' property")
  public Stream<NodeWrapper> createPropertiesMol(@Name("labels") List<String> labelNames) throws InterruptedException {
    logger.info("Update nodes with labels={}, create additional fields", labelNames);

    final String firstLabel = labelNames.get(0);
    final List<Label> labels = labelNames.stream().map(Label::label).collect(Collectors.toList());

    Iterator<Node> nodeIterator = db.findNodes(Label.label(firstLabel))
            .stream()
            .filter(node -> labels.stream().allMatch(node::hasLabel))
            .iterator();

    final PagingIterator<Node> pagingIterator = new PagingIterator<>(nodeIterator, 10_000);

    Thread t = new Thread(() -> {  // we do explicit tx management so we require a separate thread

      Transaction tx = db.beginTx();  // tx needs to opened here - we need one upon consuming the iterator
      try {

        int numberOfBatches = 0;
        while (pagingIterator.hasNext()) {
          Iterator<Node> page = pagingIterator.nextPage();

          tx.success();
          tx.close();
          tx = db.beginTx();

          page.forEachRemaining(node -> {
            final String mol = (String) node.getProperty("mdlmol");
            final MolBlock block = Converter.convertMolBlock(mol);
            RDKitEventHandler.addProperties(node, block);

          });
          numberOfBatches++;
          log.info("batch # %d", numberOfBatches);
        }
        log.info("done, ran %d batches successfully", numberOfBatches);


      } finally {
        if (tx!=null) {
          tx.success();
          tx.close();
        }
      }

    });
    t.start();
    t.join();
    return Stream.empty();
  }

  public static class NodeWrapper {

    public Node node;

    public NodeWrapper(Node node) {
      this.node = node;
    }

    public NodeWrapper(Map<String, Object> map) {
      this((Node) map.get("node"));
    }
  }

  private String indexName(String label) {
    return "rdkit";
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
